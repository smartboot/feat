package tech.smartboot.feat.ai.chat.provider;

import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.StreamContext;
import tech.smartboot.feat.ai.chat.entity.Function;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.client.SseEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI 聊天 API 提供商抽象基类
 * <p>
 * 该类采用策略模式，定义了不同 AI 服务提供商（如 OpenAI、Anthropic 等）的统一接口。
 * 通过继承此类并实现抽象方法，可以支持多种不同的 AI API 规范。
 * </p>
 *
 * <h3>设计要点：</h3>
 * <ul>
 *   <li><b>策略模式</b>：每个具体的 Provider 实现代表一种 API 规范策略</li>
 *   <li><b>模板方法</b>：提供通用的错误处理方法，子类只需关注具体实现</li>
 *   <li><b>流式状态管理</b>：定义了流式响应的生命周期状态常量</li>
 *   <li><b>配置封装</b>：通过 ChatOptions 统一管理 API 配置信息</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 创建 OpenAI Provider
 * ChatOptions options = new ChatOptions()
 *     .baseUrl("https://api.openai.com/v1")
 *     .apiKey("your-api-key")
 *     .model("gpt-4");
 * Provider provider = new OpenAiProvider(options);
 *
 * // 非流式调用
 * provider.chat(messages, response -> {
 *     System.out.println(response.getContent());
 * });
 *
 * // 流式调用
 * provider.chatStream(messages, new StreamResponseCallback() {
 *     public void onStreamResponse(String content) {
 *         System.out.print(content); // 实时输出
 *     }
 *     public void onCompletion(ResponseMessage response) {
 *         System.out.println("\n完成");
 *     }
 * });
 * }</pre>
 *
 * @see OpenAiProvider OpenAI API 实现
 * @see AnthropicProvider Anthropic API 实现
 * @see ChatOptions 聊天配置选项
 * @see StreamResponseCallback 流式响应回调接口
 */
public abstract class Provider {
    /**
     * 聊天配置选项，包含 API 地址、密钥、模型等配置信息
     */
    protected final ChatOptions options;


    /**
     * 构造 Provider 实例
     *
     * @param options 聊天配置选项，包含 API 基础信息
     */
    public Provider(ChatOptions options) {
        this.options = options;
    }

    public abstract HttpRest buildRequest(List<Message> messages, boolean stream, List<Function> functions);

    /**
     * 处理流式聊天响应
     * <p>
     * 该方法用于实现 Server-Sent Events (SSE) 或类似的流式传输协议。
     * 实现类需要：
     * </p>
     * <ul>
     *   <li>构建流式请求（设置 stream=true）</li>
     *   <li>解析 SSE 数据流，提取文本片段</li>
     *   <li>通过回调的 {@link StreamResponseCallback#onStreamResponse(String)} 实时推送内容</li>
     *   <li>在流结束时调用 {@link StreamResponseCallback#onCompletion(ResponseMessage)} 传递完整响应</li>
     *   <li>在出错时调用 {@link StreamResponseCallback#onError(Throwable)} 报告异常</li>
     * </ul>
     *
     * <h3>流式响应生命周期：</h3>
     * <ol>
     *   <li>INIT → 发起请求</li>
     *   <li>UPGRADE → 收到第一个数据片段</li>
     *   <li>持续调用 onStreamResponse → 逐块推送内容</li>
     *   <li>COMPLETE → 收到 [DONE] 或结束标记，调用 onCompletion</li>
     *   <li>ERROR → 发生异常时调用 onError</li>
     * </ol>
     *
     * @param context
     * @param event
     * @param consumer 流式响应回调处理器，用于接收实时数据和最终结果
     * @see StreamResponseCallback 流式回调接口定义
     */
    public abstract void chatStream(StreamContext context, SseEvent event, StreamResponseCallback consumer);

    /**
     * 处理非流式聊天响应
     * <p>
     * 该方法用于实现传统的同步请求-响应模式。
     * 实现类需要：
     * </p>
     * <ul>
     *   <li>构建普通 HTTP 请求（设置 stream=false）</li>
     *   <li>等待完整的响应数据</li>
     *   <li>解析响应 JSON，提取消息内容、工具调用等信息</li>
     *   <li>通过回调的 {@link Consumer#accept(Object)} 一次性返回完整结果</li>
     * </ul>
     *
     * <h3>与非流式的区别：</h3>
     * <ul>
     *   <li>非流式：等待全部生成完成后一次性返回，适合短文本</li>
     *   <li>流式：边生成边返回，提供更好的用户体验，适合长文本</li>
     * </ul>
     *
     * @param response
     * @see ResponseMessage 响应消息结构
     */
    public abstract ResponseMessage chat(HttpResponse response);

    /**
     * 创建错误响应消息
     * <p>
     * 这是一个静态工厂方法，用于快速构建表示错误的响应对象。
     * 通常在以下场景使用：
     * </p>
     * <ul>
     *   <li>HTTP 请求失败（状态码非 200）</li>
     *   <li>API 返回错误信息</li>
     *   <li>网络异常或超时</li>
     *   <li>参数验证失败</li>
     * </ul>
     *
     * <h3>错误响应特征：</h3>
     * <ul>
     *   <li>角色固定为 {@link Message#ROLE_ASSISTANT}</li>
     *   <li>success 字段为 false</li>
     *   <li>error 字段包含错误描述</li>
     *   <li>content 字段通常为空或null</li>
     * </ul>
     *
     * @param error 错误描述信息，可以是错误消息、异常堆栈或 HTTP 响应体
     * @return 标准化的错误响应消息对象
     * @see ResponseMessage#getError() 获取错误信息
     * @see ResponseMessage#isSuccess() 检查是否成功
     */
    public static ResponseMessage error(String error) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setRole(Message.ROLE_ASSISTANT);
        responseMessage.setError(error);
        responseMessage.setSuccess(false);
        return responseMessage;
    }
}
