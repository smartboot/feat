/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpPostTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test.client;

import com.alibaba.fastjson.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.client.HttpClient;
import tech.smartboot.feat.server.HttpBootstrap;
import tech.smartboot.feat.server.HttpRequest;
import tech.smartboot.feat.server.HttpResponse;
import tech.smartboot.feat.server.HttpServerHandler;
import tech.smartboot.feat.server.handler.HttpRouteHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author huqiang
 * @since 2021/3/2 10:57
 */
public class HttpURLTest {

    private HttpBootstrap httpBootstrap;

    @Before
    public void init() {
        httpBootstrap = new HttpBootstrap();
        HttpRouteHandler routeHandle = new HttpRouteHandler();
        routeHandle.route("/post_param", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                JSONObject jsonObject = new JSONObject();
                for (String key : request.getParameters().keySet()) {
                    jsonObject.put(key, request.getParameter(key));
                }
                response.write(jsonObject.toString().getBytes());
            }
        });
        routeHandle.route("/json", new HttpServerHandler() {

            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                System.out.println("--");
                InputStream inputStream = request.getInputStream();
                byte[] bytes = new byte[1024];
                int size;
                while ((size = inputStream.read(bytes)) != -1) {
                    response.getOutputStream().write(bytes, 0, size);
                }
            }
        });
        routeHandle.route("/header", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                JSONObject jsonObject = new JSONObject();
                for (String header : request.getHeaderNames()) {
                    jsonObject.put(header, request.getHeader(header));
                }
                response.write(jsonObject.toJSONString().getBytes());
            }
        });

        routeHandle.route("/other/abc", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                System.out.println("--");
                InputStream inputStream = request.getInputStream();
                byte[] bytes = new byte[1024];
                int size;
                while ((size = inputStream.read(bytes)) != -1) {
                    response.getOutputStream().write(bytes, 0, size);
                }
            }
        });

        httpBootstrap.httpHandler(routeHandle).setPort(8080).start();
    }

    @Test
    public void testJson1() throws InterruptedException {
        HttpClient httpClient = new HttpClient("http://localhost:8080/json");
        httpClient.configuration().debug(true);
        byte[] jsonBytes = "{\"a\":1,\"b\":\"123\"}".getBytes(StandardCharsets.UTF_8);
        httpClient.post().header().setContentLength(jsonBytes.length).setContentType("application/json").done().body().write(jsonBytes).flush().done();
        httpClient.post().header().setContentLength(jsonBytes.length).setContentType("application/json").done().body().write(jsonBytes).flush().done();
        httpClient.post().body().write(jsonBytes).flush().done().done();
        Thread.sleep(100);
    }

    @After
    public void destroy() {
        httpBootstrap.shutdown();
    }
}
