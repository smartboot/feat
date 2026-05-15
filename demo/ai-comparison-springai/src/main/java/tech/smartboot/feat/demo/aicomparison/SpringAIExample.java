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

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

/**
 * Spring AI 使用示例
 * <p>
 * Spring AI 的特点：
 * 1. 与 Spring 生态深度集成
 * 2. 支持函数调用、RAG、Prompt 模板等
 * 3. 基于 Spring 的编程模型
 * 4. 支持响应式编程（Reactor）
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SpringAIExample {

    public static void main(String[] args) {
        System.out.println("=== Spring AI 示例 ===\n");

        // 1. 基础对话示例
        basicChat();

        // 2. 流式输出示例
        streamingChat();
    }

    /**
     * 基础对话 - Spring 风格的 API
     */
    public static void basicChat() {
        System.out.println("--- 基础对话 ---");

        // 创建 OpenAiApi
        OpenAiApi openAiApi = new OpenAiApi("https://ai.gitee.com/", System.getenv("FEAT_AI_API_KEY"));

        // 创建 ChatOptions
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("Qwen3-235B-A22B")
                .build();

        // 创建 ChatModel
        ChatModel chatModel = new OpenAiChatModel(openAiApi, options);

        // 使用 ChatModel 进行对话
        Prompt prompt = new Prompt("你好，请介绍一下 Spring AI 的特点。");
        ChatResponse response = chatModel.call(prompt);

        Generation generation = response.getResult();
        if (generation != null && generation.getOutput() != null) {
            System.out.println("Spring AI 回复: " + generation.getOutput().getContent());
        }
        System.out.println();
    }

    /**
     * 流式输出 - Flux 响应式流
     */
    public static void streamingChat() {
        System.out.println("--- 流式输出 ---");

        OpenAiApi openAiApi = new OpenAiApi("https://ai.gitee.com/", System.getenv("FEAT_AI_API_KEY"));

        // 创建 ChatOptions
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("Qwen3-235B-A22B")
                .build();

        // 创建 StreamingChatModel
        StreamingChatModel streamingChatModel = new OpenAiChatModel(openAiApi, options);

        // 创建 Prompt
        Prompt prompt = new Prompt("请用5句话描述 Java 异步编程的优势。");

        // 流式输出，返回 Flux<ChatResponse>
        Flux<ChatResponse> stream = streamingChatModel.stream(prompt);

        CountDownLatch latch = new CountDownLatch(1);
        // 订阅流并处理每个响应
        stream.subscribe(
                response -> {
                    // 输出每个 token
                    Generation generation = response.getResult();
                    if (generation != null && generation.getOutput() != null) {
                        String content = generation.getOutput().getContent();
                        if (content != null) {
                            System.out.print(content);
                        }
                    }
                },
                error -> {
                    System.err.println("错误: " + error.getMessage());
                    latch.countDown();
                },
                () -> {
                    System.out.println("\n[流式输出完成]\n");
                    latch.countDown();
                }
        );

        // 等待流完成
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
