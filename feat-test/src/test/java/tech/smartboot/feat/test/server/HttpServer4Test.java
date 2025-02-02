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
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
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
public class HttpServer4Test extends BastTest {
    public static final String KEY_PARAMETERS = "parameters";
    public static final String KEY_METHOD = "method";
    public static final String KEY_URI = "uri";
    public static final String KEY_URL = "url";
    public static final String KEY_HEADERS = "headers";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer4Test.class);
    private HttpServer bootstrap;
    private RequestUnit requestUnit;

    @Before
    public void init() {
        bootstrap = new HttpServer();
        bootstrap.httpHandler(new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws IOException {
                HttpResponse response=request.getResponse();
                //随机启用GZIP
                OutputStream outputStream;
                if (System.currentTimeMillis() % 2 == 0) {
                    response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValue.ContentEncoding.GZIP);
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
        bootstrap.options().readBufferSize(16);
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

    /**
     * 缓冲区溢出
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testHeaderValueOverflow() throws ExecutionException, InterruptedException {

        HttpClient httpClient = getHttpClient();
        HttpPost httpPost = httpClient.post(requestUnit.getUri());
        requestUnit.getHeaders().forEach((name, value) -> httpPost.header().add(name, value));
        httpPost.header().add("overfLow", "1234567890abcdefghi");

        tech.smartboot.feat.core.client.HttpResponse response = httpPost.done().get();
        Assert.assertEquals(response.getStatus(), HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.value());
    }

    /**
     * 缓冲区溢出
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testHeaderValueOverflow2() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        HttpPost httpPost = httpClient.post(requestUnit.getUri());
        httpPost.header().add("1234567890abcdefghi", "1234567890abcdefghi");

        tech.smartboot.feat.core.client.HttpResponse response = httpPost.done().get();
        Assert.assertEquals(response.getStatus(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
