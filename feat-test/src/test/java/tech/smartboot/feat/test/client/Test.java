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

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class Test {
    @org.junit.Test
    public void testPipline() throws Exception {
        HttpServer bootstrap = new HttpServer();
        Router route = new Router();
        byte[] body = new byte[4096];
        route.route("/other/**", ctx -> {
            HttpRequest request = ctx.Request;
            HttpResponse response = request.getResponse();
            System.out.println("=====");
            System.out.println(request.getMethod());
            System.out.println("=====");
            InputStream inputStream = request.getInputStream();
            int len = inputStream.read(body);
            if (len < 0) {
                System.out.println("no body request");
            } else {
                System.out.println(new String(body, 0, len, StandardCharsets.UTF_8));
                System.out.println(inputStream.read(body));
            }
            response.setHeader(HeaderName.CONNECTION.getName(), HeaderValue.Connection.KEEPALIVE);
            response.write("success".getBytes());
        });
        bootstrap
                .httpHandler(route)
                .options()
                .debug(true)
                .bannerEnabled(true)
                .serverName("fun-car");
        bootstrap

                .listen(8098);


        HttpClient client = new HttpClient("127.0.0.1", 8098);
//        client.configuration().debug(true);
        byte[] bytes = "test a body string".getBytes(StandardCharsets.UTF_8);
        CountDownLatch latch = new CountDownLatch(2);
        client.post("/other/abc?k=v&v=s")
//                .header().keepalive(true).done()
                .body(b -> b.write(bytes))
                .onSuccess(response -> {
                    System.out.println("======1=======>" + response.body());
                    latch.countDown();
                })
                .onFailure(t -> {
                    System.out.println(t.getMessage());
                    latch.countDown();
                }).submit();
        System.out.println("======================");
        client.post("/other/abc")
//                .header().keepalive(true).done()
                .body(b -> b.write(bytes))
                .onSuccess(response -> {
                    System.out.println("=======2======>" + response.body());
                    latch.countDown();
                })
                .onFailure(t -> {
                    System.out.println(t.getMessage());
                    latch.countDown();
                }).submit();
        latch.await();
    }
}
