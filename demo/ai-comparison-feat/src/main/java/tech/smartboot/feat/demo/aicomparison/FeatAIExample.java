/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.aicomparison;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.ChatStreamListener;
import tech.smartboot.feat.ai.chat.ThinkOption;
import tech.smartboot.feat.ai.chat.entity.ChatResponse;

import java.util.concurrent.CountDownLatch;

/**
 * Feat AI 使用示例
 * <p>
 * Feat AI 的特点：
 * 1. 简洁的链式 API 设计
 * 2. 原生支持异步编程模型
 * 3. 轻量级，无 Spring 依赖
 * 4. 内置多种模型提供商支持
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class FeatAIExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Feat AI 示例 ===\n");

        // 1. 基础对话示例
        basicChat();

        // 2. 流式输出示例
        streamingChat();

        // 3. 多轮对话示例
        multiTurnChat();
    }

    /**
     * 基础对话 - 简洁的链式配置
     */
    public static void basicChat() {
        System.out.println("--- 基础对话 ---");

        // 使用 FeatAI.chatModel 快速创建 ChatModel
        // 通过 lambda 配置选项，简洁直观
        ChatModel chatModel = FeatAI.chatModel(opts ->
                opts.model("Qwen3-235B-A22B")
                        .extraBody(ThinkOption.Qwen.DISABLE)
                        .system("你是一个乐于助人的助手。")
        );

        // 异步调用，返回 CompletableFuture
        chatModel.chat("你好，请介绍一下 Feat AI 的特点。")
                .thenAccept(response -> {
                    System.out.println("Feat AI 回复: " + response.getContent());
                    System.out.println("Token 使用: " + response.getUsage());
                    System.out.println();
                })
                .join(); // 等待完成
    }

    /**
     * 流式输出 - 实时响应处理
     */
    public static void streamingChat() throws InterruptedException {
        System.out.println("--- 流式输出 ---");

        ChatModel chatModel = FeatAI.chatModel(opts ->
                opts.model("Qwen3-235B-A22B").extraBody(ThinkOption.Qwen.DISABLE)
        );

        CountDownLatch latch = new CountDownLatch(1);
        // 流式输出，通过 ChatStreamListener 处理实时响应
        chatModel.chatStream(
                "请用5句话描述 Java 异步编程的优势。",
                new ChatStreamListener() {
                    @Override
                    public void onStreamResponse(String content) {
                        // 实时输出每个 token
                        System.out.print(content);
                    }

                    @Override
                    public void onCompletion(ChatResponse chatResponse) {
                        System.out.println("\n[流式输出完成]\n");
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        latch.countDown();
                    }
                }
        );
        latch.await();
    }

    /**
     * 多轮对话 - 保持上下文
     */
    public static void multiTurnChat() {
        System.out.println("--- 多轮对话 ---");

        ChatModel chatModel = FeatAI.chatModel(opts ->
                opts.model("Qwen3-235B-A22B")
                        .extraBody(ThinkOption.Qwen.DISABLE)
                        .system("你是一个专业的 Java 技术专家。")
        );

        // 第一轮
        chatModel.chat("什么是虚拟线程（Virtual Threads）？")
                .thenAccept(response -> {
                    System.out.println("Q: 什么是虚拟线程？");
                    System.out.println("A: " + response.getContent().substring(0, 100) + "...\n");

                    // 第二轮（自动保持上下文）
                    chatModel.chat("它相比传统线程有什么优势？")
                            .thenAccept(response2 -> {
                                System.out.println("Q: 它相比传统线程有什么优势？");
                                System.out.println("A: " + response2.getContent().substring(0, 100) + "...\n");
                            })
                            .join();
                })
                .join();
    }
}
