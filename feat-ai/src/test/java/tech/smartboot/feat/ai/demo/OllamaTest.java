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
import tech.smartboot.feat.ai.chat.entity.ToolCall;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 8/9/25
 */
public class OllamaTest {
    @Test
    public void test1() throws InterruptedException, ExecutionException {
        CompletableFuture<String> countDownLatch = new CompletableFuture<>();
        ChatModel chatModel = FeatAI.chatModel(opts -> opts.model(ChatModelVendor.Ollama.Deepseek_r1_7B)
                .addFunction(Function.of("get_weather").description("获取天气信息").addParam("city", "城市名称", "string", true))
                .noThink(true).debug(true));

        chatModel.chat("写一首诗，提供思考过程", Arrays.asList("get_weather"), new Consumer<ResponseMessage>() {
            @Override
            public void accept(ResponseMessage responseMessage) {
                System.out.println(responseMessage.getContent());
                countDownLatch.complete(responseMessage.getContent());
            }
        });
        String content = countDownLatch.get();
//        Assert.assertEquals(1, tools.size());
//        Assert.assertEquals("get_weather", tools.get(0).getFunction().get("name"));
    }

}
