/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话选项配置
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v2.0.0
 */
public class ChatOptions {
    
    private boolean enableTools = false;
    private boolean enableMemory = false;
    private double memoryRetrievalThreshold = 0.5;
    private int maxMemoryRetrievalCount = 5;
    private List<String> toolNames = new ArrayList<>();
    
    public static ChatOptions create() {
        return new ChatOptions();
    }
    
    public ChatOptions enableTools() {
        this.enableTools = true;
        return this;
    }
    
    public ChatOptions disableTools() {
        this.enableTools = false;
        return this;
    }
    
    public ChatOptions enableMemory() {
        this.enableMemory = true;
        return this;
    }
    
    public ChatOptions disableMemory() {
        this.enableMemory = false;
        return this;
    }
    
    public ChatOptions memoryRetrievalThreshold(double threshold) {
        this.memoryRetrievalThreshold = Math.max(0, Math.min(1, threshold));
        return this;
    }
    
    public ChatOptions maxMemoryRetrievalCount(int count) {
        this.maxMemoryRetrievalCount = Math.max(1, count);
        return this;
    }
    
    public ChatOptions toolNames(List<String> toolNames) {
        this.toolNames = new ArrayList<>(toolNames);
        return this;
    }
    
    public ChatOptions addToolName(String toolName) {
        this.toolNames.add(toolName);
        return this;
    }
    
    public boolean isEnableTools() {
        return enableTools;
    }
    
    public boolean isEnableMemory() {
        return enableMemory;
    }
    
    public double getMemoryRetrievalThreshold() {
        return memoryRetrievalThreshold;
    }
    
    public int getMaxMemoryRetrievalCount() {
        return maxMemoryRetrievalCount;
    }
    
    public List<String> getToolNames() {
        return new ArrayList<>(toolNames);
    }
}