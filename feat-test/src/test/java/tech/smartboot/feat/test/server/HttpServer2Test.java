/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpMethod;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.test.BastTest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class HttpServer2Test extends BastTest {
    public static final String KEY_PARAMETERS = "parameters";
    public static final String KEY_METHOD = "method";
    public static final String KEY_URI = "uri";
    public static final String KEY_URL = "url";
    public static final String KEY_HEADERS = "headers";
    HttpClient httpClient;
    HttpServer bootstrap;

    @Before
    public void init() {
        bootstrap = new HttpServer();
        bootstrap.options().debug(true);
        bootstrap.listen(SERVER_PORT);
        httpClient = getHttpClient();
    }

    @Test
    public void test1() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            response.setContentType("test");
            response.write("Hello World".getBytes(StandardCharsets.UTF_8));
        });
        tech.smartboot.feat.core.client.HttpResponse httpResponse = httpClient.get("/").submit().get();
        Assert.assertEquals(httpResponse.getContentType(), "test");
        Assert.assertEquals(httpResponse.getHeader(HeaderNameEnum.TRANSFER_ENCODING.getName()), HeaderValue.TransferEncoding.CHUNKED);
    }

    @Test
    public void test2() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(request -> request.getResponse().write("Hello World".getBytes(StandardCharsets.UTF_8)));
        tech.smartboot.feat.core.client.HttpResponse httpResponse = httpClient.get("/").submit().get();
        Assert.assertEquals(httpResponse.getProtocol(), HttpProtocolEnum.HTTP_11.getProtocol());
        Assert.assertEquals(httpResponse.getHeader(HeaderNameEnum.TRANSFER_ENCODING.getName()), HeaderValue.TransferEncoding.CHUNKED);
    }

    @Test
    public void testPut() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(request -> request.getResponse().write("Hello World".getBytes(StandardCharsets.UTF_8)));
        tech.smartboot.feat.core.client.HttpResponse httpResponse = httpClient.rest(HttpMethod.PUT, "/").submit().get();
        Assert.assertEquals(httpResponse.getProtocol(), HttpProtocolEnum.HTTP_11.getProtocol());
        Assert.assertEquals(httpResponse.statusCode(), HttpStatus.OK.value());
    }

    @Test
    public void testPost() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(request -> {
            request.getInputStream().close();
            request.getResponse().write("Hello World".getBytes(StandardCharsets.UTF_8));
        });
        for (int i = 0; i < 10; i++) {
            String body = "hello" + i;
            tech.smartboot.feat.core.client.HttpResponse httpResponse = httpClient.post("/").header(h -> h.setContentLength(body.length())).body(b -> b.write(body)).submit().get();
            Assert.assertEquals(httpResponse.getProtocol(), HttpProtocolEnum.HTTP_11.getProtocol());
            Assert.assertEquals(httpResponse.statusCode(), HttpStatus.OK.value());
            Assert.assertEquals(httpResponse.body(), "Hello World");
        }

    }

    @Test
    public void testPost1() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(request -> {
            byte[] buffer = new byte[(int) request.getContentLength()];
            request.getInputStream().read(buffer);
//                request.getInputStream().close();
            request.getResponse().write(buffer);
        });
        for (int i = 0; i < 10; i++) {
            String body = "hello" + i;
            tech.smartboot.feat.core.client.HttpResponse httpResponse = httpClient.post("/").header(h -> h.keepalive(true).setContentLength(body.length())).body(b -> b.write(body)).submit().get();
            Assert.assertEquals(httpResponse.getProtocol(), HttpProtocolEnum.HTTP_11.getProtocol());
            Assert.assertEquals(httpResponse.statusCode(), HttpStatus.OK.value());
            Assert.assertEquals(httpResponse.body(), body);
        }

    }

    @Test
    public void testPost2() throws ExecutionException, InterruptedException {
        Map<String, String> param = new HashMap<>();
        param.put("p", "p");
        param.put("p[", "p");
        param.put("p]", "p");
        param.put("p<]>", "p");

        bootstrap.httpHandler(request -> {
            HttpResponse response = request.getResponse();
            for (String key : request.getParameters().keySet()) {
                if (!Objects.equals(param.get(key), request.getParameter(key))) {
                    response.write("fail");
                    return;
                }
            }
            response.write("ok");
        });

        tech.smartboot.feat.core.client.HttpResponse httpResponse = httpClient.post("/").header(h -> h.keepalive(true)).body().formUrlencoded(param).submit().get();
        Assert.assertEquals(httpResponse.getProtocol(), HttpProtocolEnum.HTTP_11.getProtocol());
        Assert.assertEquals(httpResponse.statusCode(), HttpStatus.OK.value());
        Assert.assertEquals("ok", httpResponse.body());
    }

    @After
    public void destroy() {
        httpClient.close();
        bootstrap.shutdown();
    }
}
