/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 记忆消息实体 - 表示一条存储在记忆中的交互记录
 * <p>
 * 记忆消息可以表示用户输入、AI回复、工具调用结果、系统提示等多种类型。
 * 每条消息包含内容、角色类型、时间戳和可选的元数据信息。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see MemoryRole 消息角色类型
 */
public class MemoryMessage {

    /**
     * 消息唯一标识符
     */
    private String id;

    /**
     * 会话ID - 用于区分不同会话的记忆
     */
    private String sessionId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息角色类型
     */
    private MemoryRole role;

    /**
     * 消息创建时间戳（毫秒）
     */
    private long timestamp;

    /**
     * 消息元数据 - 可存储额外的上下文信息
     */
    private Map<String, String> metadata;

    /**
     * 向量嵌入表示 - 用于语义检索
     */
    private float[] embedding;

    /**
     * 重要性评分 - 用于决定记忆的保留策略
     */
    private double importance;

    /**
     * 构造方法，使用工厂方法创建实例
     */
    public MemoryMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.importance = 1.0;
        this.metadata = new HashMap<>();
    }

    /**
     * 创建用户消息
     *
     * @param content 消息内容
     * @return MemoryMessage实例
     */
    public static MemoryMessage ofUser(String content) {
        MemoryMessage message = new MemoryMessage();
        message.content = content;
        message.role = MemoryRole.USER;
        return message;
    }

    /**
     * 创建AI助手消息
     *
     * @param content 消息内容
     * @return MemoryMessage实例
     */
    public static MemoryMessage ofAssistant(String content) {
        MemoryMessage message = new MemoryMessage();
        message.content = content;
        message.role = MemoryRole.ASSISTANT;
        return message;
    }

    /**
     * 创建系统消息
     *
     * @param content 消息内容
     * @return MemoryMessage实例
     */
    public static MemoryMessage ofSystem(String content) {
        MemoryMessage message = new MemoryMessage();
        message.content = content;
        message.role = MemoryRole.SYSTEM;
        return message;
    }

    /**
     * 创建工具调用结果消息
     *
     * @param toolName   工具名称
     * @param toolInput  工具输入
     * @param toolOutput 工具输出结果
     * @return MemoryMessage实例
     */
    public static MemoryMessage ofTool(String toolName, String toolInput, String toolOutput) {
        MemoryMessage message = new MemoryMessage();
        message.content = String.format("Tool: %s\nInput: %s\nOutput: %s", toolName, toolInput, toolOutput);
        message.role = MemoryRole.TOOL;
        message.metadata.put("toolName", toolName);
        message.metadata.put("toolInput", toolInput);
        return message;
    }

    /**
     * 创建思考过程消息
     *
     * @param thought 思考内容
     * @return MemoryMessage实例
     */
    public static MemoryMessage ofThought(String thought) {
        MemoryMessage message = new MemoryMessage();
        message.content = thought;
        message.role = MemoryRole.THOUGHT;
        return message;
    }

    /**
     * 创建动作消息
     *
     * @param action 动作描述
     * @return MemoryMessage实例
     */
    public static MemoryMessage ofAction(String action) {
        MemoryMessage message = new MemoryMessage();
        message.content = action;
        message.role = MemoryRole.ACTION;
        return message;
    }

    /**
     * 创建观察消息
     *
     * @param observation 观察结果
     * @return MemoryMessage实例
     */
    public static MemoryMessage ofObservation(String observation) {
        MemoryMessage message = new MemoryMessage();
        message.content = observation;
        message.role = MemoryRole.OBSERVATION;
        return message;
    }

    /**
     * 创建观察消息
     *
     * @param observation 观察结果
     * @return MemoryMessage实例
     */
    public static MemoryMessage ofObservation(String action, String observation) {
        MemoryMessage message = new MemoryMessage();
        message.content = observation;
        message.role = MemoryRole.OBSERVATION;
        message.metadata.put("action", action);
        return message;
    }

    // ==================== Getter和Setter方法 ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public MemoryMessage sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MemoryRole getRole() {
        return role;
    }

    public void setRole(MemoryRole role) {
        this.role = role;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public MemoryMessage metadata(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public double getImportance() {
        return importance;
    }

    public void setImportance(double importance) {
        this.importance = importance;
    }

    public MemoryMessage importance(double importance) {
        this.importance = importance;
        return this;
    }

    /**
     * 获取格式化的记忆字符串，用于添加到提示词中
     *
     * @return 格式化字符串
     */
    public String toPromptString() {
        if (role == MemoryRole.THOUGHT) {
            return String.format("Thought: %s", content);
        } else if (role == MemoryRole.ACTION) {
            return String.format("Action: %s", content);
        } else if (role == MemoryRole.OBSERVATION) {
            return String.format("Observation: %s", content);
        } else if (role == MemoryRole.TOOL) {
            return content;
        }
        return String.format("%s: %s", role.getDisplayName(), content);
    }

    @Override
    public String toString() {
        return "MemoryMessage{" +
                "id='" + id + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", role=" + role +
                ", timestamp=" + timestamp +
                ", content='" + content + '\'' +
                ", importance=" + importance +
                '}';
    }
}
