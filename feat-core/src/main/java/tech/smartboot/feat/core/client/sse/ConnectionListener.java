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

/**
 * SSE连接监听器接口
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface ConnectionListener {
    
    /**
     * 连接打开时触发
     * 
     * @param client SSE客户端实例
     */
    default void onOpen(SseClient client) {
        // 默认空实现
    }
    
    /**
     * 连接关闭时触发
     * 
     * @param client SSE客户端实例
     * @param reason 关闭原因
     */
    default void onClose(SseClient client, String reason) {
        // 默认空实现
    }
    
    /**
     * 连接发生错误时触发
     * 
     * @param client SSE客户端实例
     * @param error 错误信息
     */
    default void onError(SseClient client, Throwable error) {
        // 默认空实现
    }
    
    /**
     * 连接状态变化时触发
     * 
     * @param client SSE客户端实例
     * @param oldState 旧状态
     * @param newState 新状态
     */
    default void onStateChange(SseClient client, ConnectionState oldState, ConnectionState newState) {
        // 默认空实现
    }
}