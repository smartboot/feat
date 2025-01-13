/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: AsyncHttpDemo.java
 * Date: 2021-10-28
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.HttpServerHandler;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/10/28
 */
public class AsyncHttpDemo {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        HttpServer bootstrap = new HttpServer();
        bootstrap.httpHandler(new HttpServerHandler() {

            @Override
            public void handle(HttpRequest request, CompletableFuture<Object> future) throws IOException {
//                response.write((new Date() + " currentThread:" + Thread.currentThread()).getBytes());
//                response.getOutputStream().flush();
                executorService.execute(() -> {
                    try {
                        //sleep 3秒模拟阻塞
                        Thread.sleep(1000);
                        request.getResponse().write(("<br/>" + new Date() + " currentThread:" + Thread.currentThread()).getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    future.complete(this);
                });

            }
        });
        bootstrap.options().debug(true);
        bootstrap.listen(8080);
    }
}
