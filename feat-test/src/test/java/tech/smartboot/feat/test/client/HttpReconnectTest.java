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
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

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
        Router routeHandler = new Router();

        routeHandler.route("/post", ctx -> {
            HttpRequest request = ctx.Request;
            HttpResponse response=ctx.Response;
            response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValue.Connection.keepalive);
            JSONObject jsonObject = new JSONObject();
            for (String key : request.getParameters().keySet()) {
                jsonObject.put(key, request.getParameter(key));
            }
            response.write(jsonObject.toString().getBytes());
            response.close();
        });
        httpServer.options().debug(true);
        httpServer.httpHandler(routeHandler).listen(8080);
    }

    @Test
    public void testPost() throws ExecutionException, InterruptedException {
        HttpClient httpClient = new HttpClient("localhost", 8080);
//        httpClient.configuration().debug(true);
        int i = 1000;
        while (i-- > 0) {
            Future<tech.smartboot.feat.core.client.HttpResponse> future = httpClient.post("/post")
                    .header(h->h.keepalive(true))
                    .onSuccess(response -> {
                        System.out.println(response.body());
                        httpClient.close();
                    })
                    .onFailure(throwable -> {
                        httpClient.close();
                    })
                    .submit();
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
