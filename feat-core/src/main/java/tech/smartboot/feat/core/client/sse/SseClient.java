/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.sse;

import java.util.concurrent.CompletableFuture;

/**
 * SSE客户端接口
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface SseClient {
    
    /**
     * 建立SSE连接
     * 
     * @return 连接Future
     */
    CompletableFuture<Void> connect();
    
    /**
     * 断开SSE连接
     * 
     * @return 断开Future
     */
    CompletableFuture<Void> disconnect();
    
    /**
     * 注册特定类型事件的处理器
     * 
     * @param eventType 事件类型
     * @param handler 事件处理器
     * @return this
     */
    SseClient onEvent(String eventType, EventHandler handler);
    
    /**
     * 注册数据事件处理器（默认事件类型）
     * 
     * @param handler 事件处理器
     * @return this
     */
    SseClient onData(EventHandler handler);
    
    /**
     * 注册错误处理器
     * 
     * @param errorHandler 错误处理器
     * @return this
     */
    SseClient onError(ErrorHandler errorHandler);
    
    /**
     * 注册连接监听器
     * 
     * @param listener 连接监听器
     * @return this
     */
    SseClient onConnection(ConnectionListener listener);
    
    /**
     * 检查连接状态
     * 
     * @return 是否已连接
     */
    boolean isConnected();
    
    /**
     * 获取当前连接状态
     * 
     * @return 连接状态
     */
    ConnectionState getConnectionState();
    
    /**
     * 获取最后接收的事件ID
     * 
     * @return 最后事件ID，可能为null
     */
    String getLastEventId();
    
    /**
     * 获取连接URL
     * 
     * @return 连接URL
     */
    String getUrl();
    
    /**
     * 获取配置选项
     * 
     * @return 配置选项
     */
    SseOptions getOptions();
    
    /**
     * 错误处理器接口
     */
    @FunctionalInterface
    interface ErrorHandler {
        /**
         * 处理错误
         * 
         * @param error 错误信息
         */
        void onError(Throwable error);
    }
}