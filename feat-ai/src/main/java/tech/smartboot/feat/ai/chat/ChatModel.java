/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *   without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat;

import tech.smartboot.feat.ai.chat.entity.ChatResponse;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.provider.Provider;
import tech.smartboot.feat.ai.chat.provider.StreamContext;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ChatModel - 聊天模型客户端
 *
 * <p>用于与AI模型进行交互，支持流式和非流式响应。
 * 支持多种API规范，包括 OpenAI、Anthropic 等主流AI服务提供商。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ChatOptions options = ChatOptions.builder()
 *     .baseUrl("https://api.openai.com/v1")
 *     .apiKey("your-api-key")
 *     .model("gpt-4")
 *     .build();
 *
 * ChatModel chatModel = new ChatModel(options);
 *
 * // 非流式调用
 * CompletableFuture<ChatResponse> future = chatModel.chat("Hello");
 * future.thenAccept(response -> System.out.println(response.getContent()));
 *
 * // 流式调用：回调接收增量内容，返回的 future 在流终态完成
 * chatModel.chatStream("Hello", content -> System.out.print(content))
 *         .thenAccept(response -> System.out.println("完成: " + response.getContent()));
 * }</pre>
 *
 * @author 三刀
 */
public class ChatModel {
    private final ChatOptions options;

    /**
     * 构造 ChatModel 实例
     *
     * <p>创建聊天模型客户端，自动处理URL末尾的斜杠。</p>
     *
     * @param options 聊天选项配置，包含API地址、密钥、模型等参数
     * @see ChatOptions
     */
    public ChatModel(ChatOptions options) {
        if (options.baseUrl().endsWith("/")) {
            options.baseUrl(options.baseUrl().substring(0, options.baseUrl().length() - 1));
        }
        this.options = options;
    }

    /**
     * 发送流式聊天请求（简单版本）
     *
     * <p>使用单条用户消息发起流式请求，适用于简单对话场景。</p>
     *
     * @param content  用户输入内容
     * @param consumer 流式响应回调，每收到一个响应块时触发
     * @return 终态 future：流正常结束时以完整响应完成，异常时异常完成
     * @see #chatStream(String, List, ChatStreamListener)
     */
    public CompletableFuture<ChatResponse> chatStream(String content, ChatStreamListener consumer) {
        return chatStream(Collections.singletonList(Message.ofUser(content)), null, consumer);
    }

    /**
     * 发送流式聊天请求（带单个工具函数）
     *
     * <p>使用单条用户消息和一个工具函数发起流式请求。</p>
     *
     * @param content  用户输入内容
     * @param tool     工具函数定义
     * @param consumer 流式响应回调
     * @return 终态 future：流正常结束时以完整响应完成，异常时异常完成
     * @see #chatStream(String, List, ChatStreamListener)
     */
    public CompletableFuture<ChatResponse> chatStream(String content, Tool tool, ChatStreamListener consumer) {
        return chatStream(Collections.singletonList(Message.ofUser(content)), Collections.singletonList(tool), consumer);
    }

    /**
     * 发送流式聊天请求（带多个工具函数）
     *
     * <p>使用单条用户消息和多个工具函数发起流式请求。</p>
     *
     * @param content  用户输入内容
     * @param tools    工具函数列表
     * @param consumer 流式响应回调
     * @return 终态 future：流正常结束时以完整响应完成，异常时异常完成
     * @see #chatStream(List, List, ChatStreamListener)
     */
    public CompletableFuture<ChatResponse> chatStream(String content, List<Tool> tools, ChatStreamListener consumer) {
        return chatStream(Collections.singletonList(Message.ofUser(content)), tools, consumer);
    }

    /**
     * 发送流式聊天请求（完整版本）
     *
     * <p>支持完整的消息列表和工具函数配置，通过回调函数实时获取AI响应。</p>
     *
     * <p>返回的 future 与回调各司其职：回调（onStreamResponse/onReasoning）
     * 负责增量输出，future 负责终态。流式收到终止标记（如 [DONE]）时以 provider 组装的完整 ChatResponse 完成；
     * 流式已启动但服务端未发终止标记即断连时，以已累积的内容补齐完成并触发 onCompletion；
     * 网络异常、HTTP 成功但流式未启动（如 401、429）时 future 异常完成并触发 onError。</p>
     *
     * @param messages 消息列表，包含对话历史
     * @param tools    工具函数列表，可为 null
     * @param consumer 流式响应回调，AI每生成一个响应块时调用
     * @return 终态 future：流正常结束时以完整 ChatResponse 完成，传输层失败时异常完成
     */
    public CompletableFuture<ChatResponse> chatStream(List<Message> messages, List<Tool> tools, ChatStreamListener consumer) {
        Provider provider = options.getProvider().apply(options);
        HttpRest rest = provider.createRequest(messages, true, tools);
        StreamContext context = new StreamContext();
        return rest.onSSE(sse -> sse.onData(event -> {
                    // 首次收到数据，标记为 UPGRADE 状态
                    if (context.getStatus() == StreamContext.STREAM_STATUS_INIT) {
                        context.setStatus(StreamContext.STREAM_STATUS_UPGRADE);
                    }
                    provider.parseStreamResponse(context, event, consumer);
                }))
                // HTTP 成功但流式未启动：说明请求失败（如 401、429）
                .onSuccess(response -> {
                    if (context.getStatus() == StreamContext.STREAM_STATUS_INIT) {
                        consumer.onError(new FeatException(response.body()));
                    }
                })
                // 网络异常或连接失败
                .onFailure(throwable -> consumer.onError(throwable))
                // 提交请求，在底层 HTTP future 的基础上转换出流式终态
                .submit()
                .thenApply(response -> {
                    // provider 已在终止标记（如 [DONE]）处组装出完整响应
                    ChatResponse result = context.getResponse();
                    if (result != null) {
                        return result;
                    }
                    // 流式已启动但未收到终止标记即断连：以已累积的内容收尾
                    if (context.getStatus() == StreamContext.STREAM_STATUS_UPGRADE) {
                        ChatResponse partial = partialResponse(context);
                        consumer.onCompletion(partial);
                        return partial;
                    }
                    // 流式未收到任何数据：HTTP 失败（如 401、429）时 onError 由 onSuccess 触发。
                    // 注意 thenApply 先于 onSuccess 执行（CompletableFuture 依赖按注册倒序触发），
                    // 此处不能依赖 context 状态，需与 SSE 升级谓词保持一致的反向判断：
                    // 仅当响应是 200 的 event-stream（onSuccess 被升级抑制）时才补发 onError
                    String contentType = response.getContentType();
                    if (response.statusCode() == 200 && contentType != null && contentType.startsWith("text/event-stream")) {
                        FeatException e = new FeatException("stream closed without any data");
                        consumer.onError(e);
                        throw e;
                    }
                    throw new FeatException(response.body());
                });
    }

    /**
     * 以流式过程中已累积的内容构建部分响应（用于服务端未发终止标记即断连的收尾）
     */
    private static ChatResponse partialResponse(StreamContext context) {
        ChatResponse partial = new ChatResponse();
        partial.setRole(Message.ROLE_ASSISTANT);
        partial.setContent(context.getContent());
        partial.setReasoningContent(context.getReasoning());
        partial.setSuccess(true);
        return partial;
    }

    /**
     * 发送非流式聊天请求（简单版本）
     *
     * <p>使用单条用户消息发起请求，等待完整响应返回。</p>
     *
     * @param content 用户输入内容
     * @return 包含响应消息的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(String content) {
        return chat(Collections.singletonList(Message.ofUser(content)), null);
    }

    public CompletableFuture<ChatResponse> chat(Message message) {
        return chat(Collections.singletonList(message), null);
    }

    /**
     * 发送非流式聊天请求（带工具函数）
     *
     * <p>使用单条用户消息和工具函数列表发起请求。
     * 工具函数会在请求时动态注入，无需在选项中预先配置。</p>
     *
     * @param content 用户输入内容
     * @param tools   工具函数列表
     * @return 包含响应消息的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(String content, List<Tool> tools) {
        return chat(Collections.singletonList(Message.ofUser(content)), tools);
    }

    /**
     * 发送非流式聊天请求（带单个工具函数）
     *
     * @param content 用户输入内容
     * @param tool    工具函数定义
     * @return 包含响应消息的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(String content, Tool tool) {
        return chat(Collections.singletonList(Message.ofUser(content)), Collections.singletonList(tool));
    }

    /**
     * 发送非流式聊天请求（完整版本）
     *
     * <p>支持完整的消息列表和工具函数配置，返回完整的AI响应。</p>
     *
     * @param messages 消息列表，包含对话历史
     * @param tools    工具函数列表，可为 null
     * @return 包含响应消息的 CompletableFuture
     */
    public CompletableFuture<ChatResponse> chat(List<Message> messages, List<Tool> tools) {
        Provider provider = options.getProvider().apply(options);
        HttpRest rest = provider.createRequest(messages, false, tools);
        return rest.submit().thenApply(response -> {
            // 检查 HTTP 状态码
            if (response.statusCode() != 200) {
                return Provider.error(response.body());
            }
            return provider.parseResponse(response);
        });
    }

}
