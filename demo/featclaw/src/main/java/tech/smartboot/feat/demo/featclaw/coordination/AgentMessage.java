/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.featclaw.coordination;

import java.util.Map;
import java.util.UUID;

/**
 * Agent消息 - 用于Agent之间的通信
 * <p>
 * 定义了Agent间通信的消息格式，包含发送者、接收者、消息类型、内容等。
 * 支持请求/响应模式、广播模式和任务委托模式。
 * </p>
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class AgentMessage {
    
    /**
     * 消息唯一ID
     */
    private final String id;
    
    /**
     * 关联的任务ID
     */
    private final String taskId;
    
    /**
     * 发送者Agent名称
     */
    private final String from;
    
    /**
     * 接收者Agent名称
     */
    private final String to;
    
    /**
     * 消息类型
     */
    private final MessageType type;
    
    /**
     * 消息内容
     */
    private final String content;
    
    /**
     * 附加数据
     */
    private final Map<String, Object> metadata;
    
    /**
     * 创建时间戳
     */
    private final long timestamp;
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /**
         * 任务请求 - 请求其他Agent执行任务
         */
        TASK_REQUEST,
        
        /**
         * 任务响应 - 返回任务执行结果
         */
        TASK_RESPONSE,
        
        /**
         * 任务分配 - 分配子任务给其他Agent
         */
        TASK_ASSIGN,
        
        /**
         * 状态查询 - 查询Agent状态
         */
        STATUS_QUERY,
        
        /**
         * 状态报告 - 报告Agent状态
         */
        STATUS_REPORT,
        
        /**
         * 结果收集 - 收集其他Agent的结果
         */
        RESULT_COLLECT,
        
        /**
         * 广播消息 - 发送给所有Agent
         */
        BROADCAST,
        
        /**
         * 通知消息 - 一般性通知
         */
        NOTIFICATION
    }
    
    /**
     * 私有构造函数，使用Builder创建
     */
    private AgentMessage(String id, String taskId, String from, String to, 
                        MessageType type, String content, 
                        Map<String, Object> metadata, long timestamp) {
        this.id = id;
        this.taskId = taskId;
        this.from = from;
        this.to = to;
        this.type = type;
        this.content = content;
        this.metadata = metadata;
        this.timestamp = timestamp;
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public String getFrom() {
        return from;
    }
    
    public String getTo() {
        return to;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public String getContent() {
        return content;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取元数据中的值
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadataValue(String key) {
        if (metadata == null) {
            return null;
        }
        return (T) metadata.get(key);
    }
    
    /**
     * 检查是否为广播消息
     */
    public boolean isBroadcast() {
        return "*".equals(to) || MessageType.BROADCAST.equals(type);
    }
    
    @Override
    public String toString() {
        return String.format("AgentMessage[id=%s, type=%s, from=%s, to=%s, taskId=%s]", 
                id, type, from, to, taskId);
    }
    
    /**
     * Builder模式构建消息
     */
    public static class Builder {
        private String id;
        private String taskId;
        private String from;
        private String to;
        private MessageType type;
        private String content;
        private Map<String, Object> metadata;
        private long timestamp;
        
        public Builder() {
            this.id = UUID.randomUUID().toString();
            this.timestamp = System.currentTimeMillis();
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }
        
        public Builder from(String from) {
            this.from = from;
            return this;
        }
        
        public Builder to(String to) {
            this.to = to;
            return this;
        }
        
        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new java.util.HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public AgentMessage build() {
            if (type == null) {
                throw new IllegalStateException("MessageType is required");
            }
            if (from == null) {
                throw new IllegalStateException("Sender (from) is required");
            }
            if (to == null) {
                throw new IllegalStateException("Receiver (to) is required");
            }
            return new AgentMessage(id, taskId, from, to, type, content, metadata, timestamp);
        }
    }
    
    // 便捷工厂方法
    
    /**
     * 创建任务请求消息
     */
    public static AgentMessage taskRequest(String from, String to, String taskId, String task) {
        return new Builder()
                .from(from)
                .to(to)
                .type(MessageType.TASK_REQUEST)
                .taskId(taskId)
                .content(task)
                .build();
    }
    
    /**
     * 创建任务响应消息
     */
    public static AgentMessage taskResponse(String from, String to, String taskId, String result) {
        return new Builder()
                .from(from)
                .to(to)
                .type(MessageType.TASK_RESPONSE)
                .taskId(taskId)
                .content(result)
                .build();
    }
    
    /**
     * 创建任务分配消息
     */
    public static AgentMessage taskAssign(String from, String to, String taskId, String task, Map<String, Object> params) {
        Builder builder = new Builder()
                .from(from)
                .to(to)
                .type(MessageType.TASK_ASSIGN)
                .taskId(taskId)
                .content(task);
        if (params != null) {
            builder.metadata(params);
        }
        return builder.build();
    }
    
    /**
     * 创建广播消息
     */
    public static AgentMessage broadcast(String from, String content) {
        return new Builder()
                .from(from)
                .to("*")
                .type(MessageType.BROADCAST)
                .content(content)
                .build();
    }
    
    /**
     * 创建通知消息
     */
    public static AgentMessage notification(String from, String to, String message) {
        return new Builder()
                .from(from)
                .to(to)
                .type(MessageType.NOTIFICATION)
                .content(message)
                .build();
    }
}
