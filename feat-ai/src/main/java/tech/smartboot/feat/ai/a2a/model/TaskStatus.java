/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.model;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.a2a.enums.TaskState;

/**
 * A2A 任务状态更新类
 *
 * <p>用于表示任务状态的变化，包含新的状态和可选的消息。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class TaskStatus {
    /**
     * 任务状态
     */
    private TaskState state;

    /**
     * 状态消息
     */
    private Message message;

    /**
     * 状态更新时间戳
     */
    private long timestamp;

    /**
     * 状态元数据
     */
    private JSONObject metadata;

    public TaskStatus() {
        this.timestamp = System.currentTimeMillis();
    }

    public TaskStatus(TaskState state) {
        this.state = state;
        this.timestamp = System.currentTimeMillis();
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public JSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }
}
