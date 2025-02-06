/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpGetDemo.java
 * Date: 2021-06-08
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

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