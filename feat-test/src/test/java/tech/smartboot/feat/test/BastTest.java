/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */
package tech.smartboot.feat.test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import org.junit.Assert;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.test.server.RequestUnit;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
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
        LOGGER.info(JSON.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat, JSONWriter.Feature.WriteMapNullValue));
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
