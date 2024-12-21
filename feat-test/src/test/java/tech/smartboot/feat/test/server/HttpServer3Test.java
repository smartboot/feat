/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpServerTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test.server;

import com.alibaba.fastjson.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.feat.client.HttpClient;
import tech.smartboot.feat.client.HttpPost;
import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HeaderValueEnum;
import tech.smartboot.feat.common.enums.HttpMethodEnum;
import tech.smartboot.feat.server.HttpBootstrap;
import tech.smartboot.feat.server.HttpRequest;
import tech.smartboot.feat.server.HttpResponse;
import tech.smartboot.feat.server.HttpServerHandler;
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
public class HttpServer3Test extends BastTest {
    public static final String KEY_PARAMETERS = "parameters";
    public static final String KEY_METHOD = "method";
    public static final String KEY_URI = "uri";
    public static final String KEY_URL = "url";
    public static final String KEY_HEADERS = "headers";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer3Test.class);
    private HttpBootstrap bootstrap;
    private RequestUnit requestUnit;

    @Before
    public void init() {
        bootstrap = new HttpBootstrap();
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                //随机启用GZIP
                OutputStream outputStream;
                if (System.currentTimeMillis() % 2 == 0) {
                    response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValueEnum.GZIP.getName());
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
        bootstrap.configuration().readBufferSize(2 * 1024 * 1024);
        bootstrap.configuration().addPlugin(new StreamMonitorPlugin<>(StreamMonitorPlugin.BLUE_TEXT_INPUT_STREAM, StreamMonitorPlugin.RED_TEXT_OUTPUT_STREAM));
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
    public void testPost3() throws ExecutionException, InterruptedException {

        HttpClient httpClient = getHttpClient();
        HttpPost httpPost = httpClient.post(requestUnit.getUri());
        requestUnit.getHeaders().forEach((name, value) -> httpPost.header().add(name, value));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("author").append("=").append("三刀");
        for (int i = 0; i < 10000; i++) {
            stringBuilder.append("&").append("author").append(i).append("=").append("三刀").append(i);
        }
        httpPost.header().add("longText", stringBuilder.toString());
        httpPost.body().formUrlencoded(requestUnit.getParameters());

        JSONObject jsonObject = basicCheck(httpPost.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.POST.getMethod(), jsonObject.get(KEY_METHOD));
    }
    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
