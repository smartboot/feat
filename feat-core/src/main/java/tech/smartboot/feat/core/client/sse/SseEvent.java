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

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SSE事件对象，封装从服务器接收的事件数据
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseEvent {
    
    /**
     * 事件ID
     */
    private final String id;
    
    /**
     * 事件类型
     */
    private final String type;
    
    /**
     * 事件数据
     */
    private final String data;
    
    /**
     * 重连间隔建议(毫秒)
     */
    private final Long retry;
    
    /**
     * 接收时间戳
     */
    private final LocalDateTime timestamp;
    
    /**
     * 原始事件字段映射
     */
    private final Map<String, String> rawFields;

    public SseEvent(String id, String type, String data, Long retry, Map<String, String> rawFields) {
        this.id = id;
        this.type = type;
        this.data = data;
        this.retry = retry;
        this.timestamp = LocalDateTime.now();
        this.rawFields = rawFields;
    }

    /**
     * 获取事件ID
     * 
     * @return 事件ID，可能为null
     */
    public String getId() {
        return id;
    }

    /**
     * 获取事件类型
     * 
     * @return 事件类型，可能为null（默认为message类型）
     */
    public String getType() {
        return type;
    }

    /**
     * 获取事件数据
     * 
     * @return 事件数据
     */
    public String getData() {
        return data;
    }

    /**
     * 获取重连间隔建议
     * 
     * @return 重连间隔（毫秒），可能为null
     */
    public Long getRetry() {
        return retry;
    }

    /**
     * 获取接收时间戳
     * 
     * @return 事件接收时间
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 获取原始事件字段映射
     * 
     * @return 原始字段映射
     */
    public Map<String, String> getRawFields() {
        return rawFields;
    }

    @Override
    public String toString() {
        return "SseEvent{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", data='" + data + '\'' +
                ", retry=" + retry +
                ", timestamp=" + timestamp +
                '}';
    }
}