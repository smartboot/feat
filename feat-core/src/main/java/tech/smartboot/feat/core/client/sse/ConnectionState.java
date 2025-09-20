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
 * SSE连接状态枚举
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public enum ConnectionState {
    
    /**
     * 未连接状态
     */
    DISCONNECTED("DISCONNECTED"),
    
    /**
     * 连接建立中
     */
    CONNECTING("CONNECTING"),
    
    /**
     * 已连接状态
     */
    CONNECTED("CONNECTED"),
    
    /**
     * 重连中
     */
    RECONNECTING("RECONNECTING"),
    
    /**
     * 连接失败
     */
    FAILED("FAILED");
    
    private final String state;
    
    ConnectionState(String state) {
        this.state = state;
    }
    
    public String getState() {
        return state;
    }
    
    @Override
    public String toString() {
        return state;
    }
}