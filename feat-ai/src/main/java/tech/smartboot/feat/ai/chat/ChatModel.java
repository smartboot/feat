/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat;

import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天模型类，用于与AI模型进行交互，支持流式和非流式响应
 * 支持 OpenAI 和 Anthropic 两种API规范
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatModel {
    private final ChatOptions options;


    /**
     * 构造函数
     *
     * @param options 聊天选项配置
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
     * @param content  用户输入内容
     * @param consumer 流式响应回调
     */
    public void chatStream(String content, StreamResponseCallback consumer) {
        chatStream(Collections.singletonList(Message.ofUser(content)), consumer);
    }

    /**
     * 发送流式聊天请求（完整版本）
     *
     * @param messages 消息列表
     * @param consumer 流式响应回调
     */
    public void chatStream(List<Message> messages, StreamResponseCallback consumer) {
        options.getProvider().apply(options).chatStream(messages, consumer);
    }

    /**
     * 发送非流式聊天请求（消息列表版本）
     *
     * @param messages 消息列表
     * @return 包含响应消息的CompletableFuture
     */
    public CompletableFuture<ResponseMessage> chat(List<Message> messages) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(messages, future::complete);
        return future;
    }

    /**
     * 发送非流式聊天请求（工具版本）
     *
     * @param content 用户输入内容
     * @return 包含响应消息的CompletableFuture
     */
    public CompletableFuture<ResponseMessage> chat(String content) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(Collections.singletonList(Message.ofUser(content)), new CompletionHandler() {

            @Override
            public void completed(ResponseMessage message) {
                future.complete(message);
            }

            @Override
            public void failed(Throwable exc) {
                future.completeExceptionally(exc);
            }
        });
        return future;
    }

    /**
     * 发送非流式聊天请求（回调版本）
     *
     * @param messages 消息列表
     * @param callback 响应回调
     */
    public void chat(List<Message> messages, CompletionHandler callback) {
        options.getProvider().apply(options).chat(messages, callback);
    }

    /**
     * 发送非流式聊天请求（无工具版本）
     *
     * @param content  用户输入内容
     * @param callback 响应回调
     */
    public void chat(String content, CompletionHandler callback) {
        chat(Collections.singletonList(Message.ofUser(content)), callback);
    }

    /**
     * 获取聊天选项配置
     *
     * @return 聊天选项配置
     */
    public ChatOptions getOptions() {
        return options;
    }
}