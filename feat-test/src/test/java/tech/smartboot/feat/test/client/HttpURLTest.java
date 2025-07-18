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
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
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
        httpClient.post().header(h -> h.setContentLength(jsonBytes.length).setContentType("application/json")).body(b -> b.write(jsonBytes).flush());
        httpClient.post().header(h -> h.setContentLength(jsonBytes.length).setContentType("application/json")).body(b -> b.write(jsonBytes).flush());
        httpClient.post().body(b -> b.write(jsonBytes).flush()).submit();
        Thread.sleep(100);
    }

    @After
    public void destroy() {
        httpServer.shutdown();
    }
}
