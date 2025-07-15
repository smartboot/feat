/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.server.upgrade.sse;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface SseEventBuilder {

    /**
     * Add an SSE "id" line.
     */
    SseEventBuilder id(String id);

    /**
     * Add an SSE "event" line.
     */
    SseEventBuilder name(String eventName);

    /**
     * Add an SSE "retry" line.
     */
    SseEventBuilder reconnectTime(long reconnectTimeMillis);

    /**
     * Add an SSE "comment" line.
     */
    SseEventBuilder comment(String comment);

    /**
     * Add an SSE "data" line.
     */
    SseEventBuilder data(String object);

    String build();

}