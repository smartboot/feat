package tech.smartboot.feat.ai.chat.provider.openai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.ChatStreamListener;
import tech.smartboot.feat.ai.chat.entity.ChatChunk;
import tech.smartboot.feat.ai.chat.entity.ChatResponse;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.ai.chat.entity.Usage;
import tech.smartboot.feat.ai.chat.provider.Provider;
import tech.smartboot.feat.ai.chat.provider.StreamContext;
import tech.smartboot.feat.ai.chat.provider.ToolCallBuilder;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.SseEvent;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Responses API 规范处理器实现
 * <p>
 * 实现 OpenAI Responses API（POST {baseUrl}/responses）通信逻辑。
 * 与 {@link OpenAiProvider}（Chat Completions 规范）的差异：
 * </p>
 *
 * <h3>协议差异：</h3>
 * <ul>
 *   <li>消息历史走 {@code input} 项数组（message / function_call / function_call_output），
 *       系统提示用顶层 {@code instructions} 表达，不再有 messages 数组与 role=system</li>
 *   <li>工具定义为扁平结构（type/name/description/parameters 在同一层），
 *       不再有 function 嵌套；工具调用以 {@code call_id} 关联（非 tool_call_id）</li>
 *   <li>响应是 {@code output} 项数组（message / reasoning / function_call），
 *       文本位于 content 内的 output_text 片段</li>
 *   <li>流式为语义事件：没有 [DONE] 终止标记，以 response.completed /
 *       response.incomplete / response.failed 事件收尾；文本增量来自
 *       response.output_text.delta，推理增量来自 response.reasoning_summary_text.delta
 *       与 response.reasoning_text.delta，工具参数增量来自
 *       response.function_call_arguments.delta</li>
 * </ul>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * ChatModel chatModel = FeatAI.chatModel(o -> {
 *     o.baseUrl("https://api.openai.com/v1");
 *     o.apiKey("sk-...");
 *     o.model("gpt-4.1");
 *     o.specProvider(OpenAiResponsesProvider::new);
 * });
 * }</pre>
 *
 * @see OpenAiProvider Chat Completions 规范实现
 * @see Provider 抽象基类
 */
public class OpenAiResponsesProvider extends Provider {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiResponsesProvider.class);

    /**
     * 工具调用构建器累积器：key=output_index, value=ToolCallBuilder
     */
    private final Map<Integer, ToolCallBuilder> toolCallMap = new HashMap<>();

    public OpenAiResponsesProvider(ChatOptions options) {
        super(options);
    }

    /**
     * 构建 HTTP POST 请求
     * <p>
     * 系统提示（options.system 与 messages 中的 system 消息）合并为顶层 instructions；
     * 消息历史转换为 input 项数组；工具定义转为 Responses 扁平格式。
     * </p>
     *
     * @param messages 消息列表
     * @param stream   是否启用流式响应
     * @param tools    工具函数列表
     * @return HttpPost 请求对象
     */
    @Override
    public HttpPost createRequest(List<Message> messages, boolean stream, List<Tool> tools) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", options.getModel());
        jsonObject.put("stream", stream);
        String instructions = buildInstructions(messages);
        if (FeatUtils.isNotBlank(instructions)) {
            jsonObject.put("instructions", instructions);
        }
        jsonObject.put("input", toResponseInput(messages));

        // 工具定义：Responses 为扁平结构（type/name/description/parameters 同层）
        if (FeatUtils.isNotEmpty(tools)) {
            JSONArray toolsArray = new JSONArray();
            for (Tool tool : tools) {
                JSONObject toolJson = (JSONObject) JSON.toJSON(tool);
                toolJson.put("type", "function");
                toolsArray.add(toolJson);
            }
            jsonObject.put("tools", toolsArray);
            jsonObject.put("tool_choice", "auto");
        }

        // 注入额外参数（如 reasoning、temperature、max_output_tokens 等）
        JSONObject extraBody = options.getExtraBody();
        if (extraBody != null && !extraBody.isEmpty()) {
            jsonObject.putAll(extraBody);
        }

        return Feat.postJson(options.baseUrl() + "/responses", opts -> {
            opts.debug(options.isDebug());
            if (options.getHttpOptions() != null) {
                options.getHttpOptions().accept(opts);
            }
        }, header -> {
            if (FeatUtils.isNotBlank(options.apiKey())) {
                header.add(HeaderName.AUTHORIZATION, "Bearer " + options.apiKey());
            }
        }, jsonObject);
    }

    /**
     * 处理流式响应（语义事件流）
     * <p>
     * Responses 协议没有 [DONE] 终止标记：文本增量来自 response.output_text.delta，
     * 推理增量来自 response.reasoning_summary_text.delta / response.reasoning_text.delta，
     * 工具参数增量来自 response.function_call_arguments.delta（以 output_index 关联），
     * 流在 response.completed / response.incomplete / response.failed 处收尾并组装完整响应。
     * </p>
     *
     * @param context  流式上下文
     * @param event    SSE 事件
     * @param consumer 流式回调
     */
    @Override
    public void parseStreamResponse(StreamContext context, SseEvent event, ChatStreamListener consumer) {
        String data = event.getData();
        // Responses 协议无 [DONE]，空数据帧直接忽略；终态由 response.completed 等事件触发
        if (FeatUtils.isBlank(data)) {
            return;
        }
        JSONObject object = JSON.parseObject(data);
        String type = object.getString("type");
        if (type == null) {
            LOGGER.warn("unknown stream event without type: " + data);
            return;
        }

        switch (type) {
            case "response.created":
            case "response.in_progress": {
                // 元数据帧：捕获响应 id / model，供 chunk 与终态回填
                JSONObject response = object.getJSONObject("response");
                if (response != null) {
                    context.setId(response.getString("id"));
                    context.setModel(response.getString("model"));
                }
                consumer.onChunk(metaChunk(context));
                break;
            }
            case "response.output_text.delta": {
                String delta = object.getString("delta");
                if (delta != null) {
                    consumer.onStreamResponse(delta);
                    context.appendContent(delta);
                    ChatChunk chunk = metaChunk(context);
                    chunk.setContent(delta);
                    consumer.onChunk(chunk);
                }
                break;
            }
            case "response.refusal.delta": {
                // 拒答文本按普通文本内容流式透出
                String delta = object.getString("delta");
                if (delta != null) {
                    consumer.onStreamResponse(delta);
                    context.appendContent(delta);
                    ChatChunk chunk = metaChunk(context);
                    chunk.setContent(delta);
                    consumer.onChunk(chunk);
                }
                break;
            }
            case "response.reasoning_summary_text.delta":
            case "response.reasoning_text.delta": {
                String delta = object.getString("delta");
                if (delta != null) {
                    consumer.onReasoning(delta);
                    context.appendReasoning(delta);
                    ChatChunk chunk = metaChunk(context);
                    chunk.setReasoningContent(delta);
                    consumer.onChunk(chunk);
                }
                break;
            }
            case "response.output_item.added": {
                // function_call 项开始：建立构建器并暴露 id(call_id)/name 增量
                JSONObject item = object.getJSONObject("item");
                if (item != null && "function_call".equals(item.getString("type"))) {
                    int index = object.getIntValue("output_index");
                    ToolCallBuilder builder = toolCallMap.computeIfAbsent(index, ToolCallBuilder::new);
                    builder.setId(item.getString("call_id"));
                    if (FeatUtils.isNotBlank(item.getString("name"))) {
                        builder.setName(item.getString("name"));
                    }
                    ChatChunk chunk = metaChunk(context);
                    chunk.setToolCalls(Collections.singletonList(incrementOf(index, builder)));
                    consumer.onChunk(chunk);
                }
                break;
            }
            case "response.function_call_arguments.delta": {
                String delta = object.getString("delta");
                if (delta != null) {
                    int index = object.getIntValue("output_index");
                    ToolCallBuilder builder = toolCallMap.computeIfAbsent(index, ToolCallBuilder::new);
                    builder.appendArguments(delta);
                    ChatChunk chunk = metaChunk(context);
                    chunk.setToolCalls(Collections.singletonList(incrementOf(index, builder)));
                    consumer.onChunk(chunk);
                }
                break;
            }
            case "response.completed":
            case "response.incomplete": {
                // 终态事件：事件携带的 response 对象即完整响应
                JSONObject response = object.getJSONObject("response");
                ChatResponse chatResponse = response == null ? new ChatResponse() : toChatResponse(response);
                if ("response.incomplete".equals(type)) {
                    JSONObject incompleteDetails = response == null ? null : response.getJSONObject("incomplete_details");
                    String reason = incompleteDetails == null ? null : incompleteDetails.getString("reason");
                    // 截断原因映射为 OpenAI 风格的 finish_reason
                    chatResponse.setFinishReason("max_output_tokens".equals(reason) ? "length" : "incomplete");
                }
                context.setId(chatResponse.getId());
                context.setModel(chatResponse.getModel());
                context.setUsage(chatResponse.getUsage());
                context.setFinishReason(chatResponse.getFinishReason());
                ChatChunk chunk = metaChunk(context);
                chunk.setFinishReason(chatResponse.getFinishReason());
                chunk.setUsage(chatResponse.getUsage());
                consumer.onChunk(chunk);
                context.setStatus(StreamContext.STREAM_STATUS_COMPLETE);
                context.setResponse(chatResponse);
                consumer.onCompletion(chatResponse);
                break;
            }
            case "response.failed": {
                JSONObject response = object.getJSONObject("response");
                JSONObject error = response == null ? null : response.getJSONObject("error");
                String message = error == null ? "response failed" : error.getString("message");
                ChatResponse chatResponse = Provider.error(message);
                context.setStatus(StreamContext.STREAM_STATUS_COMPLETE);
                context.setResponse(chatResponse);
                consumer.onCompletion(chatResponse);
                break;
            }
            case "error": {
                // 全局错误事件：不带 response 对象，直接以 message 组装错误响应
                ChatResponse chatResponse = Provider.error(object.getString("message"));
                context.setStatus(StreamContext.STREAM_STATUS_COMPLETE);
                context.setResponse(chatResponse);
                consumer.onCompletion(chatResponse);
                break;
            }
            default:
                // output_text.done、content_part.done、output_item.done 等过程事件无需处理
                break;
        }
    }

    /**
     * 处理非流式响应
     *
     * @param response HTTP 响应
     * @return 响应消息
     */
    @Override
    public ChatResponse parseResponse(HttpResponse response) {
        JSONObject object = JSON.parseObject(response.body());
        JSONObject error = object.getJSONObject("error");
        if (error != null) {
            return Provider.error(error.getString("message"));
        }
        return toChatResponse(object);
    }

    /**
     * 将 Responses 协议的 response 对象转换为通用 ChatResponse
     * <p>
     * 遍历 output 项数组：message 提取 output_text / refusal 文本，reasoning 提取
     * summary 与 content 中的推理文本，function_call 转换为通用 ToolCall
     * （call_id 映射为 ToolCall.id，供下一轮以 tool_call_id 关联回传）。
     * </p>
     *
     * @param response Responses 协议 response 对象
     * @return 通用 ChatResponse
     */
    private ChatResponse toChatResponse(JSONObject response) {
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setRole(Message.ROLE_ASSISTANT);
        chatResponse.setId(response.getString("id"));
        chatResponse.setModel(response.getString("model"));
        chatResponse.setUsage(toUsage(response.getJSONObject("usage")));

        StringBuilder content = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();
        JSONArray output = response.getJSONArray("output");
        if (output != null) {
            for (int i = 0; i < output.size(); i++) {
                JSONObject item = output.getJSONObject(i);
                String type = item.getString("type");
                if ("message".equals(type)) {
                    appendContentParts(item.getJSONArray("content"), content);
                } else if ("reasoning".equals(type)) {
                    appendContentParts(item.getJSONArray("summary"), reasoning);
                    appendContentParts(item.getJSONArray("content"), reasoning);
                } else if ("function_call".equals(type)) {
                    ToolCall toolCall = new ToolCall();
                    toolCall.setIndex(toolCalls.size());
                    toolCall.setId(item.getString("call_id"));
                    toolCall.setName(item.getString("name"));
                    toolCall.setArguments(item.getString("arguments"));
                    toolCalls.add(toolCall);
                }
            }
        }
        if (content.length() > 0) {
            chatResponse.setContent(content.toString());
        }
        if (reasoning.length() > 0) {
            chatResponse.setReasoningContent(reasoning.toString());
        }
        chatResponse.setToolCalls(toolCalls);

        // 终态映射：failed 时以 error 对象组装错误响应；成功时按内容推断 finish_reason
        String status = response.getString("status");
        if ("failed".equals(status)) {
            JSONObject error = response.getJSONObject("error");
            chatResponse.setError(error == null ? "response failed" : error.getString("message"));
            chatResponse.setSuccess(false);
            return chatResponse;
        }
        if (!toolCalls.isEmpty()) {
            chatResponse.setFinishReason("tool_calls");
        } else if ("incomplete".equals(status)) {
            JSONObject incompleteDetails = response.getJSONObject("incomplete_details");
            String reason = incompleteDetails == null ? null : incompleteDetails.getString("reason");
            chatResponse.setFinishReason("max_output_tokens".equals(reason) ? "length" : "incomplete");
        } else {
            chatResponse.setFinishReason("stop");
        }
        chatResponse.setSuccess(true);
        return chatResponse;
    }

    /**
     * 提取内容片段数组中的文本：output_text 取 text，refusal 取 refusal
     */
    private static void appendContentParts(JSONArray parts, StringBuilder builder) {
        if (parts == null) {
            return;
        }
        for (int i = 0; i < parts.size(); i++) {
            JSONObject part = parts.getJSONObject(i);
            String type = part.getString("type");
            if ("output_text".equals(type)) {
                builder.append(part.getString("text"));
            } else if ("refusal".equals(type)) {
                builder.append(part.getString("refusal"));
            } else if ("summary_text".equals(type) || "reasoning_text".equals(type)) {
                builder.append(part.getString("text"));
            }
        }
    }

    /**
     * Responses 用量字段映射到通用 Usage：input_tokens → promptTokens、
     * output_tokens → completionTokens、total_tokens → totalTokens
     */
    private static Usage toUsage(JSONObject usageObj) {
        if (usageObj == null) {
            return null;
        }
        Usage usage = new Usage();
        usage.setPromptTokens(usageObj.getIntValue("input_tokens"));
        usage.setCompletionTokens(usageObj.getIntValue("output_tokens"));
        usage.setTotalTokens(usageObj.getIntValue("total_tokens"));
        return usage;
    }

    /**
     * 以流式上下文当前元数据构建 chunk
     */
    private static ChatChunk metaChunk(StreamContext context) {
        ChatChunk chunk = new ChatChunk();
        chunk.setId(context.getId());
        chunk.setModel(context.getModel());
        return chunk;
    }

    /**
     * 构建工具调用增量（仅携带当前已知字段，参数为全量累积值）
     */
    private static ToolCall incrementOf(int index, ToolCallBuilder builder) {
        ToolCall increment = builder.toToolCall();
        increment.setIndex(index);
        return increment;
    }

    /**
     * 合并系统提示：options.system 与 messages 中的 system 消息统一转为顶层 instructions
     */
    private String buildInstructions(List<Message> messages) {
        StringBuilder instructions = new StringBuilder();
        if (FeatUtils.isNotBlank(options.getSystem())) {
            instructions.append(options.getSystem());
        }
        for (Message message : messages) {
            if (Message.ROLE_SYSTEM.equals(message.getRole())) {
                String text = textOf(message);
                if (FeatUtils.isNotBlank(text)) {
                    if (instructions.length() > 0) {
                        instructions.append('\n');
                    }
                    instructions.append(text);
                }
            }
        }
        return instructions.length() > 0 ? instructions.toString() : null;
    }

    /**
     * 将通用 Message 列表转换为 Responses 协议的 input 项数组
     * <p>
     * tool 消息转为 function_call_output 项；assistant 消息拆为 message 项（文本以
     * output_text 表达）与独立的 function_call 项（call_id 关联）；user/system 消息
     * 的文本与图片内容转为 input_text / input_image 片段。
     * </p>
     *
     * @param messages 通用消息列表
     * @return Responses 协议格式的 input 项数组
     */
    private JSONArray toResponseInput(List<Message> messages) {
        JSONArray input = new JSONArray(messages.size());
        for (Message message : messages) {
            String role = message.getRole();
            if (Message.ROLE_SYSTEM.equals(role)) {
                // system 消息已并入 instructions，不再进 input
                continue;
            }
            if (Message.ROLE_TOOL.equals(role)) {
                input.add(JSONObject.of("type", "function_call_output",
                        "call_id", message.getToolCallId(), "output", textOf(message)));
                continue;
            }
            if (Message.ROLE_ASSISTANT.equals(role)) {
                String text = textOf(message);
                if (FeatUtils.isNotBlank(text)) {
                    input.add(JSONObject.of("type", "message", "role", Message.ROLE_ASSISTANT,
                            "content", JSONArray.of(JSONObject.of("type", "output_text", "text", text))));
                }
                if (message.getToolCalls() != null) {
                    for (ToolCall call : message.getToolCalls()) {
                        input.add(JSONObject.of("type", "function_call",
                                "call_id", call.getId(), "name", call.getName(), "arguments", call.getArguments()));
                    }
                }
                continue;
            }
            input.add(toInputMessage(message));
        }
        return input;
    }

    /**
     * 将 user 消息转换为 Responses 输入项：字符串内容原样透传（协议允许），
     * 多模态片段转为 input_text / input_image
     */
    private JSONObject toInputMessage(Message message) {
        JSONObject json = JSONObject.of("role", message.getRole());
        Object content = ((JSONObject) JSON.toJSON(message)).get("content");
        if (content instanceof String) {
            json.put("content", content);
            return json;
        }
        JSONArray parts = new JSONArray();
        if (content instanceof JSONArray) {
            for (int i = 0; i < ((JSONArray) content).size(); i++) {
                JSONObject part = ((JSONArray) content).getJSONObject(i);
                String type = part.getString("type");
                if ("text".equals(type)) {
                    parts.add(JSONObject.of("type", "input_text", "text", part.getString("text")));
                } else if ("image_url".equals(type)) {
                    // Chat Completions 的 image_url 为 {url: ...} 嵌套，Responses 直接用字符串
                    JSONObject imageUrl = part.getJSONObject("image_url");
                    String url = imageUrl == null ? part.getString("image_url") : imageUrl.getString("url");
                    parts.add(JSONObject.of("type", "input_image", "image_url", url));
                }
            }
        }
        json.put("content", parts);
        return json;
    }

    /**
     * 提取消息文本内容：字符串内容原样返回，多模态片段拼接 text 部分
     */
    private static String textOf(Message message) {
        Object content = ((JSONObject) JSON.toJSON(message)).get("content");
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof JSONArray) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < ((JSONArray) content).size(); i++) {
                JSONObject part = ((JSONArray) content).getJSONObject(i);
                if ("text".equals(part.getString("type"))) {
                    builder.append(part.getString("text"));
                }
            }
            return builder.toString();
        }
        return null;
    }
}
