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
import tech.smartboot.feat.core.client.sse.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SSE客户端高级特性演示
 * 演示重连机制、断点续传、并发连接等高级功能
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseClientAdvancedDemo {
    
    public static void main(String[] args) throws Exception {
        // 示例1：自动重连机制演示
        reconnectionDemo();
        
        Thread.sleep(3000);
        
        // 示例2：断点续传演示
        resumeFromLastEventDemo();
        
        Thread.sleep(3000);
        
        // 示例3：多连接管理演示
        multipleConnectionsDemo();
        
        Thread.sleep(3000);
        
        // 示例4：事件统计和监控
        eventStatisticsDemo();
    }
    
    /**
     * 示例1：自动重连机制演示
     */
    public static void reconnectionDemo() throws Exception {
        System.out.println("=== 自动重连机制演示 ===");
        
        // 配置积极的重连策略
        RetryPolicy aggressiveRetry = new RetryPolicy()
                .setMaxRetries(5)
                .setInitialDelay(1000)
                .setMaxDelay(10000)
                .setBackoffMultiplier(1.5)
                .setRetryOnError(true);
        
        SseClient client = Feat.sse("http://unreliable-server.com/events")
                .retryPolicy(aggressiveRetry)
                .autoReconnect(true)
                .build();
        
        // 监听连接状态变化
        client.onConnection(new ConnectionListener() {
            @Override
            public void onStateChange(SseClient client, ConnectionState oldState, ConnectionState newState) {
                System.out.println("[重连演示] 状态变化: " + oldState + " -> " + newState);
            }
            
            @Override
            public void onError(SseClient client, Throwable error) {
                System.out.println("[重连演示] 连接错误，将自动重连: " + error.getMessage());
            }
        });
        
        client.onData(event -> {
            System.out.println("[重连演示] 接收事件: " + event.getData());
        });
        
        client.connect().exceptionally(throwable -> {
            System.out.println("[重连演示] 初始连接失败，重连机制将启动");
            return null;
        });
        
        Thread.sleep(8000);
        client.disconnect();
    }
    
    /**
     * 示例2：断点续传演示
     */
    public static void resumeFromLastEventDemo() throws Exception {
        System.out.println("\\n=== 断点续传演示 ===");
        
        // 模拟第一次连接，接收一些事件后中断
        String lastEventId = simulateInitialConnection();
        
        Thread.sleep(1000);
        
        // 使用最后的事件ID重新连接，实现断点续传
        resumeConnection(lastEventId);
    }
    
    private static String simulateInitialConnection() throws Exception {
        System.out.println("[断点续传] 模拟初始连接...");
        
        AtomicInteger eventCount = new AtomicInteger(0);
        String[] lastId = {null};
        
        SseClient client = Feat.sse("http://example.com/events")
                .build();
        
        client.onData(event -> {
            int count = eventCount.incrementAndGet();
            lastId[0] = event.getId();
            System.out.println("[断点续传] 接收事件 #" + count + ", ID: " + event.getId());
            
            // 模拟在接收到3个事件后连接中断
            if (count >= 3) {
                System.out.println("[断点续传] 模拟连接中断...");
                client.disconnect();
            }
        });
        
        client.connect();
        Thread.sleep(5000); // 等待接收事件
        
        return lastId[0];
    }
    
    private static void resumeConnection(String lastEventId) throws Exception {
        System.out.println("[断点续传] 使用最后事件ID重新连接: " + lastEventId);
        
        SseClient client = Feat.sse("http://example.com/events")
                .lastEventId(lastEventId)
                .build();
        
        client.onData(event -> {
            System.out.println("[断点续传] 续传事件, ID: " + event.getId() + ", 数据: " + event.getData());
        });
        
        client.connect().thenRun(() -> {
            System.out.println("[断点续传] 断点续传连接已建立");
        });
        
        Thread.sleep(3000);
        client.disconnect();
    }
    
    /**
     * 示例3：多连接管理演示
     */
    public static void multipleConnectionsDemo() throws Exception {
        System.out.println("\\n=== 多连接管理演示 ===");
        
        // 创建多个连接到不同的端点
        SseClient notificationClient = createClientForEndpoint("notifications", "http://api.example.com/notifications");
        SseClient updatesClient = createClientForEndpoint("updates", "http://api.example.com/updates");
        SseClient alertsClient = createClientForEndpoint("alerts", "http://api.example.com/alerts");
        
        // 启动所有连接
        CountDownLatch allConnected = new CountDownLatch(3);
        
        notificationClient.connect().thenRun(allConnected::countDown);
        updatesClient.connect().thenRun(allConnected::countDown);
        alertsClient.connect().thenRun(allConnected::countDown);
        
        // 等待所有连接建立
        if (allConnected.await(10, TimeUnit.SECONDS)) {
            System.out.println("[多连接] 所有连接已建立");
        } else {
            System.out.println("[多连接] 部分连接建立超时");
        }
        
        // 模拟运行一段时间
        Thread.sleep(5000);
        
        // 关闭所有连接
        notificationClient.disconnect();
        updatesClient.disconnect();
        alertsClient.disconnect();
        
        System.out.println("[多连接] 所有连接已关闭");
    }
    
    private static SseClient createClientForEndpoint(String name, String url) {
        SseClient client = Feat.sse(url)
                .timeout(8000)
                .autoReconnect(true)
                .build();
        
        client.onData(event -> {
            System.out.println("[" + name + "] 接收事件: " + event.getData());
        });
        
        client.onConnection(new ConnectionListener() {
            @Override
            public void onOpen(SseClient client) {
                System.out.println("[" + name + "] 连接已打开");
            }
            
            @Override
            public void onClose(SseClient client, String reason) {
                System.out.println("[" + name + "] 连接已关闭: " + reason);
            }
        });
        
        return client;
    }
    
    /**
     * 示例4：事件统计和监控
     */
    public static void eventStatisticsDemo() throws Exception {
        System.out.println("\\n=== 事件统计和监控演示 ===");
        
        EventStatistics stats = new EventStatistics();
        
        SseClient client = Feat.sse("http://example.com/events")
                .build();
        
        // 注册统计事件处理器
        client.onData(event -> {
            stats.recordEvent(event);
            
            // 每10个事件打印一次统计信息
            if (stats.getTotalEvents() % 10 == 0) {
                stats.printStatistics();
            }
        });
        
        client.onError(error -> {
            stats.recordError(error);
        });
        
        client.connect().thenRun(() -> {
            System.out.println("[统计监控] 开始事件监控...");
        });
        
        Thread.sleep(10000);
        
        // 打印最终统计信息
        stats.printFinalStatistics();
        
        client.disconnect();
    }
    
    /**
     * 事件统计类
     */
    static class EventStatistics {
        private final AtomicInteger totalEvents = new AtomicInteger(0);
        private final AtomicInteger errorCount = new AtomicInteger(0);
        private final long startTime = System.currentTimeMillis();
        private volatile long lastEventTime = startTime;
        
        public void recordEvent(SseEvent event) {
            totalEvents.incrementAndGet();
            lastEventTime = System.currentTimeMillis();
        }
        
        public void recordError(Throwable error) {
            errorCount.incrementAndGet();
        }
        
        public int getTotalEvents() {
            return totalEvents.get();
        }
        
        public void printStatistics() {
            long currentTime = System.currentTimeMillis();
            long uptime = currentTime - startTime;
            double eventsPerSecond = totalEvents.get() / (uptime / 1000.0);
            
            System.out.println("[统计] 事件总数: " + totalEvents.get() + 
                             ", 错误数: " + errorCount.get() + 
                             ", 运行时间: " + uptime + "ms" +
                             ", 事件频率: " + String.format("%.2f", eventsPerSecond) + " events/sec");
        }
        
        public void printFinalStatistics() {
            System.out.println("\\n=== 最终统计信息 ===");
            printStatistics();
            System.out.println("最后事件时间: " + (lastEventTime - startTime) + "ms前");
        }
    }
}