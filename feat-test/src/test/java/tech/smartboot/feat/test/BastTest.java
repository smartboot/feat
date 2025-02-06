/*
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: smart-servlet
 * file name: BastTest.java
 * Date: 2021-05-14
 * Author: sandao (zhengjunweimail@163.com)
 *
 */
package tech.smartboot.feat.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.test.server.RequestUnit;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/5/14
 */
public class BastTest {
    protected static final int SERVER_PORT = 8080;
    private static final Logger LOGGER = LoggerFactory.getLogger(BastTest.class);
    private static final String CONTENT_PATH = "/demo";
    public static final String KEY_URI = "uri";
    public static final String KEY_PARAMETERS = "parameters";
    public static final String KEY_HEADERS = "headers";

    protected HttpClient getHttpClient() {
        return new HttpClient("127.0.0.1", SERVER_PORT);
    }

    protected JSONObject basicCheck(HttpResponse response,
                                    RequestUnit requestUnit) {
        JSONObject jsonObject = JSON.parseObject(response.body());
        LOGGER.info(JSON.toJSONString(jsonObject, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat));
        Assert.assertEquals(requestUnit.getUri(), jsonObject.get(KEY_URI));

        JSONObject headerJson = jsonObject.getJSONObject(KEY_HEADERS);
        requestUnit.getHeaders().forEach((key, value) -> {
            Assert.assertEquals(value, headerJson.get(key));
        });

        JSONObject parameters = jsonObject.getJSONObject(KEY_PARAMETERS);
        requestUnit.getParameters().forEach((key, value) -> {
            Assert.assertEquals(value, parameters.get(key));
        });
        return jsonObject;
    }

    protected void checkPath(String path, HttpClient smartClient, HttpClient tomcatClient) {
        Future<HttpResponse> smartFuture = smartClient.get(CONTENT_PATH + path).onSuccess(resp -> {
            LOGGER.info("smart-servlet response: {}", resp.body());
        }).submit();
        Future<HttpResponse> tomcatFuture = tomcatClient.get(CONTENT_PATH + path).onSuccess(resp -> {
            LOGGER.info("tomcat response: {}", resp.body());
        }).submit();
        try {
            checkResponse(smartFuture.get(), tomcatFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected void checkResponse(HttpResponse smartResponse, HttpResponse tomcatResponse) {
        JSONObject smartJson = JSONObject.parseObject(smartResponse.body());
        JSONObject tomcatJson = JSONObject.parseObject(tomcatResponse.body());
        Assert.assertEquals("key 数量不一致", smartJson.size(), tomcatJson.size());
        for (String key : smartJson.keySet()) {
            Assert.assertEquals("key: " + key + " 匹配失败", smartJson.getString(key), tomcatJson.getString(key));
        }
    }
}
