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
import tech.smartboot.feat.client.HttpClient;
import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HeaderValueEnum;
import tech.smartboot.feat.server.HttpBootstrap;
import tech.smartboot.feat.server.HttpRequest;
import tech.smartboot.feat.server.HttpResponse;
import tech.smartboot.feat.server.HttpServerHandler;
import tech.smartboot.feat.server.handler.HttpRouteHandler;

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

    private HttpBootstrap httpBootstrap;


    @Before
    public void init() {
        httpBootstrap = new HttpBootstrap();
        HttpRouteHandler routeHandler = new HttpRouteHandler();
        routeHandler.route("/post", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValueEnum.keepalive.getName());
                JSONObject jsonObject = new JSONObject();
                for (String key : request.getParameters().keySet()) {
                    jsonObject.put(key, request.getParameter(key));
                }
                response.write(jsonObject.toString().getBytes());
            }
        });
        httpBootstrap.httpHandler(routeHandler).setPort(8080).start();
    }

    @Test
    public void testPost() throws ExecutionException, InterruptedException {
        HttpClient httpClient = new HttpClient("localhost", 8080);
        Future<tech.smartboot.feat.client.HttpResponse> future = httpClient.rest("/post")
                .setMethod("post")
                .onSuccess(response -> {
                    System.out.println(response.body());
                    httpClient.close();
                })
                .onFailure(throwable -> {
                    throwable.printStackTrace();
                    httpClient.close();
                })
                .done();
        System.out.println(future.get().body());
    }

    @Test
    public void testKeepalive() throws InterruptedException {
        HttpClient httpClient = new HttpClient("localhost", 8080);
        httpClient.configuration().debug(true);
        Map<String, String> form = new HashMap<>();
        int count = 100;
        CountDownLatch countDownLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            form.put("name" + i, "value" + i);
            final int j = i + 1;
            httpClient.post("/post").header().keepalive(true).done()
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
                    }).done();
        }
        countDownLatch.await();
        System.out.println("finish...");
    }

    @After
    public void destroy() {
        httpBootstrap.shutdown();
    }

}
