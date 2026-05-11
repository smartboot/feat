package tech.smartboot.feat.ai.chat.provider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.entity.Function;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.ai.chat.entity.Usage;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Anthropic API 规范处理器实现
 * <p>
 * 该类实现了 Anthropic Messages API 的通信逻辑，支持 Claude 系列模型。
 * Anthropic API 与 OpenAI API 在协议上有显著差异：
 * </p>
 *
 * <h3>核心差异：</h3>
 * <table border="1">
 *   <tr><th>特性</th><th>OpenAI</th><th>Anthropic</th></tr>
 *   <tr><td>系统提示</td><td>放在 messages 数组中</td><td>独立的 system 字段</td></tr>
 *   <tr><td>最大 token</td><td>可选参数</td><td>必填参数（max_tokens）</td></tr>
 *   <tr><td>认证头</td><td>Authorization: Bearer</td><td>x-api-key</td></tr>
 *   <tr><td>版本控制</td><td>URL 路径或查询参数</td><td>anthropic-version 请求头</td></tr>
 *   <tr><td>流式事件</td><td>统一 data 格式</td><td>多种 event type</td></tr>
 *   <tr><td>内容结构</td><td>简单字符串</td><td>content 数组（支持多模态）</td></tr>
 * </table>
 *
 * <h3>API 端点：</h3>
 * <ul>
 *   <li>流式/非流式：POST {baseUrl}/v1/messages</li>
 *   <li>认证方式：x-api-key: {apiKey}</li>
 *   <li>版本控制：anthropic-version: 2023-06-01</li>
 * </ul>
 *
 * <h3>支持的模型：</h3>
 * <ul>
 *   <li>Claude 3.5 Sonnet（最新、最强）</li>
 *   <li>Claude 3.5 Haiku（快速、经济）</li>
 *   <li>Claude 3 Opus（复杂任务）</li>
 *   <li>Claude 3 Sonnet（平衡性能）</li>
 *   <li>Claude 3 Haiku（高速响应）</li>
 * </ul>
 *
 * <h3>流式事件类型：</h3>
 * <ol>
 *   <li><b>message_start</b>：消息开始，包含初始元数据</li>
 *   <li><b>content_block_start</b>：内容块开始</li>
 *   <li><b>content_block_delta</b>：内容增量（实际文本在此事件中传输）</li>
 *   <li><b>content_block_stop</b>：内容块结束</li>
 *   <li><b>message_delta</b>：消息级别的变化（如停止原因）</li>
 *   <li><b>message_stop</b>：消息完全结束</li>
 * </ol>
 *
 * <h3>Thinking 模式：</h3>
 * <p>Claude 支持 "thinking" 字段，用于输出模型的推理过程（类似思维链）。</p>
 * <p>通过 {@link StreamResponseCallback#onReasoning(String)} 实时推送思考内容。</p>
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
     * 与 OpenAI 的主要区别：
     * </p>
     * <ul>
     *   <li>系统提示作为独立字段（system），而非 messages 的一部分</li>
     *   <li>必须指定 max_tokens（默认 4096）</li>
     *   <li>工具定义格式不同（使用 input_schema 而非 parameters）</li>
     *   <li>认证头使用 x-api-key 而非 Authorization</li>
     * </ul>
     *
     * @param messages 消息列表，包含对话历史
     * @param stream   是否启用流式响应（true=SSE，false=普通 JSON）
     * @return 配置好的 HttpPost 请求对象
     */
    private HttpPost buildRequest(List<Message> messages, boolean stream, List<Function> functions) {
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
     * 该方法实现 Anthropic 标准的 Server-Sent Events 流式传输协议。
     * 与 OpenAI 不同，Anthropic 使用多种事件类型来区分不同的流式阶段。
     * </p>
     *
     * <h3>SSE 事件流程示例：</h3>
     * <pre>{@code
     * event: message_start
     * data: {"type": "message_start", "message": {...}}
     *
     * event: content_block_start
     * data: {"type": "content_block_start", "index": 0, "content_block": {"type": "text"}}
     *
     * event: content_block_delta
     * data: {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": "Hello"}}
     *
     * event: content_block_delta
     * data: {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": " World"}}
     *
     * event: content_block_stop
     * data: {"type": "content_block_stop", "index": 0}
     *
     * event: message_delta
     * data: {"type": "message_delta", "delta": {"stop_reason": "end_turn"}}
     *
     * event: message_stop
     * data: {"type": "message_stop"}
     * }</pre>
     *
     * <h3>关键处理逻辑：</h3>
     * <ol>
     *   <li><b>状态管理</b>：使用 AtomicInteger 跟踪流式生命周期</li>
     *   <li><b>事件分发</b>：根据 eventType 路由到不同的处理分支</li>
     *   <li><b>内容提取</b>：从 content_block_delta 事件的 delta.text 提取文本片段</li>
     *   <li><b>推理内容</b>：从 delta.thinking 提取思维链内容（Claude Thinking 模式）</li>
     *   <li><b>完成触发</b>：收到 message_stop 事件时，组装完整响应并调用 onCompletion</li>
     *   <li><b>错误处理</b>：捕获解析异常，记录日志但不中断流</li>
     * </ol>
     *
     * <h3>与 OpenAI 的差异：</h3>
     * <ul>
     *   <li>OpenAI：所有数据都在 data 字段的 JSON 中，通过 choices[0].delta 提取</li>
     *   <li>Anthropic：使用 event 字段区分事件类型，数据结构更扁平</li>
     *   <li>OpenAI：用 "[DONE]" 标记结束</li>
     *   <li>Anthropic：用 message_stop 事件标记结束</li>
     * </ul>
     *
     * @param messages 消息列表，包含用户、系统、助手的对话历史
     * @param consumer 流式响应回调，接收实时内容和最终结果
     */
    @Override
    public void chatStream(List<Message> messages, List<Function> functions, StreamResponseCallback consumer) {
        HttpPost post = buildRequest(messages, true, functions);
        // 文本内容累积器
        StringBuilder contentBuilder = new StringBuilder();
        // 推理内容累积器（Claude Thinking 模式）
        StringBuilder reasoningBuilder = new StringBuilder();
        // 工具调用累积器
        Map<Integer, ToolCall> toolCallMap = new HashMap<>();
        // 当前内容块索引
        int[] currentBlockIndex = {0};
        // 流式状态跟踪器
        AtomicInteger status = new AtomicInteger(STREAM_STATUS_INIT);

        // 注册 SSE 事件处理器
        post.onSSE(sse -> sse.onData(event -> {
                    // 首次收到数据，标记为 UPGRADE 状态
                    if (status.get() == STREAM_STATUS_INIT) {
                        status.set(STREAM_STATUS_UPGRADE);
                    }

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
                                // 内容块开始事件，标识新内容块的起始
                                currentBlockIndex[0] = object.getIntValue("index");
                                break;
                            case "content_block_delta":
                                // 内容增量事件（核心事件，包含实际文本或工具调用）
                                JSONObject delta = object.getJSONObject("delta");
                                if (delta != null) {
                                    // 提取文本片段
                                    String text = delta.getString("text");
                                    if (text != null) {
                                        consumer.onStreamResponse(text); // 实时推送
                                        contentBuilder.append(text);      // 累积保存
                                    }
                                    // 提取推理内容（Claude Thinking 模式）
                                    String thinking = delta.getString("thinking");
                                    if (thinking != null) {
                                        consumer.onReasoning(thinking); // 实时推送
                                        reasoningBuilder.append(thinking); // 累积保存
                                    }
                                    // 提取工具调用信息
                                    JSONObject input = delta.getJSONObject("input");
                                    if (input != null) {
                                        ToolCall toolCall = toolCallMap.computeIfAbsent(currentBlockIndex[0], k -> {
                                            ToolCall t = new ToolCall();
                                            t.setIndex(currentBlockIndex[0]);
                                            t.setType("function");
                                            t.setFunction(new HashMap<>());
                                            return t;
                                        });
                                        // 累积 input JSON 字符串
                                        String inputStr = input.toJSONString();
                                        String existingInput = toolCall.getFunction().get("arguments");
                                        if (existingInput != null) {
                                            toolCall.getFunction().put("arguments", existingInput + inputStr);
                                        } else {
                                            toolCall.getFunction().put("arguments", inputStr);
                                        }
                                    }
                                    // 提取工具名称
                                    String name = delta.getString("name");
                                    if (name != null) {
                                        ToolCall toolCall = toolCallMap.computeIfAbsent(currentBlockIndex[0], k -> {
                                            ToolCall t = new ToolCall();
                                            t.setIndex(currentBlockIndex[0]);
                                            t.setType("function");
                                            t.setFunction(new HashMap<>());
                                            return t;
                                        });
                                        toolCall.getFunction().put("name", name);
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
                                responseMessage.setContent(contentBuilder.toString());
                                responseMessage.setReasoningContent(reasoningBuilder.toString());
                                responseMessage.setToolCalls(new ArrayList<>(toolCallMap.values()));
                                responseMessage.setSuccess(true);
                                status.set(STREAM_STATUS_COMPLETE);
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
                }))
                // HTTP 成功但流式未启动：说明请求失败（如 401、429）
                .onSuccess(response -> {
                    if (status.get() == Provider.STREAM_STATUS_INIT) {
                        status.set(Provider.STREAM_STATUS_ERROR);
                        consumer.onError(new FeatException(response.body()));
                    }
                })
                // 网络异常或连接失败
                .onFailure(throwable -> {
                    status.set(Provider.STREAM_STATUS_ERROR);
                    consumer.onError(throwable);
                })
                // 提交请求
                .submit();
    }

    /**
     * 处理非流式聊天响应
     * <p>
     * 该方法实现传统的同步请求-响应模式。
     * Anthropic 的响应结构与 OpenAI 有显著差异：
     * </p>
     *
     * <h3>响应结构示例：</h3>
     * <pre>{@code
     * {
     *   "id": "msg_01ABC123",
     *   "type": "message",
     *   "role": "assistant",
     *   "content": [
     *     {
     *       "type": "text",
     *       "text": "完整回复内容"
     *     }
     *   ],
     *   "model": "claude-3-5-sonnet-20241022",
     *   "stop_reason": "end_turn",
     *   "stop_sequence": null,
     *   "usage": {
     *     "input_tokens": 10,
     *     "output_tokens": 50
     *   }
     * }
     * }</pre>
     *
     * <h3>关键差异：</h3>
     * <ul>
     *   <li><b>content 是数组</b>：支持多模态（文本、图像等），需遍历提取</li>
     *   <li><b>Usage 字段名不同</b>：input_tokens/output_tokens 而非 prompt_tokens/completion_tokens</li>
     *   <li><b>无 tool_calls 字段</b>：工具调用通过 content 数组中的 tool_use 类型表示</li>
     *   <li><b>stop_reason</b>：明确标识停止原因（end_turn、max_tokens、stop_sequence 等）</li>
     * </ul>
     *
     * <h3>当前实现的局限性：</h3>
     * <ul>
     *   <li>❌ 未提取工具调用信息（需要额外处理 content 数组中的 tool_use 类型）</li>
     *   <li>✅ 已正确提取文本内容（遍历 content 数组）</li>
     *   <li>✅ 已正确提取 Usage 统计（映射到统一的 Usage 对象）</li>
     * </ul>
     *
     * @param messages 消息列表，包含用户、系统、助手的对话历史
     */
    @Override
    public CompletableFuture<ResponseMessage> chat(List<Message> messages, List<Function> functions) {
        HttpPost post = buildRequest(messages, false, functions);
        return post.submit().thenApply(response -> {
            // 检查 HTTP 状态码
            if (response.statusCode() != 200) {
                return Provider.error(response.body());
            }

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
        });
    }

    public AnthropicProvider maxTokens(int maxTokens) {
        options.extraBody(json -> json.put("max_tokens", maxTokens));
        return this;
    }
}
