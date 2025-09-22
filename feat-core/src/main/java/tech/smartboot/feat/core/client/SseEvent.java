/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.client.stream.ServerSentEventStream;

import java.util.Map;

/**
 * SSE事件对象，封装从服务器接收的事件数据
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseEvent {


    /**
     * 原始事件字段映射
     */
    private final Map<String, String> rawFields;

    public SseEvent(Map<String, String> rawFields) {
        this.rawFields = rawFields;
    }

    /**
     * 获取事件ID
     *
     * @return 事件ID，可能为null
     */
    public String getId() {
        return rawFields.get(ServerSentEventStream.ID);
    }

    /**
     * 获取事件类型
     *
     * @return 事件类型，可能为null（默认为message类型）
     */
    public String getType() {
        return rawFields.get(ServerSentEventStream.EVENT);
    }

    /**
     * 获取事件数据
     *
     * @return 事件数据
     */
    public String getData() {
        return rawFields.get(ServerSentEventStream.DATA);
    }

    /**
     * 获取重连间隔建议
     *
     * @return 重连间隔（毫秒），可能为null
     */
    public Long getRetry() {
        String retryStr = rawFields.get(ServerSentEventStream.RETRY);
        Long retry = null;
        if (retryStr != null) {
            try {
                retry = Long.parseLong(retryStr);
            } catch (NumberFormatException e) {
                // 忽略无效的重连间隔
            }
        }
        return retry;
    }

    public String getComment() {
        return rawFields.get("");
    }

    /**
     * 获取原始事件字段映射
     *
     * @return 原始字段映射
     */
//    public Map<String, String> getRawFields() {
//        return rawFields;
//    }
}