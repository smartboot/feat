package tech.smartboot.feat.demo.client;

import tech.smartboot.feat.core.client.HttpClient;

import java.io.IOException;

public class HttpClientDemo {
    public static void main(String[] args) throws IOException {
        // 创建 HttpClient 实例
        HttpClient client = new HttpClient("https://www.baidu.com");

        // 发送 GET 请求
        client.get().onSuccess(response -> {
            // 处理响应
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Body: " + response.body());
        }).done();
    }
}
