/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: Benchmark.java
 * Date: 2021-03-05
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo.client;

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import org.smartboot.socket.buffer.BufferPagePool;

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
            httpClient.group(asynchronousChannelGroup);
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
                        httpClient.get("/plaintext").onSuccess(this).onFailure(failure).done();
                    } else {
                        httpClient.close();
                    }
                }
            };
            for (int j = 0; j < pipeline; j++) {
                httpClient.get("/plaintext")
                        .onSuccess(consumer)
                        .onFailure(failure)
                        .done();
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
