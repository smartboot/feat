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

import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class GzipHttpDemo {
    public static void main(String[] args) {
        String text = "Hello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello World";
        Router routeHandle = new Router();
        routeHandle.route("/a", ctx -> {
            HttpResponse response = ctx.Response;
            byte[] data = text.getBytes();
            response.setContentLength(data.length);
//                response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValue.GZIP.getName());
            response.write(data);
        }).route("/b", ctx -> {
            HttpResponse response = ctx.Response;
            response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValue.ContentEncoding.GZIP);
            response.write(text.getBytes());
        }).route("/c", ctx -> {
            HttpResponse response = ctx.Response;
            response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValue.ContentEncoding.GZIP);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(response.getOutputStream());
            gzipOutputStream.write(("<html><body>hello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello " +
                    "worldhello worldhello worldhello worldhello worldhello worldhello worldhello worldhello world").getBytes());
            gzipOutputStream.write("hello world111".getBytes());
            gzipOutputStream.write("</body></html>".getBytes());
            gzipOutputStream.close();
        }).route("/d", ctx -> {
            HttpResponse response = ctx.Response;
            String content = "Hello world";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            gzipOutputStream.write(content.getBytes());
            gzipOutputStream.close();

            byte[] data = outputStream.toByteArray();
            response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValue.ContentEncoding.GZIP);
            response.setContentLength(data.length);
            response.write(data);
        });
        HttpServer bootstrap = new HttpServer();
        bootstrap.httpHandler(routeHandle);
        bootstrap.options().writeBufferSize(1024 * 1024).debug(true);
        bootstrap.listen(8080);
    }
}
