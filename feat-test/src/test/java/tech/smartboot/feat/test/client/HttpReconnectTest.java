/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpRestTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.test.client;

import com.alibaba.fastjson.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpServerHandler;
import tech.smartboot.feat.core.server.handler.HttpRouteHandler;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/7
 */
public class HttpReconnectTest {

    private HttpServer httpServer;


    @Before
    public void init() {
        httpServer = new HttpServer();
        HttpRouteHandler routeHandler = new HttpRouteHandler();

        routeHandler.route("/post", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValueEnum.Connection.keepalive);
                JSONObject jsonObject = new JSONObject();
                for (String key : request.getParameters().keySet()) {
                    jsonObject.put(key, request.getParameter(key));
                }
                response.write(jsonObject.toString().getBytes());
                response.close();
            }
        });
        httpServer.options().debug(true);
        httpServer.httpHandler(routeHandler).setPort(8080).start();
    }

    @Test
    public void testPost() throws ExecutionException, InterruptedException {
        HttpClient httpClient = new HttpClient("localhost", 8080);
//        httpClient.configuration().debug(true);
        int i = 1000;
        while (i-- > 0) {
            Future<tech.smartboot.feat.core.client.HttpResponse> future = httpClient.post("/post")
                    .header().keepalive(true).done()
                    .onSuccess(response -> {
                        System.out.println(response.body());
                        httpClient.close();
                    })
                    .onFailure(throwable -> {
                        httpClient.close();
                    })
                    .done();
            if (i % 3 == 0) {
                Thread.sleep(10);
            }
        }
    }

    @After
    public void destroy() {
        httpServer.shutdown();
    }

}
