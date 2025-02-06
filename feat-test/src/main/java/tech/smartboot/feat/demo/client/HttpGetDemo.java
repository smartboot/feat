/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpGetDemo.java
 * Date: 2021-06-08
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo.client;

import tech.smartboot.feat.core.client.HttpClient;

public class HttpGetDemo {
    public static void main(String[] args) {
        HttpClient httpClient = new HttpClient("https://smartboot.tech");
        httpClient.get("/feat/")
                .onSuccess(response -> System.out.println(response.body()))
                .onFailure(Throwable::printStackTrace)
                .submit();
    }
}