/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpPostTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test.client;

import com.alibaba.fastjson2.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author huqiang
 * @since 2021/3/2 10:57
 */
public class HttpURLTest {

    private HttpServer httpServer;

    @Before
    public void init() {
        httpServer = new HttpServer();
        Router routeHandle = new Router();
        routeHandle.route("/post_param", ctx -> {
            HttpRequest request = ctx.Request;
            JSONObject jsonObject = new JSONObject();
            for (String key : request.getParameters().keySet()) {
                jsonObject.put(key, request.getParameter(key));
            }
            request.getResponse().write(jsonObject.toString().getBytes());
        });
        routeHandle.route("/json", ctx -> {
            HttpRequest request = ctx.Request;
            System.out.println("--");
            InputStream inputStream = request.getInputStream();
            byte[] bytes = new byte[1024];
            int size;
            while ((size = inputStream.read(bytes)) != -1) {
                request.getResponse().getOutputStream().write(bytes, 0, size);
            }
        });
        routeHandle.route("/header", ctx -> {
            HttpRequest request = ctx.Request;
            JSONObject jsonObject = new JSONObject();
            for (String header : request.getHeaderNames()) {
                jsonObject.put(header, request.getHeader(header));
            }
            request.getResponse().write(jsonObject.toJSONString().getBytes());
        });

        routeHandle.route("/other/abc", ctx -> {
            HttpRequest request = ctx.Request;
            System.out.println("--");
            InputStream inputStream = request.getInputStream();
            byte[] bytes = new byte[1024];
            int size;
            while ((size = inputStream.read(bytes)) != -1) {
                request.getResponse().getOutputStream().write(bytes, 0, size);
            }
        });

        httpServer.httpHandler(routeHandle).listen(8080);
    }

    @Test
    public void testJson1() throws InterruptedException {
        HttpClient httpClient = new HttpClient("http://localhost:8080/json");
        httpClient.options().debug(true);
        byte[] jsonBytes = "{\"a\":1,\"b\":\"123\"}".getBytes(StandardCharsets.UTF_8);
        httpClient.post().header(h->h.setContentLength(jsonBytes.length).setContentType("application/json")).body(b->b.write(jsonBytes).flush());
        httpClient.post().header(h->h.setContentLength(jsonBytes.length).setContentType("application/json")).body(b->b.write(jsonBytes).flush());
        httpClient.post().body(b->b.write(jsonBytes).flush()).submit();
        Thread.sleep(100);
    }

    @After
    public void destroy() {
        httpServer.shutdown();
    }
}
