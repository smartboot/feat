package tech.smartboot.feat.ai.chat.provider.openai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.ChatStreamListener;
import tech.smartboot.feat.ai.chat.entity.ChatChunk;
import tech.smartboot.feat.ai.chat.entity.ChatResponse;
import tech.smartboot.feat.ai.chat.entity.MediaMessage;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.ai.chat.entity.Usage;
import tech.smartboot.feat.ai.chat.provider.Provider;
import tech.smartboot.feat.ai.chat.provider.StreamContext;
import tech.smartboot.feat.ai.chat.provider.ToolCallBuilder;
import tech.smartboot.feat.ai.chat.provider.anthropic.AnthropicProvider;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.SseEvent;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI API 规范处理器实现
 * <p>
 * 实现 OpenAI Chat Completions API 通信逻辑。
 * </p>
 *
 * <h3>支持特性：</h3>
 * <ul>
 *   <li>流式响应（SSE 协议）</li>
 *   <li>非流式响应</li>
 *   <li>工具调用（Function Calling）</li>
 *   <li>推理内容（reasoning_content）</li>
 *   <li>额外参数（extraBody）</li>
 * </ul>
 *
 * <h3>兼容服务商：</h3>
 * Azure OpenAI、阿里云通义千问、百度文心一言、智谱 GLM、DeepSeek 等
 *
 * <h3>API 端点：</h3>
 * POST {baseUrl}/chat/completions
 *
 * @see Provider 抽象基类
 * @see AnthropicProvider Anthropic API 实现
 */
public class OpenAiProvider extends Provider {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiProvider.class);
    public static final Consumer<JSONObject> responseJsonFormat = (jsonObject) -> jsonObject.put("response_format", JSONObject.of("type", "json_object"));
    /**
     * 工具调用构建器累积器：key=index, value=ToolCallBuilder
     */
    private final Map<Integer, ToolCallBuilder> toolCallMap = new HashMap<>();

    public OpenAiProvider(ChatOptions options) {
        super(options);
    }

    /**
     * 解析 OpenAI 格式的 tool_call JSON 对象并更新构建器状态
     *
     * @param toolCallObj OpenAI 格式的 tool_call JSON 对象
     */
    private void parseToolCall(JSONObject toolCallObj) {
        // 根据 index 获取或创建 ToolCallBuilder
        ToolCallBuilder builder = toolCallMap.computeIfAbsent(toolCallObj.getIntValue("index"), ToolCallBuilder::new);
        // 更新基础字段
        if (FeatUtils.isBlank(builder.getId())) {
            builder.setId(toolCallObj.getString("id"));
        }

        // 更新函数信息
        if (!toolCallObj.containsKey("function")) {
            return;
        }
        JSONObject functionObj = toolCallObj.getJSONObject("function");
        String functionName = functionObj.getString("name");
        if (FeatUtils.isNotBlank(functionName)) {
            builder.setName(functionName);
        }
        String functionArgs = functionObj.getString("arguments");
        if (FeatUtils.isNotBlank(functionArgs)) {
            builder.appendArguments(functionArgs);
        }
    }

    /**
     * 构建 HTTP POST 请求
     *
     * @param messages 消息列表
     * @param stream   是否启用流式响应
     * @param tools    工具函数列表
     * @return HttpPost 请求对象
     */
    public HttpPost createRequest(List<Message> messages, boolean stream, List<Tool> tools) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", options.getModel());
        jsonObject.put("stream", stream);
        jsonObject.put("messages", addSystemMessageIfNeeded(toOpenAiMessages(messages)));

        // 构建工具列表（Function Calling）
        if (FeatUtils.isNotEmpty(tools)) {
            JSONArray toolsArray = new JSONArray();
            tools.forEach(function -> {
                JSONObject toolJson = new JSONObject();
                toolJson.put("type", "function");
                toolJson.put("function", function);
                toolsArray.add(toolJson);
            });
            jsonObject.put("tools", toolsArray);
            jsonObject.put("tool_choice", "auto"); // 自动决定是否调用工具
        }


        // 注入额外参数（如 response_format、top_p、max_tokens 等）
        JSONObject extraBody = options.getExtraBody();
        if (extraBody != null && !extraBody.isEmpty()) {
            jsonObject.putAll(extraBody);
        }

        // 流式请求注入 stream_options.include_usage，让上游在流末尾返回 usage 帧
        if (stream && options.isIncludeUsage()) {
            jsonObject.put("stream_options", JSONObject.of("include_usage", true));
        }

        // 构建 HTTP 请求
        return Feat.postJson(options.baseUrl() + "/chat/completions", opts -> {
            opts.debug(options.isDebug());
            if (options.getHttpOptions() != null) {
                options.getHttpOptions().accept(opts);
            }
        }, header -> {
            // 添加认证头（Bearer Token）
            if (FeatUtils.isNotBlank(options.apiKey())) {
                header.add(HeaderName.AUTHORIZATION, "Bearer " + options.apiKey());
            }
        }, jsonObject);
    }

    /**
     * 处理流式聊天响应（SSE 模式）
     * <p>
     * 实现 OpenAI SSE 流式传输协议。
     * </p>
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>解析 SSE data 字段（JSON 格式），捕获 chunk 级 id/model/finish_reason</li>
     *   <li>从 choices[0].delta 提取 content、reasoning_content、tool_calls 增量</li>
     *   <li>choices 为空且携带 usage 的帧解析为用量统计（需开启 include_usage）</li>
     *   <li>每个数据帧通过 onChunk 推送结构化增量，文本增量走 onStreamResponse/onReasoning</li>
     *   <li>收到 "[DONE]" 时组装完整响应并调用 onCompletion</li>
     * </ol>
     *
     * @param context  流式上下文
     * @param event    SSE 事件
     * @param consumer 流式回调
     */
    @Override
    public void parseStreamResponse(StreamContext context, SseEvent event, ChatStreamListener consumer) {
        String data = event.getData();
        // 终止标记或空数据：触发完成回调
        if ("[DONE]".equals(data) || FeatUtils.isBlank(data)) {
            // 防止重复触发（已完成且无新内容）
            String content = context.getContent();
            if (context.getStatus() == StreamContext.STREAM_STATUS_COMPLETE && FeatUtils.isBlank(content)) {
                return;
            }
            // 构建完整响应消息
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setRole(Message.ROLE_ASSISTANT);
            chatResponse.setContent(content);
            chatResponse.setReasoningContent(context.getReasoning());
            // 回填流式过程中累积的元数据
            chatResponse.setId(context.getId());
            chatResponse.setModel(context.getModel());
            chatResponse.setFinishReason(context.getFinishReason());
            chatResponse.setUsage(context.getUsage());
            // 将 ToolCallBuilder 转换为通用 ToolCall
            List<ToolCall> toolCalls = new ArrayList<>();
            for (ToolCallBuilder builder : toolCallMap.values()) {
                toolCalls.add(builder.toToolCall());
            }
            chatResponse.setToolCalls(toolCalls);
            chatResponse.setSuccess(true);
            context.setStatus(StreamContext.STREAM_STATUS_COMPLETE);
            context.setResponse(chatResponse);
            consumer.onCompletion(chatResponse);
            return;
        }

        // 解析 SSE 数据为 JSON
        JSONObject object = JSON.parseObject(data);
        // 检查是否有错误信息
        JSONObject error = object.getJSONObject("error");
        if (error != null) {
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setRole(Message.ROLE_ASSISTANT);
            chatResponse.setError(error.getString("message"));
            chatResponse.setSuccess(false);
            context.setStatus(StreamContext.STREAM_STATUS_COMPLETE);
            context.setResponse(chatResponse);
            consumer.onCompletion(chatResponse);
            return;
        }

        // 捕获 chunk 级元数据（同一流内保持不变）
        context.setId(object.getString("id"));
        context.setModel(object.getString("model"));
        ChatChunk chunk = new ChatChunk();
        chunk.setId(context.getId());
        chunk.setModel(context.getModel());

        // 提取第一个选择项的 delta
        JSONArray choices = object.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            // usage 帧：choices 为空、顶层携带 usage（需开启 include_usage）
            JSONObject usageObj = object.getJSONObject("usage");
            if (usageObj != null) {
                Usage usage = usageObj.toJavaObject(Usage.class);
                context.setUsage(usage);
                chunk.setUsage(usage);
                consumer.onChunk(chunk);
            }
            return;
        }
        JSONObject choice = choices.getJSONObject(0);
        String finishReason = choice.getString("finish_reason");
        if (finishReason != null) {
            context.setFinishReason(finishReason);
            chunk.setFinishReason(finishReason);
        }
        JSONObject delta = choice.getJSONObject("delta");
        if (delta == null) {
            LOGGER.error("delta is null");
            consumer.onChunk(chunk);
            return;
        }
        chunk.setRole(delta.getString("role"));

        // 提取文本内容片段
        String content = delta.getString("content");
        if (content != null) {
            consumer.onStreamResponse(content); // 实时推送
            context.appendContent(content);      // 累积保存
            chunk.setContent(content);
        }

        // 提取推理内容片段
        String reasoningContent = delta.getString("reasoning_content");
        if (reasoningContent != null) {
            consumer.onReasoning(reasoningContent); // 实时推送
            context.appendReasoning(reasoningContent); // 累积保存
            chunk.setReasoningContent(reasoningContent);
        }

        // 提取工具调用信息（可能为空或分片）
        JSONArray toolCallsArray = delta.getJSONArray("tool_calls");
        if (FeatUtils.isNotEmpty(toolCallsArray)) {
            List<ToolCall> increments = new ArrayList<>(toolCallsArray.size());
            for (int i = 0; i < toolCallsArray.size(); i++) {
                JSONObject toolCallObj = toolCallsArray.getJSONObject(i);
                // 解析并累积到 ToolCallBuilder
                parseToolCall(toolCallObj);
                // 构建增量 ToolCall（仅携带本帧出现的字段）
                ToolCall increment = new ToolCall();
                increment.setIndex(toolCallObj.getIntValue("index"));
                if (toolCallObj.containsKey("id")) {
                    increment.setId(toolCallObj.getString("id"));
                }
                JSONObject functionObj = toolCallObj.getJSONObject("function");
                if (functionObj != null) {
                    if (functionObj.containsKey("name")) {
                        increment.setName(functionObj.getString("name"));
                    }
                    if (functionObj.containsKey("arguments")) {
                        increment.setArguments(functionObj.getString("arguments"));
                    }
                }
                increments.add(increment);
            }
            chunk.setToolCalls(increments);
        }

        // 推送结构化数据块（文本/推理增量回调已先行触发）
        consumer.onChunk(chunk);
    }

    /**
     * 处理非流式聊天响应
     * <p>
     * 实现同步请求-响应模式，一次性返回完整结果。
     * 直接从 JSONObject 中提取所需信息，无需定义额外的模型类。
     * </p>
     *
     * @param response HTTP 响应
     * @return 响应消息
     */
    @Override
    public ChatResponse parseResponse(HttpResponse response) {
        // 解析响应 JSON
        JSONObject object = JSON.parseObject(response.body());
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setRole(Message.ROLE_ASSISTANT);
        chatResponse.setSuccess(true);
        // 回填顶层元数据
        chatResponse.setId(object.getString("id"));
        chatResponse.setModel(object.getString("model"));
        JSONObject usageObj = object.getJSONObject("usage");
        if (usageObj != null) {
            chatResponse.setUsage(usageObj.toJavaObject(Usage.class));
        }

        // 提取 choices 数组
        JSONArray choices = object.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject choice = choices.getJSONObject(0);
            chatResponse.setFinishReason(choice.getString("finish_reason"));
            JSONObject message = choice.getJSONObject("message");
            if (message != null) {
                // 提取内容
                chatResponse.setContent(message.getString("content"));
                // 提取推理内容
                chatResponse.setReasoningContent(message.getString("reasoning_content"));

                // 提取工具调用
                JSONArray toolCalls = message.getJSONArray("tool_calls");
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    List<ToolCall> toolCallList = new ArrayList<>();
                    for (int i = 0; i < toolCalls.size(); i++) {
                        JSONObject toolCallObj = toolCalls.getJSONObject(i);
                        ToolCall toolCall = new ToolCall();
                        toolCall.setIndex(i);
                        toolCall.setId(toolCallObj.getString("id"));

                        JSONObject function = toolCallObj.getJSONObject("function");
                        if (function != null) {
                            toolCall.setName(function.getString("name"));
                            toolCall.setArguments(function.getString("arguments"));
                        }
                        toolCallList.add(toolCall);
                    }
                    chatResponse.setToolCalls(toolCallList);
                }
            }
        }

        return chatResponse;
    }

    /**
     * 设置温度参数
     *
     * @param temperature 温度参数，控制生成文本的随机性，范围通常为 0.0 到 2.0
     * @return 当前ChatOptions实例，用于链式调用
     */
    public OpenAiProvider temperature(double temperature) {
        options.extraBody(json -> {
            json.put("temperature", temperature);
        });
        return this;
    }

    /**
     * 设置JSON响应格式
     * 需在提示词中明确指示模型输出JSON，如："请按照json格式输出"，否则会报错。
     *
     * @return 当前ChatOptions实例，用于链式调用
     */
    public OpenAiProvider responseJsonFormat() {
        options.extraBody(responseJsonFormat);
        return this;
    }

    /**
     * 将通用 Message 列表转换为 OpenAI Chat Completions 协议的消息数组
     * <p>
     * 通用 ToolCall 结构（index/id/name/arguments 扁平字段）需转换为 OpenAI 的
     * wire 格式（type:function + function 嵌套），tool 消息的 tool_call_id 字段
     * 与协议一致，原样保留。
     * </p>
     *
     * @param messages 通用消息列表
     * @return OpenAI 协议格式的消息数组
     */
    private JSONArray toOpenAiMessages(List<Message> messages) {
        JSONArray array = new JSONArray(messages.size());
        for (Message message : messages) {
            JSONObject json = (JSONObject) JSON.toJSON(message);
            JSONArray toolCalls = json.getJSONArray("tool_calls");
            if (FeatUtils.isNotEmpty(toolCalls)) {
                JSONArray wireToolCalls = new JSONArray(toolCalls.size());
                for (int i = 0; i < toolCalls.size(); i++) {
                    JSONObject toolCall = toolCalls.getJSONObject(i);
                    JSONObject wire = new JSONObject();
                    if (toolCall.containsKey("id")) {
                        wire.put("id", toolCall.getString("id"));
                    }
                    wire.put("type", "function");
                    JSONObject function = new JSONObject();
                    if (toolCall.containsKey("name")) {
                        function.put("name", toolCall.getString("name"));
                    }
                    if (toolCall.containsKey("arguments")) {
                        function.put("arguments", toolCall.getString("arguments"));
                    }
                    wire.put("function", function);
                    wireToolCalls.add(wire);
                }
                json.put("tool_calls", wireToolCalls);
            }
            array.add(json);
        }
        return array;
    }

    protected JSONArray addSystemMessageIfNeeded(JSONArray messages) {
        if (FeatUtils.isBlank(options.getSystem())) {
            return messages;
        }
        if (FeatUtils.isNotEmpty(messages) && FeatUtils.equals(messages.getJSONObject(0).getString("role"), Message.ROLE_SYSTEM)) {
            return messages;
        }
        JSONArray result = new JSONArray(messages.size() + 1);
        if (options.getSystem() != null) {
            result.add(JSONObject.of("role", Message.ROLE_SYSTEM, "content", options.getSystem()));
        }
        result.addAll(messages);
        return result;
    }

    public static Message ofImage(String input, String imageUrl) {
        MediaMessage message = new MediaMessage();
        message.setRole(Message.ROLE_USER);
        JSONObject inputContent = new JSONObject();
        inputContent.put("text", input);
        inputContent.put("type", "text");

        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image_url");
        imageContent.put("image_url", JSONObject.of("url", imageUrl));

        message.setContent(Arrays.asList(inputContent, imageContent));
        return message;
    }
}
