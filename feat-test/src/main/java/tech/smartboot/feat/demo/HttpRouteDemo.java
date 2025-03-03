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

import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class HttpRouteDemo {
    public static void main(String[] args) {
        //1. 实例化路由Handle
        Router routeHandle = new Router();

        //2. 指定路由规则以及请求的处理实现
        routeHandle.route("/", ctx -> ctx.Response.write("feat".getBytes()))
//                .route("/h2", new BaseHttpHandler() {
//                    @Override
//                    public void handle(HttpRequest request) throws IOException {
//                        request.upgrade(new Http2UpgradeHandler() {
//                            @Override
//                            public void handle(HttpRequest request, HttpResponse response) throws Throwable {
//                                response.write("feat h2".getBytes());
//                            }
//                        });
//                    }
//                })
//                .route("/test1", new BaseHttpHandler() {
//                    @Override
//                    public void handle(HttpRequest request) throws IOException {
//                        request.getResponse().write(("test1").getBytes());
//                    }
//                })
//                .route("/test2", new BaseHttpHandler() {
//                    @Override
//                    public void handle(HttpRequest request) throws IOException {
//                        request.getResponse().write(("test2").getBytes());
//                    }
//                })
//                .route("/ws", new BaseHttpHandler() {
//                    @Override
//                    public void handle(HttpRequest request) throws IOException {
//                        request.upgrade(new WebSocketUpgradeHandler() {
//
//                        });
//                    }
//                })
//                .route("/a/test1", new BaseHttpHandler() {
//                    @Override
//                    public void handle(HttpRequest request) throws IOException {
//                        request.getResponse().write(("test1").getBytes());
//                    }
//                })
                .route("/b/c/test1", ctx -> ctx.Response.write(("/b/c/test1").getBytes()))
                .route("/b/c/test2", ctx -> ctx.Response.write(("/b/c/test2").getBytes()));
        routeHandle.addInterceptor("/b/*", (context, completableFuture, chain) -> {
            System.out.println("intercept:" + context.Request.getRequestURI());
            chain.proceed(context, completableFuture);
        });

        // 3. 启动服务
        HttpServer bootstrap = new HttpServer();
        bootstrap.options().debug(true);
        bootstrap.httpHandler(routeHandle);
        bootstrap.listen();
    }
}
