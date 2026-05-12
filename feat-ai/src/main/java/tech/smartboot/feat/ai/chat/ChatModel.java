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

import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ChatResponse;
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
 * CompletableFuture<ResponseMessage> future = chatModel.chat("Hello");
 * future.thenAccept(response -> System.out.println(response.getContent()));
 *
 * // 流式调用
 * chatModel.chatStream("Hello", response -> System.out.println(response.getContent()));
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
     * @see #chatStream(String, List, ChatStreamListener)
     */
    public void chatStream(String content, ChatStreamListener consumer) {
        chatStream(Collections.singletonList(Message.ofUser(content)), null, consumer);
    }

    /**
     * 发送流式聊天请求（带单个工具函数）
     *
     * <p>使用单条用户消息和一个工具函数发起流式请求。</p>
     *
     * @param content  用户输入内容
     * @param tool     工具函数定义
     * @param consumer 流式响应回调
     * @see #chatStream(String, List, ChatStreamListener)
     */
    public void chatStream(String content, Tool tool, ChatStreamListener consumer) {
        chatStream(Collections.singletonList(Message.ofUser(content)), Collections.singletonList(tool), consumer);
    }

    /**
     * 发送流式聊天请求（带多个工具函数）
     *
     * <p>使用单条用户消息和多个工具函数发起流式请求。</p>
     *
     * @param content  用户输入内容
     * @param tools    工具函数列表
     * @param consumer 流式响应回调
     * @see #chatStream(List, List, ChatStreamListener)
     */
    public void chatStream(String content, List<Tool> tools, ChatStreamListener consumer) {
        chatStream(Collections.singletonList(Message.ofUser(content)), tools, consumer);
    }

    /**
     * 发送流式聊天请求（完整版本）
     *
     * <p>支持完整的消息列表和工具函数配置，通过回调函数实时获取AI响应。</p>
     *
     * @param messages 消息列表，包含对话历史
     * @param tools    工具函数列表，可为 null
     * @param consumer 流式响应回调，AI每生成一个响应块时调用
     */
    public void chatStream(List<Message> messages, List<Tool> tools, ChatStreamListener consumer) {
        Provider provider = options.getProvider().apply(options);
        HttpRest rest = provider.createRequest(messages, true, tools);
        StreamContext context = new StreamContext();
        rest.onSSE(sse -> sse.onData(event -> {
                    // 首次收到数据，标记为 UPGRADE 状态
                    if (context.getStatus() == StreamContext.STREAM_STATUS_INIT) {
                        context.setStatus(StreamContext.STREAM_STATUS_UPGRADE);
                    }
                    provider.parseStreamResponse(context, event, consumer);
                }))
                // HTTP 成功但流式未启动：说明请求失败（如 401、429）
                .onSuccess(response -> {
                    if (context.getStatus() == StreamContext.STREAM_STATUS_INIT) {
                        context.setStatus(StreamContext.STREAM_STATUS_ERROR);
                        consumer.onError(new FeatException(response.body()));
                    }
                })
                // 网络异常或连接失败
                .onFailure(throwable -> {
                    context.setStatus(StreamContext.STREAM_STATUS_ERROR);
                    consumer.onError(throwable);
                })
                // 提交请求
                .submit();
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
