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

import java.util.ArrayList;
import java.util.List;

/**
 * A2A 任务类
 *
 * <p>表示智能体之间的任务交互单元，包含任务状态、消息历史、元数据等信息。</p>
 *
 * <p>任务是A2A协议的核心概念，用于表示从提交到完成的整个交互过程。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class Task {
    /**
     * 任务唯一标识符
     */
    private String id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 任务状态
     */
    private TaskState state;

    /**
     * 任务消息历史
     */
    private List<Message> history;

    /**
     * 任务元数据
     */
    private JSONObject metadata;

    /**
     * 任务创建时间戳（毫秒）
     */
    private long createdAt;

    /**
     * 任务最后更新时间戳（毫秒）
     */
    private long updatedAt;

    /**
     * 任务完成时间戳（毫秒）
     */
    private long completedAt;

    /**
     * 任务超时时间（毫秒）
     */
    private long timeout;

    /**
     * 父任务ID（用于任务嵌套）
     */
    private String parentId;

    /**
     * 任务标签列表
     */
    private List<String> tags;

    public Task() {
        this.history = new ArrayList<>();
        this.state = TaskState.SUBMITTED;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
        this.updatedAt = System.currentTimeMillis();
    }

    public List<Message> getHistory() {
        return history;
    }

    public void setHistory(List<Message> history) {
        this.history = history;
    }

    public JSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * 添加消息到历史记录
     *
     * @param message 消息
     * @return 当前Task实例（链式调用）
     */
    public Task addMessage(Message message) {
        if (this.history == null) {
            this.history = new ArrayList<>();
        }
        this.history.add(message);
        this.updatedAt = System.currentTimeMillis();
        return this;
    }

    /**
     * 添加标签
     *
     * @param tag 标签
     * @return 当前Task实例（链式调用）
     */
    public Task addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
        return this;
    }

    /**
     * 标记任务为完成状态
     */
    public void markCompleted() {
        this.state = TaskState.COMPLETED;
        this.completedAt = System.currentTimeMillis();
        this.updatedAt = this.completedAt;
    }

    /**
     * 标记任务为失败状态
     */
    public void markFailed() {
        this.state = TaskState.FAILED;
        this.completedAt = System.currentTimeMillis();
        this.updatedAt = this.completedAt;
    }

    /**
     * 标记任务为取消状态
     */
    public void markCanceled() {
        this.state = TaskState.CANCELED;
        this.completedAt = System.currentTimeMillis();
        this.updatedAt = this.completedAt;
    }

    /**
     * 检查任务是否已完成
     *
     * @return 如果任务已完成则返回true
     */
    public boolean isCompleted() {
        return state != null && state.isFinal();
    }
}
