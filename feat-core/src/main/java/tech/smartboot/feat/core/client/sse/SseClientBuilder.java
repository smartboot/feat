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

import java.util.function.Consumer;

/**
 * SSE客户端构建器
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseClientBuilder {
    
    private final String url;
    private final SseOptions options;

    public SseClientBuilder(String url) {
        this.url = url;
        this.options = new SseOptions();
    }

    /**
     * 设置连接超时
     * 
     * @param timeout 超时时间(毫秒)
     * @return this
     */
    public SseClientBuilder timeout(long timeout) {
        options.setConnectionTimeout(timeout);
        return this;
    }
    
    /**
     * 设置读取超时
     * 
     * @param timeout 超时时间(毫秒)
     * @return this
     */
    public SseClientBuilder readTimeout(long timeout) {
        options.setReadTimeout(timeout);
        return this;
    }

    /**
     * 添加请求头
     * 
     * @param name 头名称
     * @param value 头值
     * @return this
     */
    public SseClientBuilder header(String name, String value) {
        options.addHeader(name, value);
        return this;
    }

    /**
     * 设置重连策略
     * 
     * @param retryPolicy 重连策略
     * @return this
     */
    public SseClientBuilder retryPolicy(RetryPolicy retryPolicy) {
        options.setRetryPolicy(retryPolicy);
        return this;
    }
    
    /**
     * 配置重连策略
     * 
     * @param configurer 重连策略配置器
     * @return this
     */
    public SseClientBuilder retryPolicy(Consumer<RetryPolicy> configurer) {
        RetryPolicy policy = new RetryPolicy();
        configurer.accept(policy);
        options.setRetryPolicy(policy);
        return this;
    }

    /**
     * 设置心跳配置
     * 
     * @param heartbeatConfig 心跳配置
     * @return this
     */
    public SseClientBuilder heartbeat(HeartbeatConfig heartbeatConfig) {
        options.setHeartbeatConfig(heartbeatConfig);
        return this;
    }
    
    /**
     * 配置心跳
     * 
     * @param configurer 心跳配置器
     * @return this
     */
    public SseClientBuilder heartbeat(Consumer<HeartbeatConfig> configurer) {
        HeartbeatConfig config = new HeartbeatConfig();
        configurer.accept(config);
        options.setHeartbeatConfig(config);
        return this;
    }

    /**
     * 设置事件过滤器
     * 
     * @param eventFilter 事件过滤器
     * @return this
     */
    public SseClientBuilder eventFilter(EventFilter eventFilter) {
        options.setEventFilter(eventFilter);
        return this;
    }

    /**
     * 设置断点续传的最后事件ID
     * 
     * @param lastEventId 最后事件ID
     * @return this
     */
    public SseClientBuilder lastEventId(String lastEventId) {
        options.setLastEventId(lastEventId);
        return this;
    }
    
    /**
     * 设置是否自动重连
     * 
     * @param autoReconnect 是否自动重连
     * @return this
     */
    public SseClientBuilder autoReconnect(boolean autoReconnect) {
        options.setAutoReconnect(autoReconnect);
        return this;
    }
    
    /**
     * 配置选项
     * 
     * @param configurer 选项配置器
     * @return this
     */
    public SseClientBuilder options(Consumer<SseOptions> configurer) {
        configurer.accept(options);
        return this;
    }

    /**
     * 构建SSE客户端实例
     * 
     * @return SSE客户端
     */
    public SseClient build() {
        return new SseClientImpl(url, options);
    }
}