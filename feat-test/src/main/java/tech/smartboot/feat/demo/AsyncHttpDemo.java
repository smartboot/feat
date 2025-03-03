/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;

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
        bootstrap.httpHandler(new HttpHandler() {

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

            @Override
            public void handle(HttpRequest request) throws Throwable {

            }
        });
        bootstrap.options().debug(true);
        bootstrap.listen(8080);
    }
}
