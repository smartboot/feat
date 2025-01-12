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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.extension.ssl.factory.ServerSSLContextFactory;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpServerHandler;
import tech.smartboot.feat.core.server.handler.HttpRouteHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author huqiang
 * @since 2021/3/2 10:57
 */
public class HttpPostTest {

    private HttpServer httpServer;
    private HttpServer httpsBootstrap;

    @Before
    public void init() throws Exception {

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
                System.out.println(request.getParameters());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(String.valueOf(request.getContentLength()).getBytes());
                InputStream inputStream = request.getInputStream();
                byte[] bytes = new byte[1024];
                int size;
                while ((size = inputStream.read(bytes)) != -1) {
                    byteArrayOutputStream.write(bytes, 0, size);
                }
                response.write(byteArrayOutputStream.toByteArray());
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

        routeHandle.route("/chunk", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws Throwable {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                InputStream inputStream = request.getInputStream();
                int b;
                while ((b = inputStream.read()) != -1) {
                    byteArrayOutputStream.write(b);
                    response.write(new byte[]{(byte) b});
                }

                System.out.println(byteArrayOutputStream.toString());
            }
        });

        routeHandle.route("/body", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws Throwable {
                System.out.println(request.getParameters());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(String.valueOf(request.getContentLength()).getBytes());
                InputStream inputStream = request.getInputStream();
                byte[] bytes = new byte[1024];
                int size;
                while ((size = inputStream.read(bytes)) != -1) {
                    byteArrayOutputStream.write(bytes, 0, size);
                }
                response.write(byteArrayOutputStream.toByteArray());
            }
        });

        routeHandle.route("/empty", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws Throwable {
                System.out.println(request.getParameters());
                response.setContentLength(0);
            }
        });

        httpServer = new HttpServer();
        httpServer.httpHandler(routeHandle).setPort(8080).start();

        SslPlugin sslPlugin =
                new SslPlugin(new ServerSSLContextFactory(HttpPostTest.class.getClassLoader().getResourceAsStream(
                        "server.keystore"), "123456", "123456"), ClientAuth.NONE);
        httpsBootstrap = new HttpServer();
        httpsBootstrap.options().addPlugin(sslPlugin);
        httpsBootstrap.httpHandler(routeHandle).setPort(8888).start();
    }

    @Test
    public void testChunkedRequest() throws Throwable {

        Consumer consumer = client -> {
            String body = "test a body string";
            client.configuration().debug(true);
            Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/chunk")
                    .header().keepalive(true).done()
                    .body()
                    .write(body.getBytes()).write(body.getBytes())
                    .done()
                    .onSuccess(response -> {
                        System.out.println(response.body());
                    })
                    .onFailure(t -> {
                        System.out.println(t.getMessage());
                    }).done();
            Assert.assertEquals(future.get().body(), body + body);
        };
        doRequest(new HttpClient("http://127.0.0.1:8080"), consumer);
        doRequest(new HttpClient("https://127.0.0.1:8888"), consumer);
    }

    private void doRequest(HttpClient client, Consumer consumer) throws Throwable {
        consumer.accept(client);
    }

    @Test
    public void testCheckHeader() throws Throwable {
        Consumer consumer = client -> {
            String body = "test a body string";
            client.configuration().debug(true);
            Future<tech.smartboot.feat.core.client.HttpResponse> future = client.post("/header")
                    .header().keepalive(true).setContentLength(body.getBytes().length).done()
                    .body()
                    .write(body.getBytes())
                    .done()
                    .onSuccess(response -> {
                        System.out.println(response.body());
                    })
                    .onFailure(t -> {
                        System.out.println(t.getMessage());
                    }).done();
            JSONObject jsonObject = JSONObject.parseObject(future.get().body());
            Assert.assertNull(jsonObject.getString(HeaderNameEnum.TRANSFER_ENCODING.getName()));
            Assert.assertEquals(jsonObject.getString(HeaderNameEnum.CONTENT_LENGTH.getName()),
                    String.valueOf(body.getBytes().length));
        };
        doRequest(new HttpClient("http://127.0.0.1:8080"), consumer);
        doRequest(new HttpClient("https://127.0.0.1:8888"), consumer);
    }

    @Test
    public void testChunked() throws Throwable {
        Consumer consumer = client -> {
            String body = "test a body string";
            String body2 = "test a body2 string";
            client.configuration().debug(true);
            Future<tech.smartboot.feat.core.client.HttpResponse> future1 = client.post("/other/abc")
                    .header().keepalive(true).setContentLength(body.getBytes().length).done()
                    .body()
                    .write(body.getBytes())
                    .done()
                    .onSuccess(response -> {
                        System.out.println(response.body());
                        System.out.println("1111");
                    })
                    .onFailure(t -> {
                        System.out.println(t.getMessage());
                    }).done();

            Future<tech.smartboot.feat.core.client.HttpResponse> future2 = client.post("/other/abc")
                    .header().keepalive(true).setContentLength(body2.getBytes().length).done()
                    .body()
                    .write(body2.getBytes())
                    .done()
                    .onSuccess(response -> {
                        System.out.println(response.body());
                        System.out.println("222");
                    })
                    .onFailure(t -> {
                        System.out.println(t.getMessage());
                    }).done();
            Assert.assertEquals(body, future1.get().body());
            Assert.assertEquals(body2, future2.get().body());
        };

    }

    @Test
    public void testPost() throws Throwable {
        Consumer consumer = httpClient -> {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            httpClient.configuration().debug(true);
            Map<String, String> param = new HashMap<>();
            param.put("name", "zhouyu");
            param.put("age", "18");
            httpClient.post("/post_param").header().setContentType(HeaderValueEnum.ContentType.X_WWW_FORM_URLENCODED).done().onSuccess(response -> {
                System.out.println(response.body());
                JSONObject jsonObject = JSONObject.parseObject(response.body());
                boolean suc = false;
                for (String key : param.keySet()) {
                    suc = StringUtils.equals(param.get(key), jsonObject.getString(key));
                    if (!suc) {
                        break;
                    }
                }
                httpClient.close();
                future.complete(suc);
            }).onFailure(throwable -> {
                System.out.println("异常A: " + throwable.getMessage());
                throwable.printStackTrace();
                Assert.fail();
                future.complete(false);
            }).body().formUrlencoded(param);
            Assert.assertTrue(future.get());
        };
        doRequest(new HttpClient("http://127.0.0.1:8080"), consumer);
        doRequest(new HttpClient("https://127.0.0.1:8888"), consumer);
    }

    @Test
    public void testJson() throws Throwable {
        Consumer consumer = httpClient -> {
            httpClient.configuration().debug(true);
            byte[] jsonBytes = "{\"a\":1,\"b\":\"123\"}".getBytes(StandardCharsets.UTF_8);
            httpClient.post("/json").header().setContentLength(jsonBytes.length).setContentType("application/json").done().body().write(jsonBytes).flush().done();
            Thread.sleep(100);
        };
        doRequest(new HttpClient("http://127.0.0.1:8080"), consumer);
        doRequest(new HttpClient("https://127.0.0.1:8888"), consumer);
    }

    @Test
    public void testBody() throws Throwable {
        Consumer consumer = httpClient -> {
            httpClient.configuration().debug(true);
            byte[] jsonBytes = "{\"a\":1,\"b\":\"123\"}".getBytes(StandardCharsets.UTF_8);
            String resp = httpClient.post("/body").header().setContentLength(jsonBytes.length).setContentType(
                    "application/json").done().body().write(jsonBytes).flush().done().done().get().body();
            Assert.assertEquals(resp, jsonBytes.length + new String(jsonBytes));
        };
        doRequest(new HttpClient("http://127.0.0.1:8080"), consumer);
        doRequest(new HttpClient("https://127.0.0.1:8888"), consumer);
    }

    @Test
    public void testEmpty() throws Throwable {
        Consumer consumer = httpClient -> {
            httpClient.configuration().debug(true);
            byte[] jsonBytes = "{\"a\":1,\"b\":\"123\"}".getBytes(StandardCharsets.UTF_8);
            tech.smartboot.feat.core.client.HttpResponse resp = httpClient.post("/empty").header().setContentLength(jsonBytes.length).setContentType(
                    "application/json").done().body().write(jsonBytes).flush().done().done().get();
            Assert.assertEquals(0, resp.getContentLength());
        };
        doRequest(new HttpClient("http://127.0.0.1:8080"), consumer);
        doRequest(new HttpClient("https://127.0.0.1:8888"), consumer);
    }

    @After
    public void destroy() {
        httpServer.shutdown();
        httpsBootstrap.shutdown();
    }


    interface Consumer {
        void accept(HttpClient httpClient) throws Throwable;
    }
}
