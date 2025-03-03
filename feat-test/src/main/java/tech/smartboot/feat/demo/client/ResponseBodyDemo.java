/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.client;

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.stream.Stream;

import java.io.IOException;

public class ResponseBodyDemo {
    public static void main(String[] args) {
        HttpClient httpClient = new HttpClient("https://smartboot.tech");
        httpClient.get("/feat/")
                .onResponseBody(new Stream() {
                    @Override
                    public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
                        System.out.println("接收到数据：" + new String(bytes));
                        if (end) {
                            System.out.println("数据接收完毕");
                        }
                    }
                })
                .submit();
    }
}