/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.client;

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/7
 */
public class Benchmark {
    public static void main(String[] args) throws InterruptedException, IOException {

        int time = 15 * 1000;
//        int time = Integer.MAX_VALUE;
        int threadNum = 4;
        int connectCount = 1024;
        int pipeline = 1;
        AtomicLong success = new AtomicLong(0);
        AtomicLong fail = new AtomicLong(0);
        AtomicBoolean running = new AtomicBoolean(true);

        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadNum, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        List<HttpClient> httpClients = new ArrayList<>();
        for (int i = 0; i < connectCount; i++) {
            HttpClient httpClient = new HttpClient("127.0.0.1", 8080);
            httpClient.options().group(asynchronousChannelGroup);
            httpClients.add(httpClient);
        }
        System.out.println(httpClients.size() + " clients connect success");
        long startTime = System.currentTimeMillis();
        for (HttpClient httpClient : httpClients) {
            Consumer<Throwable> failure = new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    fail.incrementAndGet();
                    throwable.printStackTrace();
                }
            };
            Consumer<HttpResponse> consumer = new Consumer<HttpResponse>() {
                @Override
                public void accept(HttpResponse response) {
                    success.incrementAndGet();
//                    System.out.println(response.body());
                    if (running.get()) {
                        httpClient.get("/plaintext").onSuccess(this).onFailure(failure).submit();
                    } else {
                        httpClient.close();
                    }
                }
            };
            for (int j = 0; j < pipeline; j++) {
                httpClient.get("/plaintext")
                        .onSuccess(consumer)
                        .onFailure(failure)
                        .submit();
            }
        }
        System.out.println("all client started,cost:" + (System.currentTimeMillis() - startTime));

        Thread.sleep(time);
        running.set(false);

        System.out.println("cost:" + (System.currentTimeMillis() - startTime));

        System.out.println("success:" + success.get());
        System.out.println("fail:" + fail.get());
        asynchronousChannelGroup.shutdown();
    }

}
