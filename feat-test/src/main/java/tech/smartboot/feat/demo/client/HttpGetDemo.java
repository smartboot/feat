/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.client;

import tech.smartboot.feat.core.client.HttpClient;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HttpGetDemo {
    public static void main(String[] args) {
        HttpClient httpClient = new HttpClient("https://www.baidu.com");
        httpClient.options().debug(false).maxConnections(128);

        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                int j = 0;
                while (true) {
                    httpClient.get("/")
                            .header(h -> h.keepalive(true))
//                    .onSuccess(response -> System.out.println(response.body()))
                            .onFailure(Throwable::printStackTrace)
                            .submit();
                    System.out.println(j++);
                    if(j%100==0){
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }).start();
        }

    }
}