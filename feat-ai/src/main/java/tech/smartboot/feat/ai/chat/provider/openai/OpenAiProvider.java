package tech.smartboot.feat.ai.chat.provider.openai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
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
        jsonObject.put("messages", addSystemMessageIfNeeded(messages));

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
     *   <li>解析 SSE data 字段（JSON 格式）</li>
     *   <li>从 choices[0].delta 提取 content、reasoning_content、tool_calls</li>
     *   <li>收到 "[DONE]" 时组装完整响应并调用 onCompletion</li>
     * </ol>
     *
     * @param context  流式上下文
     * @param event    SSE 事件
     * @param consumer 流式回调
     */
    @Override
    public void parseStreamResponse(StreamContext context, SseEvent event, StreamResponseCallback consumer) {
        String data = event.getData();
        // 终止标记或空数据：触发完成回调
        if ("[DONE]".equals(data) || FeatUtils.isBlank(data)) {
            // 防止重复触发（已完成且无新内容）
            String content = context.getContent();
            if (context.getStatus() == StreamContext.STREAM_STATUS_COMPLETE && FeatUtils.isBlank(content)) {
                return;
            }
            // 构建完整响应消息
            ResponseMessage responseMessage = new ResponseMessage();
            responseMessage.setRole(Message.ROLE_ASSISTANT);
            responseMessage.setContent(content);
            responseMessage.setReasoningContent(context.getReasoning());
            // 将 ToolCallBuilder 转换为通用 ToolCall
            List<ToolCall> toolCalls = new ArrayList<>();
            for (ToolCallBuilder builder : toolCallMap.values()) {
                toolCalls.add(builder.toToolCall());
            }
            responseMessage.setToolCalls(toolCalls);
            responseMessage.setSuccess(true);
            context.setStatus(StreamContext.STREAM_STATUS_COMPLETE);
            consumer.onCompletion(responseMessage);
            return;
        }

        // 解析 SSE 数据为 JSON
        JSONObject object = JSON.parseObject(data);
        // 检查是否有错误信息
        JSONObject error = object.getJSONObject("error");
        if (error != null) {
            ResponseMessage responseMessage = new ResponseMessage();
            responseMessage.setRole(Message.ROLE_ASSISTANT);
            responseMessage.setError(error.getString("message"));
            responseMessage.setSuccess(false);
            context.setStatus(StreamContext.STREAM_STATUS_COMPLETE);
            consumer.onCompletion(responseMessage);
            return;
        }

        // 提取第一个选择项的 delta
        JSONArray choices = object.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return;
        }
        JSONObject choice = choices.getJSONObject(0);
        JSONObject delta = choice.getJSONObject("delta");
        if (delta == null) {
            LOGGER.error("delta is null");
            return;
        }

        // 提取文本内容片段
        String content = delta.getString("content");
        if (content != null) {
            consumer.onStreamResponse(content); // 实时推送
            context.appendContent(content);      // 累积保存
        }

        // 提取推理内容片段
        String reasoningContent = delta.getString("reasoning_content");
        if (reasoningContent != null) {
            consumer.onReasoning(reasoningContent); // 实时推送
            context.appendReasoning(reasoningContent); // 累积保存
        }

        // 提取工具调用信息（可能为空或分片）
        JSONArray toolCallsArray = delta.getJSONArray("tool_calls");
        if (FeatUtils.isNotEmpty(toolCallsArray)) {
            for (int i = 0; i < toolCallsArray.size(); i++) {
                JSONObject toolCallObj = toolCallsArray.getJSONObject(i);
                // 解析 toolCallObj
                parseToolCall(toolCallObj);
            }
        }
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
    public ResponseMessage parseResponse(HttpResponse response) {
        // 解析响应 JSON
        JSONObject object = JSON.parseObject(response.body());
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setRole(Message.ROLE_ASSISTANT);
        responseMessage.setSuccess(true);

        // 提取 choices 数组
        JSONArray choices = object.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            if (message != null) {
                // 提取内容
                responseMessage.setContent(message.getString("content"));
                // 提取推理内容
                responseMessage.setReasoningContent(message.getString("reasoning_content"));

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
                    responseMessage.setToolCalls(toolCallList);
                }
            }
        }

        return responseMessage;
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

    protected List<Message> addSystemMessageIfNeeded(List<Message> messages) {
        if (FeatUtils.isBlank(options.getSystem())) {
            return messages;
        }
        if (FeatUtils.isNotEmpty(messages) && FeatUtils.equals(messages.get(0).getRole(), Message.ROLE_SYSTEM)) {
            return messages;
        }
        List<Message> result = new ArrayList<>();
        if (options.getSystem() != null) {
            result.add(Message.ofSystem(options.getSystem()));
        }
        result.addAll(messages);
        return result;
    }
}
