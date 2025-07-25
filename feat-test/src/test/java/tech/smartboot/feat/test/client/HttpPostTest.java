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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.extension.ssl.factory.ServerSSLContextFactory;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HttpPostTest {

    private HttpServer httpServer;
    private HttpServer httpsBootstrap;

    @Before
    public void init() throws Exception {

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
            System.out.println(request.getParameters());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(String.valueOf(request.getContentLength()).getBytes());
            InputStream inputStream = request.getInputStream();
            byte[] bytes = new byte[1024];
            int size;
            while ((size = inputStream.read(bytes)) != -1) {
                byteArrayOutputStream.write(bytes, 0, size);
            }
            request.getResponse().write(byteArrayOutputStream.toByteArray());
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

        routeHandle.route("/chunk", ctx -> {
            HttpRequest request = ctx.Request;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            InputStream inputStream = request.getInputStream();
            int b;
            while ((b = inputStream.read()) != -1) {
                byteArrayOutputStream.write(b);
                request.getResponse().write(new byte[]{(byte) b});
            }

            System.out.println(byteArrayOutputStream.toString());
        });

        routeHandle.route("/body", ctx -> {
            HttpRequest request = ctx.Request;
            System.out.println(request.getParameters());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(String.valueOf(request.getContentLength()).getBytes());
            InputStream inputStream = request.getInputStream();
            byte[] bytes = new byte[1024];
            int size;
            while ((size = inputStream.read(bytes)) != -1) {
                byteArrayOutputStream.write(bytes, 0, size);
            }
            request.getResponse().write(byteArrayOutputStream.toByteArray());
        });

        routeHandle.route("/empty", ctx -> {
            HttpRequest request = ctx.Request;
            System.out.println(request.getParameters());
            request.getResponse().setContentLength(0);
        });

        httpServer = new HttpServer();
        httpServer.httpHandler(routeHandle).listen(8080);

        SslPlugin sslPlugin =
                new SslPlugin(new ServerSSLContextFactory(HttpPostTest.class.getClassLoader().getResourceAsStream(
                        "server.keystore"), "123456", "123456"), ClientAuth.NONE);
        httpsBootstrap = new HttpServer();
        httpsBootstrap.options().addPlugin(sslPlugin);
        httpsBootstrap.httpHandler(routeHandle).listen(8888);
    }

    @Test
    public void testChunkedRequest() throws Throwable {

        Consumer consumer = client -> {
            String body = "test a body string";
            client.options().debug(true);
            Future<HttpResponse> future = client.post("/chunk")
                    .header(h -> h.keepalive(true))
                    .body(b -> b.write(body.getBytes()).write(body.getBytes()))
                    .onSuccess(response -> {
                        System.out.println(response.body());
                    })
                    .onFailure(t -> {
                        System.out.println(t.getMessage());
                    }).submit();
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
            client.options().debug(true);
            Future<HttpResponse> future = client.post("/header")
                    .header(h -> h.keepalive(true).setContentLength(body.getBytes().length))
                    .body(b -> b.write(body.getBytes()))
                    .onSuccess(response -> {
                        System.out.println(response.body());
                    })
                    .onFailure(t -> {
                        System.out.println(t.getMessage());
                    }).submit();
            JSONObject jsonObject = JSONObject.parseObject(future.get().body());
            Assert.assertNull(jsonObject.getString(HeaderName.TRANSFER_ENCODING.getName()));
            Assert.assertEquals(jsonObject.getString(HeaderName.CONTENT_LENGTH.getName()),
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
            client.options().debug(true);
            Future<HttpResponse> future1 = client.post("/other/abc")
                    .header(h -> h.keepalive(true).setContentLength(body.getBytes().length))
                    .body(b -> b.write(body.getBytes()))
                    .onSuccess(response -> {
                        System.out.println(response.body());
                        System.out.println("1111");
                    })
                    .onFailure(t -> {
                        System.out.println(t.getMessage());
                    }).submit();

            Future<HttpResponse> future2 = client.post("/other/abc")
                    .header(h -> h.keepalive(true).setContentLength(body2.getBytes().length))
                    .body(b -> b.write(body2.getBytes()))
                    .onSuccess(response -> {
                        System.out.println(response.body());
                        System.out.println("222");
                    })
                    .onFailure(t -> {
                        System.out.println(t.getMessage());
                    }).submit();
            Assert.assertEquals(body, future1.get().body());
            Assert.assertEquals(body2, future2.get().body());
        };

    }

    @Test
    public void testPost() throws Throwable {
        Consumer consumer = httpClient -> {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            httpClient.options().debug(true);
            Map<String, String> param = new HashMap<>();
            param.put("name", "zhouyu");
            param.put("age", "18");
            httpClient.post("/post_param").header(h -> h.setContentType(HeaderValue.ContentType.X_WWW_FORM_URLENCODED)).onSuccess(response -> {
                System.out.println(response.body());
                JSONObject jsonObject = JSONObject.parseObject(response.body());
                boolean suc = false;
                for (String key : param.keySet()) {
                    suc = FeatUtils.equals(param.get(key), jsonObject.getString(key));
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
            }).postBody(p -> p.formUrlencoded(param));
            Assert.assertTrue(future.get());
        };
        doRequest(new HttpClient("http://127.0.0.1:8080"), consumer);
        doRequest(new HttpClient("https://127.0.0.1:8888"), consumer);
    }

    @Test
    public void testJson() throws Throwable {
        Consumer consumer = httpClient -> {
            httpClient.options().debug(true);
            byte[] jsonBytes = "{\"a\":1,\"b\":\"123\"}".getBytes(StandardCharsets.UTF_8);
            httpClient.post("/json").header(h -> h.setContentLength(jsonBytes.length).setContentType("application/json")).body(b -> b.write(jsonBytes).flush());
            Thread.sleep(100);
        };
        doRequest(new HttpClient("http://127.0.0.1:8080"), consumer);
        doRequest(new HttpClient("https://127.0.0.1:8888"), consumer);
    }

    @Test
    public void testBody() throws Throwable {
        Consumer consumer = httpClient -> {
            httpClient.options().debug(true);
            byte[] jsonBytes = "{\"a\":1,\"b\":\"123\"}".getBytes(StandardCharsets.UTF_8);
            String resp = httpClient.post("/body").header(h -> h.setContentLength(jsonBytes.length).setContentType(
                    "application/json")).body(b -> b.write(jsonBytes).flush()).submit().get().body();
            Assert.assertEquals(resp, jsonBytes.length + new String(jsonBytes));
        };
        doRequest(new HttpClient("http://127.0.0.1:8080"), consumer);
        doRequest(new HttpClient("https://127.0.0.1:8888"), consumer);
    }

    @Test
    public void testEmpty() throws Throwable {
        Consumer consumer = httpClient -> {
            httpClient.options().debug(true);
            byte[] jsonBytes = "{\"a\":1,\"b\":\"123\"}".getBytes(StandardCharsets.UTF_8);
            HttpResponse resp = httpClient.post("/empty").header(h -> h.setContentLength(jsonBytes.length).setContentType(
                    "application/json")).body(b -> b.write(jsonBytes).flush()).submit().get();
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
