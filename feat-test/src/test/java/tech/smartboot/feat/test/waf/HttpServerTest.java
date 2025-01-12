/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpServerTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test.waf;

import com.alibaba.fastjson.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpMethodEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpServerHandler;
import tech.smartboot.feat.test.BastTest;
import tech.smartboot.feat.test.server.RequestUnit;

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
    public static final String KEY_PARAMETERS = "parameters";
    public static final String KEY_METHOD = "method";
    public static final String KEY_URI = "uri";
    public static final String KEY_URL = "url";
    public static final String KEY_HEADERS = "headers";
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
        });
        bootstrap.options().addPlugin(new StreamMonitorPlugin<>(StreamMonitorPlugin.BLUE_TEXT_INPUT_STREAM, StreamMonitorPlugin.RED_TEXT_OUTPUT_STREAM));
        bootstrap.listen(SERVER_PORT);

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
        bootstrap.options().getWafConfiguration()
                .addAllowMethod(HttpMethodEnum.POST.getMethod());
        HttpClient httpClient = getHttpClient();
        StringBuilder uriStr = new StringBuilder(requestUnit.getUri()).append("?");
        requestUnit.getParameters().forEach((key, value) -> uriStr.append(key).append('=').append(value).append('&'));
        HttpGet httpGet = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet.header().add(name, value));

        Assert.assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), httpGet.done().get().getStatus());
    }

    @Test
    public void testGet1() throws ExecutionException, InterruptedException {
        bootstrap.options().getWafConfiguration()
                .addAllowMethod(HttpMethodEnum.GET.getMethod());
        HttpClient httpClient = getHttpClient();
        StringBuilder uriStr = new StringBuilder(requestUnit.getUri()).append("?");
        requestUnit.getParameters().forEach((key, value) -> uriStr.append(key).append('=').append(value).append('&'));
        HttpGet httpGet = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet.header().add(name, value));
        JSONObject jsonObject = basicCheck(httpGet.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testURI() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        StringBuilder uriStr = new StringBuilder(requestUnit.getUri()).append("?");
        requestUnit.getParameters().forEach((key, value) -> uriStr.append(key).append('=').append(value).append('&'));
        HttpGet httpGet = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet.header().add(name, value));
        JSONObject jsonObject = basicCheck(httpGet.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));

        bootstrap.options().getWafConfiguration()
                .addAllowUriPrefix("/aa");
        HttpGet httpGet1 = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet1.header().add(name, value));
        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), httpGet1.done().get().getStatus());

        bootstrap.options().getWafConfiguration()
                .getAllowUriPrefixes().add("/hello");
        HttpGet httpGet2 = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet2.header().add(name, value));
        jsonObject = basicCheck(httpGet2.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));

        bootstrap.options().getWafConfiguration()
                .getAllowUriPrefixes().clear();
        bootstrap.options().getWafConfiguration().getAllowUriSuffixes().add("/aa");
        HttpGet httpGet3 = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet3.header().add(name, value));
        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), httpGet3.done().get().getStatus());

        bootstrap.options().getWafConfiguration()
                .getAllowUriSuffixes().add("llo");
        HttpGet httpGet4 = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet4.header().add(name, value));
        jsonObject = basicCheck(httpGet4.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }


    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
