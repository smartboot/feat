/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.demo;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.ChatResponse;
import tech.smartboot.feat.ai.chat.ChatStreamListener;
import tech.smartboot.feat.ai.chat.entity.ToolCall;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version v1.0 8/9/25
 */
public class GiteeTest {
    @Test
    public void test1() throws InterruptedException, ExecutionException {
        CompletableFuture<List<ToolCall>> countDownLatch = new CompletableFuture<>();
        ChatModel chatModel = FeatAI.chatModel(opts -> opts.model("Qwen3-235B-A22B-Instruct-2507")
                .debug(true));

        chatModel.chatStream("今天杭州天气如何", Collections.singletonList(Tool.of("get_weather").description("获取天气信息").addParam("city", "城市名称", "string", true)), new ChatStreamListener() {
            @Override
            public void onStreamResponse(String content) {
                System.out.printf(content);
            }

            @Override
            public void onCompletion(ChatResponse chatResponse) {
                countDownLatch.complete(chatResponse.getToolCalls());
            }
        });
        List<ToolCall> tools = countDownLatch.get();
        Assert.assertEquals(1, tools.size());
        Assert.assertEquals("get_weather", tools.get(0).getName());
    }

    @Test
    public void test2() throws InterruptedException, ExecutionException {
        CompletableFuture<List<ToolCall>> future = new CompletableFuture<>();
        ChatModel chatModel = FeatAI.chatModel(opts -> opts.model("DeepSeek-V4-Flash")
                .debug(true));

        chatModel.chatStream("杭州天气如何",
                Tool.of("get_weather")
                        .description("获取天气信息")
                        .addParam("city", "城市名称", "string", true),
                new ChatStreamListener() {
                    @Override
                    public void onReasoning(String content) {
                        System.err.print(content);
                    }

                    @Override
                    public void onStreamResponse(String content) {
                        System.out.printf(content);
                    }

                    @Override
                    public void onCompletion(ChatResponse chatResponse) {
                        future.complete(chatResponse.getToolCalls());
                    }
                });
        List<ToolCall> tools = future.get();
        Assert.assertEquals(1, tools.size());
        Assert.assertEquals("get_weather", tools.get(0).getName());
    }

    @Test
    public void test3() throws InterruptedException, ExecutionException {
        ChatModel chatModel = FeatAI.chatModel(opts -> opts.model("DeepSeek-R1")
                .debug(true));

        chatModel.chat("你是谁").thenAccept(rsp -> {
            System.out.println(rsp.getReasoningContent());
            System.out.println(rsp.getContent());
        }).get();
    }
}
