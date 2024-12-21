package tech.smartboot.feat.test.client;

import tech.smartboot.feat.client.HttpClient;
import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HeaderValueEnum;
import tech.smartboot.feat.server.HttpBootstrap;
import tech.smartboot.feat.server.HttpRequest;
import tech.smartboot.feat.server.HttpResponse;
import tech.smartboot.feat.server.HttpServerHandler;
import tech.smartboot.feat.server.handler.HttpRouteHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 6/8/23
 */
public class Test {
    @org.junit.Test
    public void testPipline() throws Exception {
        HttpBootstrap bootstrap = new HttpBootstrap();
        HttpRouteHandler route = new HttpRouteHandler();
        byte[] body = new byte[4096];
        route.route("/other/**", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {

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
                response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValueEnum.KEEPALIVE.getName());
                response.write("success".getBytes());
            }
        });
        bootstrap
                .httpHandler(route)
                .configuration()
                .debug(true)
                .bannerEnabled(true)
                .serverName("fun-car");
        bootstrap
                .setPort(8098)
                .start();


        HttpClient client = new HttpClient("127.0.0.1", 8098);
//        client.configuration().debug(true);
        byte[] bytes = "test a body string".getBytes(StandardCharsets.UTF_8);
        CountDownLatch latch = new CountDownLatch(2);
        client.post("/other/abc?k=v&v=s")
//                .header().keepalive(true).done()
                .body().write(bytes).done()
                .onSuccess(response -> {
                    System.out.println("======1=======>" + response.body());
                    latch.countDown();
                })
                .onFailure(t -> {
                    System.out.println(t.getMessage());
                    latch.countDown();
                }).done();
        System.out.println("======================");
        client.post("/other/abc")
//                .header().keepalive(true).done()
                .body().write(bytes).done()
                .onSuccess(response -> {
                    System.out.println("=======2======>" + response.body());
                    latch.countDown();
                })
                .onFailure(t -> {
                    System.out.println(t.getMessage());
                    latch.countDown();
                }).done();
        latch.await();
    }
}
