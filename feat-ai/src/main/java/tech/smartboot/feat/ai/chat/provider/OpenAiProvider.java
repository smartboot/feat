package tech.smartboot.feat.ai.chat.provider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.CompletionHandler;
import tech.smartboot.feat.ai.chat.entity.ChatWholeResponse;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * OpenAI API 规范处理器实现
 * <p>
 * 该类实现了 OpenAI Chat Completions API 的通信逻辑，支持：
 * </p>
 * <ul>
 *   <li><b>流式响应</b>：基于 Server-Sent Events (SSE) 协议，实时接收生成内容</li>
 *   <li><b>非流式响应</b>：传统 HTTP POST 请求，一次性返回完整结果</li>
 *   <li><b>工具调用</b>：支持 Function Calling，允许模型调用外部函数</li>
 *   <li><b>推理内容</b>：支持 reasoning_content 字段，获取模型的思考过程</li>
 *   <li><b>额外参数</b>：通过 extraBody 支持模型特定的高级配置</li>
 * </ul>
 *
 * <h3>API 端点：</h3>
 * <ul>
 *   <li>流式/非流式：POST {baseUrl}/chat/completions</li>
 *   <li>认证方式：Bearer Token（Authorization: Bearer {apiKey}）</li>
 * </ul>
 *
 * <h3>兼容的服务商：</h3>
 * <p>由于采用 OpenAI 标准协议，以下服务商也可直接使用此 Provider：</p>
 * <ul>
 *   <li>Azure OpenAI Service</li>
 *   <li>阿里云通义千问（DashScope）</li>
 *   <li>百度文心一言</li>
 *   <li>智谱 GLM</li>
 *   <li>DeepSeek</li>
 *   <li>以及其他兼容 OpenAI 格式的 API 服务</li>
 * </ul>
 *
 * <h3>流式响应处理流程：</h3>
 * <ol>
 *   <li>发起 SSE 请求，设置 stream=true</li>
 *   <li>解析每个 SSE 事件的 data 字段（JSON 格式）</li>
 *   <li>从 choices[0].delta 提取增量内容：</li>
 *   <ul>
 *     <li>content：文本内容片段</li>
 *     <li>reasoning_content：推理过程片段</li>
 *     <li>tool_calls：工具调用信息（需累积拼接）</li>
 *   </ul>
 *   <li>收到 "[DONE]" 标记时，组装完整响应并调用 onCompletion</li>
 *   <li>发生错误时，调用 onError 报告异常</li>
 * </ol>
 *
 * <h3>工具调用累积策略：</h3>
 * <p>由于流式响应中 tool_calls 是分片传输的，需要使用 Map&lt;Integer, ToolCall&gt; 进行累积：</p>
 * <ul>
 *   <li>使用 index 作为 key，区分多个并行工具调用</li>
 *   <li>首次收到时创建 ToolCall 对象，初始化 function 为 HashMap</li>
 *   <li>后续收到同 index 的分片时，将参数字符串拼接到现有值</li>
 *   <li>最终在 onCompletion 时传递完整的 toolCalls 列表</li>
 * </ul>
 *
 * @see Provider 抽象基类
 * @see AnthropicProvider Anthropic API 实现
 */
public class OpenAiProvider extends Provider {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiProvider.class);
    public static final Consumer<JSONObject> responseJsonFormat = (jsonObject) -> jsonObject.put("response_format", JSONObject.of("type", "json_object"));

    public OpenAiProvider(ChatOptions options) {
        super(options);
    }

    /**
     * 构建 HTTP POST 请求
     * <p>
     * 根据 OpenAI API 规范构造请求体和请求头。
     * </p>
     *
     * @param messages 消息列表，包含对话历史
     * @param stream   是否启用流式响应（true=SSE，false=普通 JSON）
     * @return 配置好的 HttpPost 请求对象
     */
    private HttpPost buildRequest(List<Message> messages, boolean stream) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", options.getModel());
        jsonObject.put("stream", stream);
        jsonObject.put("messages", addSystemMessageIfNeeded(messages));

        // 构建工具列表（Function Calling）
        List<Tool> toolList = new ArrayList<>();
        options.functions().forEach((tool, function) -> {
            Tool t = new Tool();
            t.setType("function");
            t.setFunction(function);
            toolList.add(t);
        });
        if (!toolList.isEmpty()) {
            jsonObject.put("tools", toolList);
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
        }, header -> {
            // 添加认证头（Bearer Token）
            if (FeatUtils.isNotBlank(options.apiKey())) {
                header.add(HeaderName.AUTHORIZATION, "Bearer " + options.apiKey());
            }
            // 添加自定义请求头
            options.getHeaders().forEach(header::add);
        }, jsonObject);
    }

    /**
     * 处理流式聊天响应（SSE 模式）
     * <p>
     * 该方法实现 OpenAI 标准的 Server-Sent Events 流式传输协议。
     * 每个 SSE 事件的 data 字段包含一个 JSON 对象，结构如下：
     * </p>
     * <pre>{@code
     * {
     *   "choices": [{
     *     "delta": {
     *       "content": "文本片段",
     *       "reasoning_content": "推理片段",
     *       "tool_calls": [{ "index": 0, "id": "...", "function": {...} }]
     *     }
     *   }]
     * }
     * }</pre>
     *
     * <h3>关键处理逻辑：</h3>
     * <ol>
     *   <li><b>状态管理</b>：使用 AtomicInteger 跟踪流式生命周期（INIT → UPGRADE → COMPLETE/ERROR）</li>
     *   <li><b>内容累积</b>：使用 StringBuilder 拼接所有文本片段</li>
     *   <li><b>推理内容</b>：单独累积 reasoning_content，供需要思维链的场景使用</li>
     *   <li><b>工具调用</b>：使用 Map&lt;Integer, ToolCall&gt; 按索引累积分片的工具调用信息</li>
     *   <li><b>错误处理</b>：检测 error 字段，提前终止并返回错误响应</li>
     *   <li><b>终止条件</b>：收到 "[DONE]" 标记或空数据时，触发 onCompletion</li>
     * </ol>
     *
     * <h3>工具调用累积示例：</h3>
     * <p>假设模型调用 search_web(query="AI")，流式数据可能分三次到达：</p>
     * <ol>
     *   <li>第1次：{"index": 0, "function": {"name": "search"}}</li>
     *   <li>第2次：{"index": 0, "function": {"arguments": "{\"query\": \"A"}}</li>
     *   <li>第3次：{"index": 0, "function": {"arguments": "I\"}"}}</li>
     * </ol>
     * <p>最终累积为：{"name": "search", "arguments": "{\"query\": \"AI\"}"}</p>
     *
     * @param messages 消息列表，包含用户、系统、助手的对话历史
     * @param consumer 流式响应回调，接收实时内容和最终结果
     */
    @Override
    public void chatStream(List<Message> messages, StreamResponseCallback consumer) {
        HttpPost post = buildRequest(messages, true);
        // 工具调用累积器：key=index, value=ToolCall
        Map<Integer, ToolCall> toolCallMap = new HashMap<>();
        // 文本内容累积器
        StringBuilder contentBuilder = new StringBuilder();
        // 推理内容累积器
        StringBuilder reasoningBuilder = new StringBuilder();
        // 流式状态跟踪器
        AtomicInteger status = new AtomicInteger(STREAM_STATUS_INIT);

        // 注册 SSE 事件处理器
        post.onSSE(sse -> sse.onData(event -> {
                    // 首次收到数据，标记为 UPGRADE 状态
                    if (status.get() == STREAM_STATUS_INIT) {
                        status.set(STREAM_STATUS_UPGRADE);
                    }

                    String data = event.getData();
                    // 终止标记或空数据：触发完成回调
                    if ("[DONE]".equals(data) || FeatUtils.isBlank(data)) {
                        // 防止重复触发（已完成且无新内容）
                        if (status.get() == STREAM_STATUS_COMPLETE && contentBuilder.length() == 0) {
                            return;
                        }
                        // 构建完整响应消息
                        ResponseMessage responseMessage = new ResponseMessage();
                        responseMessage.setRole(Message.ROLE_ASSISTANT);
                        responseMessage.setContent(contentBuilder.toString());
                        responseMessage.setReasoningContent(reasoningBuilder.toString());
                        responseMessage.setToolCalls(new ArrayList<>(toolCallMap.values()));
                        responseMessage.setSuccess(true);
                        status.set(STREAM_STATUS_COMPLETE);
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
                        status.set(STREAM_STATUS_COMPLETE);
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
                        contentBuilder.append(content);      // 累积保存
                    }

                    // 提取推理内容片段
                    String reasoningContent = delta.getString("reasoning_content");
                    if (reasoningContent != null) {
                        consumer.onReasoning(reasoningContent); // 实时推送
                        reasoningBuilder.append(reasoningContent); // 累积保存
                    }

                    // 提取工具调用信息（可能为空或分片）
                    List<ToolCall> toolCalls = delta.getObject("tool_calls", new TypeReference<List<ToolCall>>() {
                    });
                    if (FeatUtils.isNotEmpty(toolCalls)) {
                        for (ToolCall toolCall : toolCalls) {
                            // 根据 index 获取或创建 ToolCall 对象
                            ToolCall tool = toolCallMap.computeIfAbsent(toolCall.getIndex(), k -> {
                                ToolCall t = new ToolCall();
                                t.setFunction(new HashMap<>());
                                return t;
                            });

                            // 更新基础字段（仅首次有值）
                            if (FeatUtils.isNotBlank(toolCall.getId())) {
                                tool.setId(toolCall.getId());
                            }
                            if (FeatUtils.isNotBlank(toolCall.getType())) {
                                tool.setType(toolCall.getType());
                            }

                            // 累积函数参数字段（字符串拼接）
                            if (toolCall.getFunction() != null) {
                                toolCall.getFunction().forEach((k, v) -> {
                                    if (v == null) {
                                        return;
                                    }
                                    String preV = tool.getFunction().get(k);
                                    if (FeatUtils.isNotBlank(preV)) {
                                        // 追加到已有值后面
                                        tool.getFunction().put(k, preV + v);
                                    } else {
                                        // 首次设置
                                        tool.getFunction().put(k, v);
                                    }
                                });
                            }
                        }
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
     * 该方法实现传统的同步请求-响应模式，适用于以下场景：
     * </p>
     * <ul>
     *   <li>短文本生成（无需实时显示进度）</li>
     *   <li>批量处理任务（追求吞吐量而非延迟）</li>
     *   <li>需要完整 Usage 统计信息</li>
     *   <li>服务端内部调用（用户体验不是首要考虑）</li>
     * </ul>
     *
     * <h3>响应结构：</h3>
     * <pre>{@code
     * {
     *   "choices": [{
     *     "message": {
     *       "role": "assistant",
     *       "content": "完整回复内容",
     *       "tool_calls": [...]
     *     }
     *   }],
     *   "usage": {
     *     "prompt_tokens": 10,
     *     "completion_tokens": 50,
     *     "total_tokens": 60
     *   },
     *   "prompt_logprobs": "..."
     * }
     * }</pre>
     *
     * <h3>与流式的对比：</h3>
     * <table border="1">
     *   <tr><th>特性</th><th>非流式</th><th>流式</th></tr>
     *   <tr><td>首字延迟</td><td>高（等待全部生成）</td><td>低（立即返回首个 token）</td></tr>
     *   <tr><td>总耗时</td><td>相同</td><td>相同</td></tr>
     *   <tr><td>Usage 统计</td><td>✅ 包含</td><td>❌ 通常不包含</td></tr>
     *   <tr><td>用户体验</td><td>较差（白屏等待）</td><td>优秀（逐字显示）</td></tr>
     *   <tr><td>网络稳定性</td><td>更稳定</td><td>长连接易中断</td></tr>
     * </table>
     *
     * @param messages 消息列表，包含用户、系统、助手的对话历史
     * @param callback 响应回调，接收完整的响应消息对象
     */
    @Override
    public void chat(List<Message> messages, CompletionHandler callback) {
        HttpPost post = buildRequest(messages, false);
        post.onSuccess(response -> {
                    // 检查 HTTP 状态码
                    if (response.statusCode() != 200) {
                        callback.completed(Provider.error(response.body()));
                        return;
                    }

                    // 解析完整响应（使用强类型对象）
                    ChatWholeResponse chatResponse = JSON.parseObject(response.body(), ChatWholeResponse.class);

                    // 提取响应消息
                    ResponseMessage responseMessage = chatResponse.getChoice().getMessage();

                    // 附加元数据（Usage、Logprobs）
                    responseMessage.setUsage(chatResponse.getUsage());
                    responseMessage.setPromptLogprobs(chatResponse.getPromptLogprobs());
                    responseMessage.setSuccess(true);

                    // 触发回调
                    callback.completed(responseMessage);
                })
                .onFailure(callback::failed)
                // 提交请求
                .submit();
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
     * 需在提示词中明确指示模型输出JSON，如：“请按照json格式输出”，否则会报错。
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
