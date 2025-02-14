package tech.smartboot.feat.test.client;

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.router.Router;

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
        HttpServer bootstrap = new HttpServer();
        Router route = new Router();
        byte[] body = new byte[4096];
        route.route("/other/**", new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws IOException {
                HttpResponse response=request.getResponse();
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
                response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValue.Connection.KEEPALIVE);
                response.write("success".getBytes());
            }
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
                .body(b->b.write(bytes))
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
                .body(b->b.write(bytes))
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
