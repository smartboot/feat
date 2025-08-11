/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.demo;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.ai.chat.entity.Function;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.entity.ToolCall;

import java.util.Arrays;
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
        ChatModel chatModel = FeatAI.chatModel(opts -> opts.model(ChatModelVendor.GiteeAI.Qwen3_235B_A22B_Instruct_2507)
                .addFunction(Function.of("get_weather").description("获取天气信息").addParam("city", "城市名称", "string", true))
                .noThink(true).debug(true));

        chatModel.chatStream("今天杭州天气如何", Arrays.asList("get_weather"), new StreamResponseCallback() {
            @Override
            public void onStreamResponse(String content) {
                System.out.printf(content);
            }

            @Override
            public void onCompletion(ResponseMessage responseMessage) {
                countDownLatch.complete(responseMessage.getToolCalls());
            }
        });
        List<ToolCall> tools = countDownLatch.get();
        Assert.assertEquals(1, tools.size());
        Assert.assertEquals("get_weather", tools.get(0).getFunction().get("name"));
    }

    @Test
    public void test2() throws InterruptedException, ExecutionException {
        CompletableFuture<List<ToolCall>> future = new CompletableFuture<>();
        ChatModel chatModel = FeatAI.chatModel(opts -> opts.model(ChatModelVendor.GiteeAI.Kimi_K2_Instruct)
                .addFunction(
                        Function.of("get_weather")
                                .description("获取天气信息")
                                .addParam("city", "城市名称", "string", true))
                .noThink(false).debug(false));

        chatModel.chatStream("今天杭州天气如何", Arrays.asList("get_weather"), new StreamResponseCallback() {
            @Override
            public void onStreamResponse(String content) {
                System.out.printf(content);
            }

            @Override
            public void onCompletion(ResponseMessage responseMessage) {
                future.complete(responseMessage.getToolCalls());
            }
        });
        List<ToolCall> tools = future.get();
        Assert.assertEquals(1, tools.size());
        Assert.assertEquals("get_weather", tools.get(0).getFunction().get("name"));
    }

    @Test
    public void test3() throws InterruptedException, ExecutionException {
        CompletableFuture<List<ToolCall>> future = new CompletableFuture<>();
        ChatModel chatModel = FeatAI.chatModel(opts -> opts.model(ChatModelVendor.GiteeAI.DeepSeek_R1)
                .noThink(true).debug(true));

//        chatModel.chatStream("你是谁", new StreamResponseCallback() {
//            @Override
//            public void onStreamResponse(String content) {
//                System.out.printf(content);
//            }
//
//            @Override
//            public void onCompletion(ResponseMessage responseMessage) {
//                future.complete(responseMessage.getToolCalls());
//            }
//        });
        chatModel.chat("你是谁",rsp->{
            System.out.println(rsp.getContent());
            future.complete(rsp.getToolCalls());
        });
        List<ToolCall> tools = future.get();
        Assert.assertEquals(1, tools.size());
        Assert.assertEquals("get_weather", tools.get(0).getFunction().get("name"));
    }
}
