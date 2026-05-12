package tech.smartboot.feat.ai.chat.provider;

import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ChatResponse;
import tech.smartboot.feat.ai.chat.ChatStreamListener;
import tech.smartboot.feat.ai.chat.provider.anthropic.AnthropicProvider;
import tech.smartboot.feat.ai.chat.provider.openai.OpenAiProvider;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.client.SseEvent;

import java.util.List;

/**
 * AI 聊天 API 提供商抽象基类
 * <p>
 * 采用策略模式定义不同 AI 服务商（OpenAI、Anthropic 等）的统一接口。
 * 子类通过实现抽象方法支持各自的 API 规范。
 * </p>
 *
 * <h3>设计要点：</h3>
 * <ul>
 *   <li><b>策略模式</b>：每个 Provider 实现代表一种 API 规范策略</li>
 *   <li><b>模板方法</b>：提供通用错误处理，子类专注具体实现</li>
 *   <li><b>配置封装</b>：通过 ChatOptions 统一管理 API 配置</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * ChatOptions options = new ChatOptions()
 *     .baseUrl("https://api.openai.com/v1")
 *     .apiKey("your-api-key")
 *     .model("gpt-4");
 * Provider provider = new OpenAiProvider(options);
 * }</pre>
 *
 * @see OpenAiProvider OpenAI API 实现
 * @see AnthropicProvider Anthropic API 实现
 * @see ChatOptions 聊天配置选项
 * @see ChatStreamListener 流式响应回调接口
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

    /**
     * 创建聊天请求
     * <p>
     * 构建符合特定 AI 服务商规范的 HTTP 请求。
     * </p>
     *
     * @param messages  消息列表，包含对话历史
     * @param stream   是否启用流式响应
     * @param tools 工具函数列表
     * @return HttpRest 请求对象
     * @see HttpRest Feat HTTP 请求接口
     */
    public abstract HttpRest createRequest(List<Message> messages, boolean stream, List<Tool> tools);

    /**
     * 解析流式聊天响应（SSE 模式）
     * <p>
     * 实现 Server-Sent Events 流式传输协议，实时推送生成内容。
     * </p>
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>解析 SSE 数据流，提取文本片段</li>
     *   <li>通过 {@link ChatStreamListener#onStreamResponse(String)} 实时推送</li>
     *   <li>流结束时调用 {@link ChatStreamListener#onCompletion(ChatResponse)}</li>
     * </ol>
     *
     * @param context  流式上下文，累积状态和数据
     * @param event    SSE 事件
     * @param consumer 流式回调处理器
     * @see ChatStreamListener 流式回调接口
     */
    public abstract void parseStreamResponse(StreamContext context, SseEvent event, ChatStreamListener consumer);

    /**
     * 解析非流式聊天响应
     * <p>
     * 实现同步请求-响应模式，一次性返回完整结果。
     * </p>
     *
     * <h3>与流式区别：</h3>
     * <ul>
     *   <li>非流式：等待全部生成后一次性返回，包含完整 Usage 统计</li>
     *   <li>流式：边生成边返回，用户体验更好</li>
     * </ul>
     *
     * @param response HTTP 响应对象
     * @return 解析后的响应消息
     * @see ChatResponse 响应消息结构
     */
    public abstract ChatResponse parseResponse(HttpResponse response);

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
     * @see ChatResponse#getError() 获取错误信息
     * @see ChatResponse#isSuccess() 检查是否成功
     */
    public static ChatResponse error(String error) {
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setRole(Message.ROLE_ASSISTANT);
        chatResponse.setError(error);
        chatResponse.setSuccess(false);
        return chatResponse;
    }
}
