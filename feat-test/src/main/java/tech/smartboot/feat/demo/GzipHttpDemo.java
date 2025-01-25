/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: GzipHttpDemo.java
 * Date: 2021-10-24
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.router.Router;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/10/24
 */
public class GzipHttpDemo {
    public static void main(String[] args) {
        String text = "Hello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello World";
        Router routeHandle = new Router();
        routeHandle.route("/a", new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws IOException {
                HttpResponse response=request.getResponse();
                byte[] data = text.getBytes();
                response.setContentLength(data.length);
//                response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValueEnum.GZIP.getName());
                response.write(data);
            }
        }).route("/b", new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws IOException {
                HttpResponse response=request.getResponse();
                response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValueEnum.ContentEncoding.GZIP);
                response.write(text.getBytes());
            }
        }).route("/c", new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws Throwable {
                HttpResponse response=request.getResponse();
                response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValueEnum.ContentEncoding.GZIP);
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(response.getOutputStream());
                gzipOutputStream.write(("<html><body>hello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello " +
                        "worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello world").getBytes());
                gzipOutputStream.write("hello world111".getBytes());
                gzipOutputStream.write("</body></html>".getBytes());
                gzipOutputStream.close();
            }
        }).route("/d", new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws Throwable {
                HttpResponse response=request.getResponse();
                String content = "Hello world";
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
                gzipOutputStream.write(content.getBytes());
                gzipOutputStream.close();

                byte[] data = outputStream.toByteArray();
                response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValueEnum.ContentEncoding.GZIP);
                response.setContentLength(data.length);
                response.write(data);
            }
        });
        HttpServer bootstrap = new HttpServer();
        bootstrap.httpHandler(routeHandle);
        bootstrap.options().writeBufferSize(1024 * 1024).debug(true);
        bootstrap.listen(8080);
    }
}
