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

import tech.smartboot.feat.core.common.HttpMethod;

import java.util.function.Predicate;

/**
 * SSE客户端配置选项
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseOptions {
    /**
     * 接受所有事件
     */
    public static final Predicate<SseEvent> EVENT_FILTER_ACCEPT_ALL = sseEvent -> true;
    /**
     * 拒绝所有事件
     */
    public static final Predicate<SseEvent> EVENT_FILTER_REJECT_ALL = sseEvent -> false;
    private String method = HttpMethod.GET;

    /**
     * 重连策略
     */
    private RetryPolicy retryPolicy = RetryPolicy.noRetry();


    /**
     * 事件过滤器
     */
    private Predicate<SseEvent> eventFilter = EVENT_FILTER_ACCEPT_ALL;

    /**
     * 断点续传的最后事件ID
     */
    private String lastEventId;


    SseOptions() {
    }


    RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public SseOptions retryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    Predicate<SseEvent> getEventFilter() {
        return eventFilter;
    }

    public SseOptions eventFilter(Predicate<SseEvent> eventFilter) {
        this.eventFilter = eventFilter;
        return this;
    }

    String getLastEventId() {
        return lastEventId;
    }

    public SseOptions lastEventId(String lastEventId) {
        this.lastEventId = lastEventId;
        return this;
    }

    String getMethod() {
        return method;
    }

    public SseOptions setMethod(String method) {
        this.method = method;
        return this;
    }
}