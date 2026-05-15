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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * LangChain4j 使用示例
 * <p>
 * LangChain4j 的特点：
 * 1. 功能丰富，生态系统完善
 * 2. 支持多种模型和工具集成
 * 3. 提供内存、RAG、Agent 等高级功能
 * 4. 基于 Builder 模式的 API 设计
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class LangChain4jExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== LangChain4j 示例 ===\n");

        // 1. 基础对话示例
        basicChat();

        // 2. 流式输出示例
        streamingChat();

        // 3. 多轮对话示例
        multiTurnChat();
    }

    /**
     * 基础对话 - Builder 模式配置
     */
    public static void basicChat() {
        System.out.println("--- 基础对话 ---");

        // 使用 Builder 模式创建 ChatLanguageModel
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("FEAT_AI_API_KEY"))
            .modelName("Qwen3-235B-A22B")
                .baseUrl("https://ai.gitee.com/v1/")
            .build();

        // 同步调用，直接返回结果
        String response = model.generate("你好，请介绍一下 LangChain4j 的特点。");
        System.out.println("LangChain4j 回复: " + response);
        System.out.println();
    }

    /**
     * 流式输出 - StreamingResponseHandler
     */
    public static void streamingChat() throws InterruptedException {
        System.out.println("--- 流式输出 ---");

        StreamingChatLanguageModel streamingModel = OpenAiStreamingChatModel.builder()
            .apiKey(System.getenv("FEAT_AI_API_KEY"))
            .modelName("Qwen3-235B-A22B")
                .baseUrl("https://ai.gitee.com/v1/")
            .build();

        CountDownLatch latch = new CountDownLatch(1);
        // 流式输出，通过 StreamingResponseHandler 处理
        streamingModel.generate(
            "请用5句话描述 Java 异步编程的优势。",
            new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    // 实时输出每个 token
                    System.out.print(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    System.out.println("\n[流式输出完成]\n");
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    System.err.println("错误: " + error.getMessage());
                    latch.countDown();
                }
            }
        );
        latch.await();
    }

    /**
     * 多轮对话 - 手动管理消息列表
     */
    public static void multiTurnChat() {
        System.out.println("--- 多轮对话 ---");

        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("FEAT_AI_API_KEY"))
            .modelName("Qwen3-235B-A22B")
                .baseUrl("https://ai.gitee.com/v1/")
            .build();

        // 手动管理对话历史
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();

        // 添加系统消息
        messages.add(dev.langchain4j.data.message.SystemMessage.from("你是一个专业的 Java 技术专家。"));

        // 第一轮
        UserMessage userMessage1 = UserMessage.from("什么是虚拟线程（Virtual Threads）？");
        messages.add(userMessage1);

        Response<AiMessage> response1 = model.generate(messages);
        System.out.println("Q: 什么是虚拟线程？");
        System.out.println("A: " + response1.content().text().substring(0, 100) + "...\n");

        // 将 AI 回复添加到历史
        messages.add(response1.content());

        // 第二轮
        UserMessage userMessage2 = UserMessage.from("它相比传统线程有什么优势？");
        messages.add(userMessage2);

        Response<AiMessage> response2 = model.generate(messages);
        System.out.println("Q: 它相比传统线程有什么优势？");
        System.out.println("A: " + response2.content().text().substring(0, 100) + "...\n");
    }
}
