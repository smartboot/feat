package tech.smartboot.feat.ai.chat.provider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.StreamContext;
import tech.smartboot.feat.ai.chat.entity.Function;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.ai.chat.entity.Usage;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.SseEvent;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
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

    public AnthropicProvider(ChatOptions options) {
        super(options);
    }

    /**
     * 构建 HTTP POST 请求
     * <p>
     * 根据 Anthropic API 规范构造请求体和请求头。
     * </p>
     *
     * @param messages  消息列表
     * @param stream    是否启用流式响应
     * @param functions 工具函数列表
     * @return HttpPost 请求对象
     */
    public HttpPost createRequest(List<Message> messages, boolean stream, List<Function> functions) {
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

        // 添加用户消息列表
        jsonObject.put("messages", messages);

        // 处理工具定义（Function Calling）
        if (FeatUtils.isNotEmpty(functions)) {
            JSONArray toolsArray = new JSONArray();
            functions.forEach(tool -> {
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
     * 处理流式聊天响应（SSE 模式）
     * <p>
     * 实现 Anthropic SSE 流式传输协议，使用多种事件类型区分流式阶段。
     * </p>
     *
     * <h3>处理逻辑：</h3>
     * <ol>
     *   <li>根据 eventType 分发处理</li>
     *   <li>从 content_block_delta 提取文本（delta.text）</li>
     *   <li>提取推理内容（delta.thinking）</li>
     *   <li>收到 message_stop 时组装完整响应并调用 onCompletion</li>
     * </ol>
     *
     * @param context  流式上下文
     * @param event    SSE 事件
     * @param consumer 流式回调
     */
    @Override
    public void parseStreamResponse(StreamContext context, SseEvent event, StreamResponseCallback consumer) {
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
                case "message_start":
                    // 消息开始事件，通常包含元数据，此处无需处理
                    break;
                case "content_block_start":
                    break;
                case "content_block_delta":
                    // 内容增量事件（核心事件，包含实际文本或工具调用）
                    JSONObject delta = object.getJSONObject("delta");
                    if (delta != null) {
                        // 提取文本片段
                        String text = delta.getString("text");
                        if (text != null) {
                            consumer.onStreamResponse(text); // 实时推送
                            context.appendContent(text);      // 累积保存
                        }
                        // 提取推理内容（Claude Thinking 模式）
                        String thinking = delta.getString("thinking");
                        if (thinking != null) {
                            consumer.onReasoning(thinking); // 实时推送
                            context.appendReasoning(thinking); // 累积保存
                        }
                    }
                    break;
                case "content_block_stop":
                    // 内容块结束事件
                    break;
                case "message_delta":
                    // 消息级别变化事件（如 stop_reason）
                    break;
                case "message_stop":
                    // 消息完全结束，触发完成回调
                    ResponseMessage responseMessage = new ResponseMessage();
                    responseMessage.setRole(Message.ROLE_ASSISTANT);
                    responseMessage.setContent(context.getContent());
                    responseMessage.setReasoningContent(context.getReasoning());
                    responseMessage.setSuccess(true);
                    context.setStatus(StreamContext.STREAM_STATUS_COMPLETE);
                    consumer.onCompletion(responseMessage);
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
    public ResponseMessage parseResponse(HttpResponse response) {
        // 解析响应 JSON
        JSONObject object = JSON.parseObject(response.body());
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setRole(Message.ROLE_ASSISTANT);
        responseMessage.setSuccess(true);

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
                    toolCall.setType("function");
                    Map<String, String> functionMap = new HashMap<>();
                    functionMap.put("name", contentItem.getString("name"));
                    functionMap.put("arguments", contentItem.getJSONObject("input") != null
                            ? contentItem.getJSONObject("input").toJSONString() : "{}");
                    toolCall.setFunction(functionMap);
                    toolCalls.add(toolCall);
                }
            }
            responseMessage.setContent(contentBuilder.toString());
        }
        // 设置工具调用
        if (!toolCalls.isEmpty()) {
            responseMessage.setToolCalls(toolCalls);
        }

        // 提取使用统计：映射 Anthropic 的字段名到统一的 Usage 对象
        JSONObject usage = object.getJSONObject("usage");
        if (usage != null) {
            Usage usageObj = new Usage();
            // Anthropic 使用 input_tokens/output_tokens
            usageObj.setPromptTokens(usage.getInteger("input_tokens") != null ? usage.getInteger("input_tokens") : 0);
            usageObj.setCompletionTokens(usage.getInteger("output_tokens") != null ? usage.getInteger("output_tokens") : 0);
            responseMessage.setUsage(usageObj);
        }

        // 触发回调
        return responseMessage;
    }

    public AnthropicProvider maxTokens(int maxTokens) {
        options.extraBody(json -> json.put("max_tokens", maxTokens));
        return this;
    }
}
