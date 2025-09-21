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
import tech.smartboot.feat.core.client.sse.SseClient;
import tech.smartboot.feat.core.client.sse.ConnectionListener;
import tech.smartboot.feat.core.client.sse.ConnectionState;
import tech.smartboot.feat.core.client.sse.RetryPolicy;
import tech.smartboot.feat.core.server.upgrade.sse.SSEUpgrade;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用Feat SSE服务器测试SSE客户端
 * 演示完整的SSE服务器-客户端交互流程
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseServerClientTest {

    private static final int SERVER_PORT = 8080;
    private static final String SERVER_URL = "http://localhost:" + SERVER_PORT;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Feat SSE 服务器-客户端测试 ===");

        // 启动SSE服务器
        startSseServer();

        // 等待服务器启动
        Thread.sleep(2000);

        // 运行不同的测试场景
        testBasicSseClient();
        Thread.sleep(3000);

        testAdvancedSseClient();
        Thread.sleep(3000);

        testMultipleClients();
        Thread.sleep(3000);

        testReconnection();

        System.out.println("\\n=== 所有测试完成 ===");
    }

    /**
     * 启动SSE服务器
     */
    private static void startSseServer() {
        System.out.println("启动SSE服务器，端口: " + SERVER_PORT);

        Feat.httpServer(serverOptions -> serverOptions.debug(false)).httpHandler(req -> {
            String path = req.getRequestURI();

            if (path.equals("/events")) {
                // 基础事件流
                req.upgrade(new SSEUpgrade() {
                    @Override
                    public void onOpen(SseEmitter sseEmitter) {
                        sendBasicEvents(sseEmitter);
                    }
                });
            } else if (path.equals("/advanced-events")) {
                // 高级事件流（多种事件类型）
                req.upgrade(new SSEUpgrade() {
                    @Override
                    public void onOpen(SseEmitter sseEmitter) {
                        sendAdvancedEvents(sseEmitter);
                    }
                });
            } else if (path.equals("/notification-events")) {
                // 通知事件流
                req.upgrade(new SSEUpgrade() {
                    @Override
                    public void onOpen(SseEmitter sseEmitter) {
                        sendNotificationEvents(sseEmitter);
                    }
                });
            } else if (path.equals("/unstable-events")) {
                // 不稳定连接（用于测试重连）
                req.upgrade(new SSEUpgrade() {
                    @Override
                    public void onOpen(SseEmitter sseEmitter) {
                        sendUnstableEvents(sseEmitter);
                    }
                });
            }
        }).listen(SERVER_PORT);

        System.out.println("SSE服务器已启动，监听端口: " + SERVER_PORT);
    }

    /**
     * 发送基础事件
     */
    private static void sendBasicEvents(SseEmitter sseEmitter) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                long timestamp = System.currentTimeMillis();
                String data = "当前时间: " + timestamp;

                sseEmitter.send(SseEmitter.event().id("event-" + timestamp).name("data").data(data));

            } catch (IOException e) {
                System.err.println("发送基础事件失败: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * 发送高级事件（多种类型）
     */
    private static void sendAdvancedEvents(SseEmitter sseEmitter) {
        AtomicInteger counter = new AtomicInteger(0);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                int count = counter.incrementAndGet();
                String eventType;
                String data;

                if (count % 3 == 0) {
                    eventType = "notification";
                    data = "这是第 " + count + " 个通知事件";
                } else if (count % 3 == 1) {
                    eventType = "update";
                    data = "系统更新信息 #" + count;
                } else {
                    eventType = "status";
                    data = "状态检查 - 一切正常 (" + count + ")";
                }

                sseEmitter.send(SseEmitter.event().id("advanced-" + count).name(eventType).data(data).reconnectTime(3000));

                // 发送10个事件后结束
                if (count >= 10) {
                    sseEmitter.complete();
                }

            } catch (IOException e) {
                System.err.println("发送高级事件失败: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 发送通知事件
     */
    private static void sendNotificationEvents(SseEmitter sseEmitter) {
        AtomicInteger counter = new AtomicInteger(0);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                int count = counter.incrementAndGet();

                sseEmitter.send(SseEmitter.event().id("notification-" + count).name("notification").comment("重要通知").data("您有新的消息 #" + count));

            } catch (IOException e) {
                System.err.println("发送通知事件失败: " + e.getMessage());
            }
        }, 0, 1500, TimeUnit.MILLISECONDS);
    }

    /**
     * 发送不稳定事件（模拟网络问题）
     */
    private static void sendUnstableEvents(SseEmitter sseEmitter) {
        AtomicInteger counter = new AtomicInteger(0);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                int count = counter.incrementAndGet();

                // 模拟在第3个事件后连接中断
                if (count == 3) {
                    System.out.println("[服务器] 模拟连接中断...");
                    sseEmitter.complete();
                    return;
                }

                sseEmitter.send(SseEmitter.event().id("unstable-" + count).name("data").data("不稳定连接事件 #" + count));

            } catch (IOException e) {
                System.err.println("发送不稳定事件失败: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 测试基础SSE客户端
     */
    private static void testBasicSseClient() throws Exception {
        System.out.println("\\n--- 测试基础SSE客户端 ---");

        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3); // 等待接收3个事件

        SseClient client = Feat.sse(SERVER_URL + "/events", opt -> opt.httpOptions().connectTimeout(5000));

        client.onData(event -> {
            int count = eventCount.incrementAndGet();
            System.out.println("[基础客户端] 接收事件 #" + count + ": " + event.getData());
            System.out.println("    事件ID: " + event.getId());
            System.out.println("    事件类型: " + event.getType());
            latch.countDown();
        });

        client.onError(error -> {
            System.err.println("[基础客户端] 错误: " + error.getMessage());
        });

        client.onConnection(new ConnectionListener() {
            @Override
            public void onOpen(SseClient client) {
                System.out.println("[基础客户端] 连接已建立");
            }

            @Override
            public void onClose(SseClient client, String reason) {
                System.out.println("[基础客户端] 连接已关闭: " + reason);
            }
        });

        client.connect();

        // 等待接收事件
        latch.await(10, TimeUnit.SECONDS);

        client.disconnect();
        System.out.println("[基础客户端] 测试完成，共接收 " + eventCount.get() + " 个事件");
    }

    /**
     * 测试高级SSE客户端
     */
    private static void testAdvancedSseClient() throws Exception {
        System.out.println("\\n--- 测试高级SSE客户端 ---");

        AtomicInteger notificationCount = new AtomicInteger(0);
        AtomicInteger updateCount = new AtomicInteger(0);
        AtomicInteger statusCount = new AtomicInteger(0);

        SseClient client = Feat.sse(SERVER_URL + "/advanced-events", options -> {
            options.retryPolicy(RetryPolicy.defaultPolicy()).httpOptions().connectTimeout(5000);
        });

        // 注册不同类型的事件处理器
        client.onEvent("notification", event -> {
            int count = notificationCount.incrementAndGet();
            System.out.println("[高级客户端] 通知事件 #" + count + ": " + event.getData());
        });

        client.onEvent("update", event -> {
            int count = updateCount.incrementAndGet();
            System.out.println("[高级客户端] 更新事件 #" + count + ": " + event.getData());
        });

        client.onEvent("status", event -> {
            int count = statusCount.incrementAndGet();
            System.out.println("[高级客户端] 状态事件 #" + count + ": " + event.getData());
        });

        client.onConnection(new ConnectionListener() {
            @Override
            public void onOpen(SseClient client) {
                System.out.println("[高级客户端] 连接已建立");
            }

            @Override
            public void onClose(SseClient client, String reason) {
                System.out.println("[高级客户端] 连接已关闭: " + reason);
                System.out.println("[高级客户端] 事件统计 - 通知:" + notificationCount.get() + ", 更新:" + updateCount.get() + ", 状态:" + statusCount.get());
            }
        });

        client.connect();

        // 等待接收所有事件
        Thread.sleep(12000);

        client.disconnect();
    }

    /**
     * 测试多个客户端
     */
    private static void testMultipleClients() throws Exception {
        System.out.println("\\n--- 测试多个客户端 ---");

        // 创建3个客户端连接不同的端点
        SseClient client1 = createTestClient("客户端1", SERVER_URL + "/events");
        SseClient client2 = createTestClient("客户端2", SERVER_URL + "/notification-events");
        SseClient client3 = createTestClient("客户端3", SERVER_URL + "/advanced-events");

        // 同时启动所有客户端
        CountDownLatch allConnected = new CountDownLatch(3);

        client1.onConnection(new ConnectionListener() {
            @Override
            public void onOpen(SseClient client) {
                System.out.println("[多客户端] 客户端1连接成功");
                allConnected.countDown();
            }
        });

        client2.onConnection(new ConnectionListener() {
            @Override
            public void onOpen(SseClient client) {
                System.out.println("[多客户端] 客户端2连接成功");
                allConnected.countDown();
            }
        });

        client3.onConnection(new ConnectionListener() {
            @Override
            public void onOpen(SseClient client) {
                System.out.println("[多客户端] 客户端3连接成功");
                allConnected.countDown();
            }
        });

        client1.connect();
        client2.connect();
        client3.connect();

        // 等待所有客户端连接
        allConnected.await(10, TimeUnit.SECONDS);
        System.out.println("[多客户端] 所有客户端已连接，开始接收事件...");

        // 运行5秒
        Thread.sleep(5000);

        // 关闭所有客户端
        client1.disconnect();
        client2.disconnect();
        client3.disconnect();

        System.out.println("[多客户端] 所有客户端已断开");
    }

    /**
     * 测试重连功能
     */
    private static void testReconnection() throws Exception {
        System.out.println("\\n--- 测试重连功能 ---");

        // 配置积极的重连策略
        RetryPolicy retryPolicy = new RetryPolicy().setMaxRetries(3).setInitialDelay(1000).setBackoffMultiplier(1.5);

        SseClient client = Feat.sse(SERVER_URL + "/unstable-events", opt -> opt.retryPolicy(retryPolicy));

        AtomicInteger eventCount = new AtomicInteger(0);

        client.onData(event -> {
            int count = eventCount.incrementAndGet();
            System.out.println("[重连测试] 接收事件 #" + count + ": " + event.getData());
        });

        client.onConnection(new ConnectionListener() {
            @Override
            public void onOpen(SseClient client) {
                System.out.println("[重连测试] 连接已建立");
            }

            @Override
            public void onClose(SseClient client, String reason) {
                System.out.println("[重连测试] 连接已关闭: " + reason);
            }

            @Override
            public void onError(SseClient client, Throwable error) {
                System.out.println("[重连测试] 连接错误，准备重连: " + error.getMessage());
            }

            @Override
            public void onStateChange(SseClient client, ConnectionState oldState, ConnectionState newState) {
                System.out.println("[重连测试] 状态变化: " + oldState + " -> " + newState);
            }
        });

        client.connect();

        // 等待连接中断和重连尝试
        Thread.sleep(8000);

        client.disconnect();
        System.out.println("[重连测试] 测试完成，共接收 " + eventCount.get() + " 个事件");
    }

    /**
     * 创建测试客户端
     */
    private static SseClient createTestClient(String name, String url) {
        SseClient client = Feat.sse(url, opt -> opt.retryPolicy(RetryPolicy.defaultPolicy()).httpOptions().connectTimeout(5000));

        client.onData(event -> {
            System.out.println("[" + name + "] 接收: " + event.getData());
        });

        client.onError(error -> {
            System.err.println("[" + name + "] 错误: " + error.getMessage());
        });

        return client;
    }
}