package tech.smartboot.feat.ai.chat.provider.anthropic;

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
import tech.smartboot.feat.ai.chat.provider.openai.OpenAiProvider;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.SseEvent;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic API 规范处理器实现
 * <p>
 * 实现 Anthropic Messages API 通信逻辑，支持 Claude 系列模型。
 * </p>
 *
 * <h3>与 OpenAI 的主要差异：</h3>
 * <table border="1">
 *   <tr><th>特性</th><th>OpenAI</th><th>Anthropic</th></tr>
 *   <tr><td>系统提示</td><td>messages 数组中</td><td>独立 system 字段</td></tr>
 *   <tr><td>max_tokens</td><td>可选</td><td>必填（默认 4096）</td></tr>
 *   <tr><td>认证头</td><td>Authorization: Bearer</td><td>x-api-key</td></tr>
 *   <tr><td>版本控制</td><td>URL 路径</td><td>anthropic-version 头</td></tr>
 *   <tr><td>内容结构</td><td>字符串</td><td>content 数组（多模态）</td></tr>
 * </table>
 *
 * <h3>API 端点：</h3>
 * POST {baseUrl}/v1/messages
 *
 * <h3>流式事件类型：</h3>
 * <ul>
 *   <li><b>message_start</b>：消息开始</li>
 *   <li><b>content_block_start</b>：内容块开始</li>
 *   <li><b>content_block_delta</b>：内容增量（实际文本）</li>
 *   <li><b>content_block_stop</b>：内容块结束</li>
 *   <li><b>message_delta</b>：消息级变化（如 stop_reason）</li>
 *   <li><b>message_stop</b>：消息结束</li>
 * </ul>
 *
 * @see Provider 抽象基类
 * @see OpenAiProvider OpenAI API 实现
 */
public class AnthropicProvider extends Provider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicProvider.class);
    /**
     * Anthropic API 版本号
     * <p>用于确保客户端与服务端的兼容性，不同版本可能有不同的请求/响应格式</p>
     */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /**
     * 工具调用构建器累积器：key=index, value=ToolCallBuilder
     */
    private final Map<Integer, ToolCallBuilder> toolCallBuilderMap = new HashMap<>();

    public AnthropicProvider(ChatOptions options) {
        super(options);
    }

    /**
     * 构建 HTTP POST 请求
     * <p>
     * 根据 Anthropic API 规范构造请求体和请求头。
     * </p>
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
        // Anthropic 要求必须设置 max_tokens
        if (!jsonObject.containsKey("max_tokens")) {
            jsonObject.put("max_tokens", 4096);
        }


        // 注入额外参数
        JSONObject extraBody = options.getExtraBody();
        if (extraBody != null && !extraBody.isEmpty()) {
            jsonObject.putAll(extraBody);
        }

        // 添加系统提示（独立字段）
        if (FeatUtils.isNotBlank(options.getSystem())) {
            jsonObject.put("system", options.getSystem());
        }

        // 添加用户消息列表（含 tool 消息到 tool_result 块的映射）
        jsonObject.put("messages", toAnthropicMessages(messages));

        // 处理工具定义（Function Calling）
        if (FeatUtils.isNotEmpty(tools)) {
            JSONArray toolsArray = new JSONArray();
            tools.forEach(tool -> {
                JSONObject toolJson = new JSONObject();
                toolJson.put("name", tool.getName());
                toolJson.put("description", tool.getDescription());
                // Anthropic 使用 input_schema 而非 parameters
                toolJson.put("input_schema", tool.getParameters());
                toolsArray.add(toolJson);
            });
            jsonObject.put("tools", toolsArray);
        }

        // 构建请求 URL
        String url = options.baseUrl() + "/v1/messages";

        return Feat.postJson(url, opts -> {
            opts.debug(options.isDebug());
            if (options.getHttpOptions() != null) {
                options.getHttpOptions().accept(opts);
            }
        }, header -> {
            // 添加 API Key（Anthropic 使用 x-api-key 而非 Authorization）
            if (FeatUtils.isNotBlank(options.apiKey())) {
                header.add("x-api-key", options.apiKey());
            }
            // 添加版本号（必需）
            header.add("anthropic-version", ANTHROPIC_VERSION);
        }, jsonObject);
    }

    /**
     * 将通用 Message 列表转换为 Anthropic Messages 协议的消息数组
     * <p>
     * 两处协议差异需要显式映射：
     * </p>
     * <ul>
     *   <li>tool 角色消息 → user 消息 + tool_result 内容块（tool_call_id 映射为 tool_use_id）</li>
     *   <li>assistant 消息的 tool_calls → content 块数组中的 tool_use 块（arguments JSON 串解析为 input 对象）</li>
     * </ul>
     * 其余消息（text、多模态内容）与 OpenAI 结构兼容，原样透传。
     *
     * @param messages 通用消息列表
     * @return Anthropic 协议格式的消息数组
     */
    private Object toAnthropicMessages(List<Message> messages) {
        JSONArray array = new JSONArray(messages.size());
        for (Message message : messages) {
            JSONObject json = (JSONObject) JSON.toJSON(message);
            String role = json.getString("role");

            // 工具结果消息：映射为 user 消息 + tool_result 块
            if (Message.ROLE_TOOL.equals(role)) {
                JSONObject block = new JSONObject();
                block.put("type", "tool_result");
                block.put("tool_use_id", json.getString("tool_call_id"));
                block.put("content", json.get("content"));
                JSONObject toolResultMessage = new JSONObject();
                toolResultMessage.put("role", Message.ROLE_USER);
                toolResultMessage.put("content", Collections.singletonList(block));
                array.add(toolResultMessage);
                continue;
            }

            // 助手消息的工具调用：tool_calls 转换为 content 块数组（text + tool_use）
            JSONArray toolCalls = json.getJSONArray("tool_calls");
            if (Message.ROLE_ASSISTANT.equals(role) && FeatUtils.isNotEmpty(toolCalls)) {
                JSONArray blocks = new JSONArray();
                Object content = json.get("content");
                if (content != null) {
                    blocks.add(JSONObject.of("type", "text", "text", content));
                }
                for (int i = 0; i < toolCalls.size(); i++) {
                    JSONObject toolCall = toolCalls.getJSONObject(i);
                    JSONObject toolUse = new JSONObject();
                    toolUse.put("type", "tool_use");
                    toolUse.put("id", toolCall.getString("id"));
                    toolUse.put("name", toolCall.getString("name"));
                    String arguments = toolCall.getString("arguments");
                    toolUse.put("input", FeatUtils.isNotBlank(arguments) ? JSON.parse(arguments) : new JSONObject());
                    blocks.add(toolUse);
                }
                json.put("content", blocks);
                json.remove("tool_calls");
            }
            array.add(json);
        }
        return array;
    }

    @Override
    public void parseStreamResponse(StreamContext context, SseEvent event, ChatStreamListener consumer) {
        // 获取事件类型和数据
        String eventType = event.getType();
        String data = event.getData();

        // 跳过空数据
        if (FeatUtils.isBlank(data)) {
            return;
        }

        try {
            // 解析事件数据为 JSON
            JSONObject object = JSON.parseObject(data);

            // 根据事件类型分发处理
            switch (eventType) {
                case "message_start": {
                    // 消息开始事件，重置工具调用状态，并捕获响应 id/model 与输入 token 用量
                    toolCallBuilderMap.clear();
                    JSONObject message = object.getJSONObject("message");
                    if (message != null) {
                        context.setId(message.getString("id"));
                        context.setModel(message.getString("model"));
                        JSONObject usage = message.getJSONObject("usage");
                        if (usage != null) {
                            Usage usageObj = new Usage();
                            usageObj.setPromptTokens(usage.getIntValue("input_tokens"));
                            context.setUsage(usageObj);
                        }
                    }
                    ChatChunk chunk = new ChatChunk();
                    chunk.setId(context.getId());
                    chunk.setModel(context.getModel());
                    chunk.setRole(Message.ROLE_ASSISTANT);
                    chunk.setUsage(context.getUsage());
                    consumer.onChunk(chunk);
                    break;
                }
                case "content_block_start":
                    // 内容块开始事件，检查是否为工具调用类型
                    // Anthropic 格式: {"index": 0, "content_block": {"type": "tool_use", "id": "toolu_xxx", "name": "function_name"}}
                    JSONObject contentBlock = object.getJSONObject("content_block");
                    if (contentBlock != null && "tool_use".equals(contentBlock.getString("type"))) {
                        int blockIndex = object.getIntValue("index");
                        ToolCallBuilder builder = new ToolCallBuilder(blockIndex);
                        builder.setId(contentBlock.getString("id"));
                        builder.setName(contentBlock.getString("name"));
                        toolCallBuilderMap.put(blockIndex, builder);
                    }
                    break;
                case "content_block_delta": {
                    // 内容增量事件（核心事件，包含实际文本或工具调用）
                    // Anthropic 格式: {"index": 0, "delta": {"type": "text_delta", "text": "..."}}
                    // 或 {"index": 0, "delta": {"type": "input_json_delta", "partial_json": "..."}}
                    JSONObject delta = object.getJSONObject("delta");
                    if (delta == null) {
                        break;
                    }
                    String deltaType = delta.getString("type");
                    ChatChunk chunk = new ChatChunk();
                    chunk.setId(context.getId());
                    chunk.setModel(context.getModel());
                    // 提取文本片段
                    if ("text_delta".equals(deltaType)) {
                        String text = delta.getString("text");
                        if (text != null) {
                            consumer.onStreamResponse(text); // 实时推送
                            context.appendContent(text);      // 累积保存
                            chunk.setContent(text);
                        }
                    }
                    // 提取推理内容（Claude Thinking 模式）
                    else if ("thinking_delta".equals(deltaType)) {
                        String thinking = delta.getString("thinking");
                        if (thinking != null) {
                            consumer.onReasoning(thinking); // 实时推送
                            context.appendReasoning(thinking); // 累积保存
                            chunk.setReasoningContent(thinking);
                        }
                    }
                    // 处理工具调用参数增量
                    else if ("input_json_delta".equals(deltaType)) {
                        int deltaIndex = object.getIntValue("index");
                        ToolCallBuilder builder = toolCallBuilderMap.get(deltaIndex);
                        String partialJson = delta.getString("partial_json");
                        if (builder != null && FeatUtils.isNotBlank(partialJson)) {
                            builder.appendArguments(partialJson);
                        }
                        // 工具调用参数增量以 ToolCall 分片形式推送
                        ToolCall increment = new ToolCall();
                        increment.setIndex(deltaIndex);
                        increment.setArguments(partialJson);
                        chunk.setToolCalls(Collections.singletonList(increment));
                    }
                    consumer.onChunk(chunk);
                    break;
                }
                case "content_block_stop":
                    // 内容块结束事件，无需处理
                    break;
                case "message_delta": {
                    // 消息级别变化事件：捕获 stop_reason 与输出 token 用量
                    JSONObject delta = object.getJSONObject("delta");
                    if (delta != null) {
                        String stopReason = delta.getString("stop_reason");
                        if (stopReason != null) {
                            context.setFinishReason(mapStopReason(stopReason));
                        }
                    }
                    JSONObject usage = object.getJSONObject("usage");
                    if (usage != null) {
                        Usage usageObj = context.getUsage();
                        if (usageObj == null) {
                            usageObj = new Usage();
                            context.setUsage(usageObj);
                        }
                        usageObj.setCompletionTokens(usage.getIntValue("output_tokens"));
                        usageObj.setTotalTokens(usageObj.getPromptTokens() + usageObj.getCompletionTokens());
                    }
                    ChatChunk chunk = new ChatChunk();
                    chunk.setId(context.getId());
                    chunk.setModel(context.getModel());
                    chunk.setFinishReason(context.getFinishReason());
                    chunk.setUsage(context.getUsage());
                    consumer.onChunk(chunk);
                    break;
                }
                case "message_stop":
                    // 消息完全结束，触发完成回调
                    ChatResponse chatResponse = new ChatResponse();
                    chatResponse.setRole(Message.ROLE_ASSISTANT);
                    chatResponse.setContent(context.getContent());
                    chatResponse.setReasoningContent(context.getReasoning());
                    // 回填流式过程中累积的元数据
                    chatResponse.setId(context.getId());
                    chatResponse.setModel(context.getModel());
                    chatResponse.setFinishReason(context.getFinishReason());
                    chatResponse.setUsage(context.getUsage());
                    // 转换 ToolCallBuilder 为 ToolCall
                    List<ToolCall> toolCalls = new ArrayList<>();
                    for (ToolCallBuilder builder : toolCallBuilderMap.values()) {
                        toolCalls.add(builder.toToolCall());
                    }
                    chatResponse.setToolCalls(toolCalls);
                    chatResponse.setSuccess(true);
                    context.setStatus(StreamContext.STREAM_STATUS_COMPLETE);
                    context.setResponse(chatResponse);
                    consumer.onCompletion(chatResponse);
                    break;
                default:
                    // 未知事件类型，记录调试日志
                    LOGGER.debug("Unknown Anthropic event type: " + eventType);
            }
        } catch (Exception e) {
            // 解析异常处理（避免单个事件失败导致整个流中断）
            LOGGER.error("Error parsing Anthropic stream response", e);
        }
    }

    /**
     * 处理非流式聊天响应
     * <p>
     * 实现同步请求-响应模式，解析完整响应 JSON。
     * </p>
     *
     * <h3>响应结构差异：</h3>
     * <ul>
     *   <li>content 是数组，需遍历提取文本</li>
     *   <li>Usage 字段：input_tokens/output_tokens</li>
     *   <li>工具调用通过 content 中的 tool_use 类型表示</li>
     * </ul>
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
        chatResponse.setFinishReason(mapStopReason(object.getString("stop_reason")));

        // 提取内容：Anthropic 的 content 是数组，需遍历拼接
        JSONArray contentArray = object.getJSONArray("content");
        List<ToolCall> toolCalls = new ArrayList<>();
        if (contentArray != null && !contentArray.isEmpty()) {
            StringBuilder contentBuilder = new StringBuilder();
            for (int i = 0; i < contentArray.size(); i++) {
                JSONObject contentItem = contentArray.getJSONObject(i);
                String type = contentItem.getString("type");
                // 提取文本类型的内容
                if ("text".equals(type)) {
                    contentBuilder.append(contentItem.getString("text"));
                }
                // 提取工具调用信息
                if ("tool_use".equals(type)) {
                    ToolCall toolCall = new ToolCall();
                    toolCall.setIndex(i);
                    toolCall.setId(contentItem.getString("id"));
                    toolCall.setName(contentItem.getString("name"));
                    JSONObject input = contentItem.getJSONObject("input");
                    toolCall.setArguments(input != null ? input.toJSONString() : "{}");
                    toolCalls.add(toolCall);
                }
            }
            chatResponse.setContent(contentBuilder.toString());
        }
        // 设置工具调用
        if (!toolCalls.isEmpty()) {
            chatResponse.setToolCalls(toolCalls);
        }

        // 提取使用统计：映射 Anthropic 的字段名到统一的 Usage 对象
        JSONObject usage = object.getJSONObject("usage");
        if (usage != null) {
            Usage usageObj = new Usage();
            // Anthropic 使用 input_tokens/output_tokens
            usageObj.setPromptTokens(usage.getInteger("input_tokens") != null ? usage.getInteger("input_tokens") : 0);
            usageObj.setCompletionTokens(usage.getInteger("output_tokens") != null ? usage.getInteger("output_tokens") : 0);
            usageObj.setTotalTokens(usageObj.getPromptTokens() + usageObj.getCompletionTokens());
            chatResponse.setUsage(usageObj);
        }

        // 触发回调
        return chatResponse;
    }

    /**
     * 将 Anthropic 的 stop_reason 映射为 OpenAI 兼容的 finish_reason
     *
     * @param stopReason Anthropic 的停止原因
     * @return OpenAI 兼容的结束原因，未知取值原样透传
     */
    private static String mapStopReason(String stopReason) {
        if (stopReason == null) {
            return null;
        }
        switch (stopReason) {
            case "end_turn":
            case "stop_sequence":
                return "stop";
            case "max_tokens":
                return "length";
            case "tool_use":
                return "tool_calls";
            default:
                return stopReason;
        }
    }

    public AnthropicProvider maxTokens(int maxTokens) {
        options.extraBody(json -> json.put("max_tokens", maxTokens));
        return this;
    }

    public static Message ofImage(String input, String media_type, String data) {
        MediaMessage message = new MediaMessage();
        message.setRole(Message.ROLE_USER);

        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image");
        imageContent.put("source", new JSONObject().fluentPut("type", "base64").fluentPut("data", data).fluentPut("media_type", media_type));


        JSONObject inputContent = new JSONObject();
        inputContent.put("text", input);
        inputContent.put("type", "text");


        message.setContent(Arrays.asList(imageContent, inputContent));
        return message;
    }
}
