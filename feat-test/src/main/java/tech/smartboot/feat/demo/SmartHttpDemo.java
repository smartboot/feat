/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: SmartHttpDemo.java
 * Date: 2021-06-20
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpServerHandler;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.handler.BasicAuthServerHandler;
import tech.smartboot.feat.core.server.handler.HttpRouteHandler;
import tech.smartboot.feat.core.server.upgrade.websocket.WebSocketUpgradeHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * 打开浏览器请求：http://127.0.0.0:8080/
 *
 * @author 三刀
 * @version V1.0 , 2019/11/3
 */
public class SmartHttpDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartHttpDemo.class);

    public static void main(String[] args) {
        System.setProperty("smartHttp.server.alias", "SANDAO base on ");

        HttpRouteHandler routeHandle = new HttpRouteHandler();
        routeHandle.route("/basic", new BasicAuthServerHandler("admin", "admin1", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                response.write("success".getBytes());
            }
        }));
        routeHandle.route("/", new HttpServerHandler() {
                    byte[] body = ("<html>" +
                            "<head><title>feat demo</title></head>" +
                            "<body>" +
                            "GET 表单提交<form action='/get' method='get'><input type='text' name='text'/><input type='submit'/></form></br>" +
                            "POST 表单提交<form action='/post' method='post'><input type='text' name='text'/><input type='submit'/></form></br>" +
                            "文件上传<form action='/upload' method='post' enctype='multipart/form-data'><input type='file' name='text'/><input type='submit'/></form></br>" +
                            "</body></html>").getBytes();

                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {

                        response.setContentLength(body.length);
                        response.getOutputStream().write(body);
                    }
                })
                .route("/get", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        response.write(("收到Get参数text=" + request.getParameter("text")).getBytes());
                    }
                }).route("/post", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        response.write(("收到Post参数text=" + request.getParameter("text")).getBytes());
                    }
                }).route("/upload", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        InputStream in = request.getInputStream();
                        byte[] buffer = new byte[1024];
                        int len = 0;
                        while ((len = in.read(buffer)) != -1) {
                            response.getOutputStream().write(buffer, 0, len);
                        }
                        in.close();
                    }
                }).route("/post_json", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        InputStream in = request.getInputStream();
                        byte[] buffer = new byte[1024];
                        int len = 0;
                        System.out.println(request.getContentType());
                        while ((len = in.read(buffer)) != -1) {
                            response.getOutputStream().write(buffer, 0, len);
                        }
                        in.close();
                    }
                }).route("/plaintext", new HttpServerHandler() {
                    byte[] body = "Hello World!".getBytes();

                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        response.setContentLength(body.length);
                        response.setContentType("text/plain; charset=UTF-8");
                        response.write(body);
//                LOGGER.info("hello world");
                    }
                }).route("/head", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        response.addHeader("a", "b");
                        response.addHeader("a", "c");
                        Collection<String> headNames = request.getHeaderNames();
                        for (String headerName : headNames) {
                            response.write((headerName + ": " + request.getHeaders(headerName) + "</br>").getBytes());
                        }
                    }
                }).route("/post_param", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        //curl -X POST -H "Transfer-Encoding: chunked" -H "Content-Type: application/x-www-form-urlencoded" --data "field1=value1&field2=value2" http://localhost:8080/post_param
                        for (String parameter : request.getParameters().keySet()) {
                            response.write((parameter + ": " + request.getParameter(parameter) + "</br>").getBytes());
                        }
                    }
                }).route("/ws", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws Throwable {
                        request.upgrade(new WebSocketUpgradeHandler() {
                            @Override
                            public void onHandShake(WebSocketRequest request, WebSocketResponse webSocketResponse) {
                                System.out.println("收到握手消息");
                            }

                            @Override
                            public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String data) {
                                System.out.println("收到请求消息:" + data);
                                response.sendTextMessage("服务端收到响应:" + data);
                            }


                            @Override
                            public void handleBinaryMessage(WebSocketRequest request, WebSocketResponse response, byte[] data) {
                                response.sendBinaryMessage(data);
                            }
                        });
                    }
                });


        HttpServer bootstrap = new HttpServer();
        //配置HTTP消息处理管道
        bootstrap.httpHandler(routeHandle);
        bootstrap.configuration().debug(true);
        //设定服务器配置并启动
        bootstrap.start();
    }
}
