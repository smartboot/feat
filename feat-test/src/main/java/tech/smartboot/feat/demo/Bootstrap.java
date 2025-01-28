/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpBootstrap.java
 * Date: 2018-01-28
 * Author: sandao
 */

package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.router.Router;

import java.io.IOException;

public class Bootstrap {
    static byte[] body = "Hello, World!".getBytes();

    public static void main(String[] args) {
        Router routeHandle = new Router();
        routeHandle.route("/plaintext", new BaseHttpHandler() {


            @Override
            public void handle(HttpRequest request) throws IOException {
                HttpResponse response=request.getResponse();
                response.setContentLength(body.length);
                response.setContentType("text/plain; charset=UTF-8");
                response.write(body);
            }
        });
        int cpuNum = Runtime.getRuntime().availableProcessors();

        // 定义服务器接受的消息类型以及各类消息对应的处理器
        HttpServer bootstrap = Feat.httpServer(opt -> opt.threadNum(cpuNum).debug(false).headerLimiter(0).readBufferSize(1024 * 4).writeBufferSize(1024 * 4));
        bootstrap.httpHandler(routeHandle).listen(8080);
    }
}
