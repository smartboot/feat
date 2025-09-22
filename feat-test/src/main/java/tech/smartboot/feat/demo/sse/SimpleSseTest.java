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
import tech.smartboot.feat.core.server.upgrade.sse.SSEUpgrade;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简化版SSE服务器客户端测试
 * 使用现有的ServerSentEventStream测试SSE功能
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SimpleSseTest {

    private static final int SERVER_PORT = 8081;

    public static void main(String[] args) throws Exception {
        System.out.println("=== 简化版 SSE 测试 ===");

        // 启动SSE服务器
        startServer();

        // 等待服务器启动
        Thread.sleep(2000);

        // 启动客户端
        startClient();

        // 运行10秒后结束
        Thread.sleep(10000);

        System.out.println("\\n=== 测试完成 ===");
        System.exit(0);
    }

    /**
     * 启动SSE服务器
     */
    private static void startServer() {
        System.out.println("启动SSE服务器，端口: " + SERVER_PORT);

        Feat.httpServer().httpHandler(req -> {
            req.upgrade(new SSEUpgrade() {
                @Override
                public void onOpen(SseEmitter sseEmitter) {
                    System.out.println("[服务器] 客户端连接成功");

                    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                        AtomicInteger counter = new AtomicInteger(0);

                        @Override
                        public void run() {
                            try {
                                int count = counter.incrementAndGet();
                                String eventData = "Hello SSE Client #" + count + " - " + System.currentTimeMillis();

                                sseEmitter.send(SseEmitter.event()
                                        .name("message")
                                        .id("event-" + count)
                                        .comment("测试事件")
                                        .data(eventData));

                                System.out.println("[服务器] 发送事件 #" + count + ": " + eventData);

                                if (count >= 8) {
                                    System.out.println("[服务器] 发送完毕，关闭连接");
                                    sseEmitter.complete();
                                }
                            } catch (IOException e) {
                                System.err.println("[服务器] 发送事件失败: " + e.getMessage());
                            }
                        }
                    }, 1, 1, TimeUnit.SECONDS);
                }
            });
        }).listen(SERVER_PORT);

        System.out.println("SSE服务器已启动，监听端口: " + SERVER_PORT);
    }

    /**
     * 启动SSE客户端
     */
    private static void startClient() throws Exception {
        System.out.println("启动SSE客户端...");

        String url = "http://localhost:" + SERVER_PORT;

        AtomicInteger eventCount = new AtomicInteger(0);
        Feat.httpClient(url, opt -> opt.debug(true)).get().toSseClient().onData(event -> {
            int count = eventCount.incrementAndGet();
            System.out.println("[客户端] 接收事件 #" + count);
            System.out.println("    ID: " + event.getId());
            System.out.println("    类型: " + event.getType());
            System.out.println("    数据: " + event.getData());
            System.out.println("    注释: " + event.getComment());
            System.out.println();
        }).onOpen(sseClient -> {
            System.out.println("[客户端] 监听成功，开始接收事件...");
        }).onError(throwable -> {
            System.err.println("[客户端] 监听失败: " + throwable.getMessage());
            throwable.printStackTrace();
        }).submit();

        System.out.println("SSE客户端已启动，开始接收事件...");
    }
}