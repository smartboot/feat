/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpPostTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test.client;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.utils.NumberUtils;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.router.Router;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

/**
 * @author huqiang
 * @since 2021/3/2 10:57
 */
public class HttpGzipTest {

    private HttpServer httpServer;
    private final int chunk = 1024;

    @Before
    public void init() {
        httpServer = new HttpServer();
        Router routeHandle = new Router();
        routeHandle.route("/test", request -> {
            HttpResponse response=request.getResponse();
            int count = NumberUtils.toInt(request.getParameter("count"), 1);
            response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValue.ContentEncoding.GZIP);
            GZIPOutputStream outputStream = new GZIPOutputStream(response.getOutputStream());
            while (count-- > 0) {
                outputStream.write(new byte[chunk]);
            }
            outputStream.close();
        });

        routeHandle.route("/html", request -> {
            HttpResponse response=request.getResponse();
            response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValue.ContentEncoding.GZIP);
            GZIPOutputStream outputStream = new GZIPOutputStream(response.getOutputStream());
            outputStream.write("<html>".getBytes());
            outputStream.write("<body>".getBytes());
            outputStream.write("hello world".getBytes());
            outputStream.write("</body></html>".getBytes());
            outputStream.close();
        });

        httpServer.httpHandler(routeHandle).listen(8080);
    }

    @Test
    public void testCheckHeader() throws InterruptedException, ExecutionException {
        extracted(1);
    }

    @Test
    public void testCheckHeader2() throws InterruptedException, ExecutionException {
        extracted(2);
    }

    @Test
    public void testCheckHeader3() throws InterruptedException, ExecutionException {
        extracted(3);
    }

    @Test
    public void testGzip4() throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        client.options().debug(true);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/html")
                .header(h->h.keepalive(true))
                .onSuccess(response -> {
                    System.out.println(response.body());
                })
                .onFailure(t -> {
                    System.out.println(t.getMessage());
                }).submit();
        Assert.assertEquals(HeaderValue.ContentEncoding.GZIP, future.get().getHeader(HeaderNameEnum.CONTENT_ENCODING.getName()));
//        Assert.assertEquals(count * chunk, future.get().body().length());
    }

    private void extracted(int count) throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        client.options().debug(true);
        Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/test?count=" + count)
                .header(h->h.keepalive(true))
                .onSuccess(response -> {
//                    System.out.println(response.body());
                })
                .onFailure(t -> {
                    System.out.println(t.getMessage());
                }).submit();
        Assert.assertEquals(HeaderValue.ContentEncoding.GZIP, future.get().getHeader(HeaderNameEnum.CONTENT_ENCODING.getName()));
        Assert.assertEquals(count * chunk, future.get().body().length());
    }


    @After
    public void destroy() {
        httpServer.shutdown();
    }
}
