/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpRouteDemo.java
 * Date: 2021-06-20
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.HttpServerHandler;
import tech.smartboot.feat.core.server.handler.Router;
import tech.smartboot.feat.core.server.upgrade.http2.Http2UpgradeHandler;
import tech.smartboot.feat.core.server.upgrade.websocket.WebSocketUpgradeHandler;

import java.io.IOException;

/**
 * 请求路由示例
 *
 * @author 三刀
 * @version V1.0 , 2020/4/1
 */
public class HttpRouteDemo {
    public static void main(String[] args) {
        //1. 实例化路由Handle
        Router routeHandle = new Router();

        //2. 指定路由规则以及请求的处理实现
        routeHandle.route("/", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        response.write("feat".getBytes());
                    }
                })
                .route("/h2", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        request.upgrade(new Http2UpgradeHandler() {
                            @Override
                            public void handle(HttpRequest request, HttpResponse response) throws Throwable {
                                response.write("feat h2".getBytes());
                            }
                        });
                    }
                })
                .route("/test1", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        response.write(("test1").getBytes());
                    }
                })
                .route("/test2", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        response.write(("test2").getBytes());
                    }
                })
                .route("/ws", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        request.upgrade(new WebSocketUpgradeHandler() {

                        });
                    }
                });

        // 3. 启动服务
        HttpServer bootstrap = new HttpServer();
        bootstrap.options().setWsIdleTimeout(5000).debug(true);
        bootstrap.httpHandler(routeHandle);
        bootstrap.listen();
    }
}
