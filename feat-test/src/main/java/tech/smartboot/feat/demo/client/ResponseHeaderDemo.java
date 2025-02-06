/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpGetDemo.java
 * Date: 2021-06-08
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo.client;

import tech.smartboot.feat.core.client.HttpClient;

public class ResponseHeaderDemo {
    public static void main(String[] args) {
        HttpClient httpClient = new HttpClient("https://smartboot.tech");
        httpClient.get("/feat/")
                .onResponseHeader(response -> {
                    for (String name : response.getHeaderNames()) {
                        System.out.println(name + ": " + response.getHeader(name));
                    }
                })
                .submit();
    }
}