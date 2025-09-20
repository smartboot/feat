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

import java.util.HashMap;
import java.util.Map;

/**
 * SSE客户端配置选项
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseOptions {
    
    /**
     * 连接超时(毫秒)
     */
    private long connectionTimeout = 10000;
    
    /**
     * 读取超时(毫秒)
     */
    private long readTimeout = 0; // 0表示无超时
    
    /**
     * 重连策略
     */
    private RetryPolicy retryPolicy = RetryPolicy.defaultPolicy();
    
    /**
     * 心跳配置
     */
    private HeartbeatConfig heartbeatConfig = HeartbeatConfig.disabled();
    
    /**
     * 事件过滤器
     */
    private EventFilter eventFilter = EventFilter.acceptAll();
    
    /**
     * 断点续传的最后事件ID
     */
    private String lastEventId;
    
    /**
     * 自定义请求头
     */
    private Map<String, String> headers = new HashMap<>();
    
    /**
     * 是否启用自动重连
     */
    private boolean autoReconnect = true;

    public SseOptions() {
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public SseOptions setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public SseOptions setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public SseOptions setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    public HeartbeatConfig getHeartbeatConfig() {
        return heartbeatConfig;
    }

    public SseOptions setHeartbeatConfig(HeartbeatConfig heartbeatConfig) {
        this.heartbeatConfig = heartbeatConfig;
        return this;
    }

    public EventFilter getEventFilter() {
        return eventFilter;
    }

    public SseOptions setEventFilter(EventFilter eventFilter) {
        this.eventFilter = eventFilter;
        return this;
    }

    public String getLastEventId() {
        return lastEventId;
    }

    public SseOptions setLastEventId(String lastEventId) {
        this.lastEventId = lastEventId;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public SseOptions setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }
    
    /**
     * 添加请求头
     * 
     * @param name 头名称
     * @param value 头值
     * @return this
     */
    public SseOptions addHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public SseOptions setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }
}