package tech.smartboot.feat.demo.client;

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.HttpMethod;

import java.io.IOException;

public class HttpRestDemo {
    public static void main(String[] args) throws IOException {
        // 创建 HttpClient 实例
        HttpClient client = new HttpClient("https://smartboot.tech");
        client.options().debug(true);
        // 发送 GET 请求
        client.rest(HttpMethod.GET, "/feat/").onSuccess(response -> {
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Body: " + response.body());
        }).submit();
    }
}
