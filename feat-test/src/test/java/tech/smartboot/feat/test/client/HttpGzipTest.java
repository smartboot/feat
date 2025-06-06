/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.client;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class HttpGzipTest {

    private HttpServer httpServer;
    private final int chunk = 1024;

    @Before
    public void init() {
        httpServer = new HttpServer();
        Router routeHandle = new Router();
        routeHandle.route("/test", ctx -> {
            tech.smartboot.feat.core.server.HttpResponse response = ctx.Response;
            int count = FeatUtils.toInt(ctx.Request.getParameter("count"), 1);
            response.setHeader(HeaderName.CONTENT_ENCODING, HeaderValue.ContentEncoding.GZIP);
            GZIPOutputStream outputStream = new GZIPOutputStream(response.getOutputStream());
            while (count-- > 0) {
                outputStream.write(new byte[chunk]);
            }
            outputStream.close();
        });

        routeHandle.route("/html", ctx -> {
            tech.smartboot.feat.core.server.HttpResponse response = ctx.Response;
            response.setHeader(HeaderName.CONTENT_ENCODING, HeaderValue.ContentEncoding.GZIP);
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
        Future<HttpResponse> future = client.post("/html")
                .header(h -> h.keepalive(true))
                .onSuccess(response -> {
                    System.out.println(response.body());
                })
                .onFailure(t -> {
                    System.out.println(t.getMessage());
                }).submit();
        Assert.assertEquals(HeaderValue.ContentEncoding.GZIP, future.get().getHeader(HeaderName.CONTENT_ENCODING));
//        Assert.assertEquals(count * chunk, future.get().body().length());
    }

    private void extracted(int count) throws InterruptedException, ExecutionException {
        HttpClient client = new HttpClient("127.0.0.1", 8080);
        client.options().debug(true);
        Future<HttpResponse> future = client.post("/test?count=" + count)
                .header(h -> h.keepalive(true))
                .onSuccess(response -> {
//                    System.out.println(response.body());
                })
                .onFailure(t -> {
                    System.out.println(t.getMessage());
                }).submit();
        Assert.assertEquals(HeaderValue.ContentEncoding.GZIP, future.get().getHeader(HeaderName.CONTENT_ENCODING));
        Assert.assertEquals(count * chunk, future.get().body().length());
    }


    @After
    public void destroy() {
        httpServer.shutdown();
    }
}
