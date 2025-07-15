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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HttpGetDemo2 {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        HttpClient httpClient = new HttpClient("www.baidu.com", 80);
        for (int i = 0; i < 100; i++) {
            int j = i;
            executorService.submit(() -> {
                httpClient.get("/").header(h -> h.keepalive(false))
                        .onSuccess(response -> System.out.println(j))
                        .onFailure(Throwable::printStackTrace)
                        .submit();
            });
        }
        // 关闭线程池并等待任务完成
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }


    }
}