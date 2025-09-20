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

/**
 * SSE客户端基础使用示例
 * 演示如何使用Feat框架的SSE客户端接收服务器推送的事件
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseClientBasicDemo {
    
    public static void main(String[] args) throws Exception {
        // 示例1：简单的SSE客户端使用
        basicSseClientUsage();
        
        Thread.sleep(2000);
        
        // 示例2：带配置的SSE客户端使用
        configuredSseClientUsage();
        
        Thread.sleep(2000);
        
        // 示例3：事件过滤和处理
        eventFilteringUsage();
        
        Thread.sleep(2000);
        
        // 示例4：连接状态监听
        connectionStateMonitoring();
    }
    
    /**
     * 示例1：基础SSE客户端使用
     */
    public static void basicSseClientUsage() throws Exception {
        System.out.println("=== 基础SSE客户端使用示例 ===");
        
        // 使用Feat工厂方法创建SSE客户端
        SseClient client = Feat.sse("http://example.com/events")
                .timeout(10000)
                .autoReconnect(true)
                .build();
        
        // 注册事件处理器
        client.onData(event -> {
            System.out.println("接收到数据事件: " + event.getData());
            System.out.println("事件ID: " + event.getId());
            System.out.println("事件类型: " + event.getType());
            System.out.println("接收时间: " + event.getTimestamp());
        });
        
        // 注册错误处理器
        client.onError(error -> {
            System.err.println("连接发生错误: " + error.getMessage());
        });
        
        // 建立连接
        client.connect().thenRun(() -> {
            System.out.println("SSE连接已建立");
        }).exceptionally(throwable -> {
            System.err.println("连接失败: " + throwable.getMessage());
            return null;
        });
        
        // 等待一段时间接收事件
        Thread.sleep(5000);
        
        // 断开连接
        client.disconnect().thenRun(() -> {
            System.out.println("SSE连接已断开");
        });
    }
    
    /**
     * 示例2：带高级配置的SSE客户端使用
     */
    public static void configuredSseClientUsage() throws Exception {
        System.out.println("\\n=== 高级配置SSE客户端示例 ===");
        
        // 创建自定义重连策略
        RetryPolicy retryPolicy = new RetryPolicy()
                .setMaxRetries(10)
                .setInitialDelay(2000)
                .setBackoffMultiplier(2.0)
                .setRetryOnError(true);
        
        // 创建心跳配置
        HeartbeatConfig heartbeat = HeartbeatConfig.enabled()
                .setInterval(30000)
                .setTimeout(5000)
                .setMaxMissed(3);
        
        // 使用函数式配置创建客户端
        SseClient client = Feat.sse("http://example.com/events", options -> {
            options.setConnectionTimeout(15000)
                   .setRetryPolicy(retryPolicy)
                   .setHeartbeatConfig(heartbeat)
                   .addHeader("Authorization", "Bearer your-token")
                   .addHeader("User-Agent", "Feat-SSE-Client/1.0")
                   .setLastEventId("last-received-event-id");
        });
        
        // 注册多种类型的事件处理器
        client.onEvent("notification", event -> {
            System.out.println("收到通知事件: " + event.getData());
        });
        
        client.onEvent("update", event -> {
            System.out.println("收到更新事件: " + event.getData());
        });
        
        client.onEvent("error", event -> {
            System.out.println("收到错误事件: " + event.getData());
        });
        
        // 建立连接并处理
        CountDownLatch latch = new CountDownLatch(1);
        
        client.connect().thenRun(() -> {
            System.out.println("高级配置SSE连接已建立");
            latch.countDown();
        });
        
        // 等待连接建立
        latch.await(10, TimeUnit.SECONDS);
        
        // 检查连接状态
        System.out.println("连接状态: " + client.getConnectionState());
        System.out.println("是否已连接: " + client.isConnected());
        
        Thread.sleep(3000);
        
        client.disconnect();
    }
    
    /**
     * 示例3：事件过滤和处理
     */
    public static void eventFilteringUsage() throws Exception {
        System.out.println("\\n=== 事件过滤处理示例 ===");
        
        // 创建只接受特定类型事件的过滤器
        EventFilter notificationFilter = EventFilter.byType("notification");
        
        SseClient client = Feat.sse("http://example.com/events")
                .eventFilter(notificationFilter)
                .build();
        
        // 注册事件处理器
        client.onData(event -> {
            System.out.println("过滤后的通知事件: " + event.getData());
        });
        
        client.connect().thenRun(() -> {
            System.out.println("事件过滤SSE连接已建立");
        });
        
        Thread.sleep(3000);
        
        client.disconnect();
    }
    
    /**
     * 示例4：连接状态监听
     */
    public static void connectionStateMonitoring() throws Exception {
        System.out.println("\\n=== 连接状态监听示例 ===");
        
        SseClient client = Feat.sse("http://example.com/events")
                .autoReconnect(true)
                .build();
        
        // 注册连接监听器
        client.onConnection(new ConnectionListener() {
            @Override
            public void onOpen(SseClient client) {
                System.out.println("连接已打开: " + client.getUrl());
            }
            
            @Override
            public void onClose(SseClient client, String reason) {
                System.out.println("连接已关闭, 原因: " + reason);
            }
            
            @Override
            public void onError(SseClient client, Throwable error) {
                System.out.println("连接错误: " + error.getMessage());
            }
            
            @Override
            public void onStateChange(SseClient client, ConnectionState oldState, ConnectionState newState) {
                System.out.println("连接状态变化: " + oldState + " -> " + newState);
            }
        });
        
        // 注册事件处理器
        client.onData(event -> {
            System.out.println("监控模式下接收事件: " + event.getData());
        });
        
        client.connect().thenRun(() -> {
            System.out.println("监控模式SSE连接已建立");
        });
        
        Thread.sleep(5000);
        
        client.disconnect();
        
        System.out.println("\\n=== 所有示例执行完毕 ===");
    }
}