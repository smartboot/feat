/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpRestTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test.client;

import com.alibaba.fastjson.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.router.Router;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/7
 */
public class HttpRestTest {

    private HttpServer httpServer;


    @Before
    public void init() {
        httpServer = new HttpServer();
        Router routeHandler = new Router();
        routeHandler.route("/post", new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws IOException {
                HttpResponse response=request.getResponse();
                response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValue.Connection.keepalive);
                JSONObject jsonObject = new JSONObject();
                for (String key : request.getParameters().keySet()) {
                    jsonObject.put(key, request.getParameter(key));
                }
                response.write(jsonObject.toString().getBytes());
            }
        });
        httpServer.httpHandler(routeHandler).listen(8080);
    }

    @Test
    public void testPost() throws ExecutionException, InterruptedException {
        HttpClient httpClient = new HttpClient("localhost", 8080);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = httpClient.rest("/post")
                .setMethod("post")
                .onSuccess(response -> {
                    System.out.println(response.body());
                    httpClient.close();
                })
                .onFailure(throwable -> {
                    throwable.printStackTrace();
                    httpClient.close();
                })
                .submit();
        System.out.println(future.get().body());
    }

    @Test
    public void testKeepalive() throws InterruptedException {
        HttpClient httpClient = new HttpClient("localhost", 8080);
        httpClient.options().debug(true);
        Map<String, String> form = new HashMap<>();
        int count = 100;
        CountDownLatch countDownLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            form.put("name" + i, "value" + i);
            final int j = i + 1;
            httpClient.post("/post").header(h->h.keepalive(true))
                    .body().formUrlencoded(form)
                    .onSuccess(httpResponse -> {
                        countDownLatch.countDown();
                        JSONObject jsonObject = JSONObject.parseObject(httpResponse.body());
                        Assert.assertEquals(jsonObject.size(), j);
                        System.out.println(httpResponse.body());
                    })
                    .onFailure(throwable -> {
                        countDownLatch.countDown();
                        throwable.printStackTrace();
                    }).submit();
        }
        countDownLatch.await();
        System.out.println("finish...");
    }

    @After
    public void destroy() {
        httpServer.shutdown();
    }

}
