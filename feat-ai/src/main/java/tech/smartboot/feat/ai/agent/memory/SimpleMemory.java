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

/**
 * 简单记忆实现
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SimpleMemory implements Memory {
    
    private final String content;
    private final long timestamp;
    private final double importance;
    
    public SimpleMemory(String content) {
        this(content, System.currentTimeMillis(), 0.5);
    }
    
    public SimpleMemory(String content, double importance) {
        this(content, System.currentTimeMillis(), importance);
    }
    
    public SimpleMemory(String content, long timestamp, double importance) {
        this.content = content;
        this.timestamp = timestamp;
        this.importance = importance;
    }
    
    @Override
    public String getContent() {
        return content;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public double getImportance() {
        return importance;
    }
}