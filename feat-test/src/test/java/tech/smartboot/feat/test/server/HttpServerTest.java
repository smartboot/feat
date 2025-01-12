/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpServerTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpMethodEnum;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServerHandler;
import tech.smartboot.feat.test.BastTest;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPOutputStream;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/4
 */
public class HttpServerTest extends BastTest {

    public static final String KEY_METHOD = "method";

    public static final String KEY_URL = "url";

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerTest.class);
    private HttpServer bootstrap;
    private RequestUnit requestUnit;

    @Before
    public void init() {
        bootstrap = new HttpServer();
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                //随机启用GZIP
                OutputStream outputStream;
                if (System.currentTimeMillis() % 2 == 0) {
                    response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValueEnum.ContentEncoding.GZIP);
                    outputStream = new GZIPOutputStream(response.getOutputStream());
                } else {
                    outputStream = response.getOutputStream();
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(KEY_METHOD, request.getMethod());
                jsonObject.put(KEY_URI, request.getRequestURI());
                jsonObject.put(KEY_URL, request.getRequestURL());

                Map<String, String> parameterMap = new HashMap<>();
                request.getParameters().keySet().forEach(parameter -> parameterMap.put(parameter, request.getParameter(parameter)));
                jsonObject.put(KEY_PARAMETERS, parameterMap);

                Map<String, String> headerMap = new HashMap<>();
                request.getHeaderNames().forEach(headerName -> headerMap.put(headerName, request.getHeader(headerName)));
                jsonObject.put(KEY_HEADERS, headerMap);

                outputStream.write(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
                outputStream.close();
            }
        }).setPort(SERVER_PORT);
        bootstrap.options().addPlugin(new StreamMonitorPlugin<>(StreamMonitorPlugin.BLUE_TEXT_INPUT_STREAM, StreamMonitorPlugin.RED_TEXT_OUTPUT_STREAM));
        bootstrap.start();

        requestUnit = new RequestUnit();
        requestUnit.setUri("/hello");
        Map<String, String> headers = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            headers.put("header_" + i, UUID.randomUUID().toString());
        }
        headers.put("header_empty", "");
        requestUnit.setHeaders(headers);
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            params.put("params_" + i, UUID.randomUUID().toString());
        }
        requestUnit.setParameters(params);
    }

    @Test
    public void testGet() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        StringBuilder uriStr = new StringBuilder(requestUnit.getUri()).append("?");
        requestUnit.getParameters().forEach((key, value) -> uriStr.append(key).append('=').append(value).append('&'));
        HttpGet httpGet = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet.header().add(name, value));

        JSONObject jsonObject = basicCheck(httpGet.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testGet1() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        requestUnit.getParameters().put("author", "三刀");
        HttpGet httpGet = httpClient.get(requestUnit.getUri());
        requestUnit.getParameters().forEach(httpGet::addQueryParam);
        requestUnit.getHeaders().forEach((name, value) -> httpGet.header().add(name, value));

        JSONObject jsonObject = basicCheck(httpGet.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testGet2() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("/hello?").addQueryParam("author", "三刀").addQueryParam("abc", "123").done().get();

        JSONObject jsonObject = JSON.parseObject(response.body());
        JSONObject parameters = jsonObject.getJSONObject(KEY_PARAMETERS);
        Assert.assertEquals("123", parameters.get("abc"));
        Assert.assertEquals("三刀", parameters.get("author"));
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testGet3() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("/hello").addQueryParam("author", "三刀").addQueryParam("abc", "123").done().get();

        JSONObject jsonObject = JSON.parseObject(response.body());
        JSONObject parameters = jsonObject.getJSONObject(KEY_PARAMETERS);
        Assert.assertEquals("123", parameters.get("abc"));
        Assert.assertEquals("三刀", parameters.get("author"));
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testGet4() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("/hello#").addQueryParam("author", "三刀").addQueryParam("abc", "123").done().get();

        JSONObject jsonObject = JSON.parseObject(response.body());
        JSONObject parameters = jsonObject.getJSONObject(KEY_PARAMETERS);
        Assert.assertEquals("123", parameters.get("abc"));
        Assert.assertEquals("三刀", parameters.get("author"));
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testGet5() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("/hello#abca=sdf").addQueryParam("author", "三刀").addQueryParam("abc", "123").done().get();

        JSONObject jsonObject = JSON.parseObject(response.body());
        JSONObject parameters = jsonObject.getJSONObject(KEY_PARAMETERS);
        Assert.assertEquals("123", parameters.get("abc"));
        Assert.assertEquals("三刀", parameters.get("author"));
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testGet6() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("/hello?a=b#abca=sdf").addQueryParam("author", "三刀").addQueryParam("abc", "123").done().get();

        JSONObject jsonObject = JSON.parseObject(response.body());
        JSONObject parameters = jsonObject.getJSONObject(KEY_PARAMETERS);
        Assert.assertEquals("123", parameters.get("abc"));
        Assert.assertEquals("三刀", parameters.get("author"));
        Assert.assertEquals("b", parameters.get("a"));
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testGet7() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        tech.smartboot.feat.core.client.HttpResponse response = httpClient.get("/hello?#abca=sdf").addQueryParam("author", "三刀").addQueryParam("abc", "123").done().get();

        JSONObject jsonObject = JSON.parseObject(response.body());
        JSONObject parameters = jsonObject.getJSONObject(KEY_PARAMETERS);
        Assert.assertEquals("123", parameters.get("abc"));
        Assert.assertEquals("三刀", parameters.get("author"));
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testPost() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        HttpPost httpPost = httpClient.post(requestUnit.getUri());
        requestUnit.getHeaders().forEach((name, value) -> httpPost.header().add(name, value));
        httpPost.body().formUrlencoded(requestUnit.getParameters());

        JSONObject jsonObject = basicCheck(httpPost.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.POST.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testPost1() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        HttpPost httpPost = httpClient.post(requestUnit.getUri());
        requestUnit.getHeaders().forEach((name, value) -> httpPost.header().add(name, value));
        requestUnit.getParameters().put("author", "三刀");
        httpPost.body().formUrlencoded(requestUnit.getParameters());

        JSONObject jsonObject = basicCheck(httpPost.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.POST.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testPost2() throws ExecutionException, InterruptedException {
        bootstrap.options().readBufferSize(2 * 1024 * 1024);
        HttpClient httpClient = getHttpClient();
        HttpPost httpPost = httpClient.post(requestUnit.getUri());
        requestUnit.getHeaders().forEach((name, value) -> httpPost.header().add(name, value));
        for (int i = 0; i < 10000; i++) {
            requestUnit.getParameters().put("author" + i, "三刀" + i);
        }
        httpPost.body().formUrlencoded(requestUnit.getParameters());

        JSONObject jsonObject = basicCheck(httpPost.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.POST.getMethod(), jsonObject.get(KEY_METHOD));
    }


    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
