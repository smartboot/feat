/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.sse;

import tech.smartboot.feat.Feat;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用外部SSE服务测试Feat的SSE客户端
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ExternalSseTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 使用外部SSE服务测试Feat客户端 ===");

        // 测试HTTPSse demo 
        testHttpsSseDemo();

        // 等待30秒接收事件
        Thread.sleep(30000);

        System.out.println("\\n=== 测试完成 ===");
    }

    /**
     * 测试公开的SSE demo服务
     */
    private static void testHttpsSseDemo() throws Exception {
        System.out.println("连接到SSE Demo服务...");

        AtomicInteger eventCount = new AtomicInteger(0);

        // 使用公开的SSE测试服务
        String url = "https://sse.dev/test";

        Feat.httpClient(url, opt -> opt.debug(true)).get().onSSE(sse -> sse.onData(event -> {
            int count = eventCount.incrementAndGet();

            System.out.println("\\n[事件 #" + count + "]");
            System.out.println("  ID: " + event.getId());
            System.out.println("  类型: " + event.getType());
            System.out.println("  数据: " + event.getData());
            System.out.println("  重试: " + event.getRetry());
            System.out.println("  时间: " + System.currentTimeMillis());

            if (count >= 10) {
                System.out.println("\\n已接收10个事件，测试结束");
                System.exit(0);
            }
        })).onFailure(error -> {
            System.err.println("❌ 发生错误: " + error.getMessage());
            error.printStackTrace();
        }).submit();

    }
}