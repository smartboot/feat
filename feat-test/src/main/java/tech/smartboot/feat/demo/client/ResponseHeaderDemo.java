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

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
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