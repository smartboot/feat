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
import tech.smartboot.feat.core.server.HttpServerHandler;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.handler.HttpRouteHandler;
import tech.smartboot.feat.core.server.upgrade.WebSocketUpgradeHandler;

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
        HttpRouteHandler routeHandle = new HttpRouteHandler();

        //2. 指定路由规则以及请求的处理实现
        routeHandle.route("/", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        response.write("feat".getBytes());
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
                        request.upgrade(new WebSocketUpgradeHandler(){

                        });
                    }
                });

        // 3. 启动服务
        HttpServer bootstrap = new HttpServer();
        bootstrap.httpHandler(routeHandle);
        bootstrap.start();
    }
}
