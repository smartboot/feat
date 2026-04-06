/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.featclaw.config;

import java.util.List;
import java.util.Map;

/**
 * Agent配置模型 - 定义Agent的配置属性
 * <p>
 * 对应agents目录下的JSON配置文件，包含Agent的基本信息、
 * 角色定义、支持的工具和技能等。
 * </p>
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class AgentConfig {
    
    /**
     * Agent唯一标识名
     */
    private String name;
    
    /**
     * 显示名称
     */
    private String displayName;
    
    /**
     * Agent描述
     */
    private String description;
    
    /**
     * Agent角色定义
     */
    private String role;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 支持的技能列表
     */
    private List<String> skills;
    
    /**
     * 支持的工具列表
     */
    private List<String> tools;
    
    /**
     * 模型配置
     */
    private ModelConfig model;
    
    /**
     * 记忆配置
     */
    private MemoryConfig memory;
    
    /**
     * 是否为调度器Agent
     */
    private boolean isOrchestrator = false;
    
    /**
     * 额外配置参数
     */
    private Map<String, Object> extra;

    // Getters and Setters
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getTools() {
        return tools;
    }

    public void setTools(List<String> tools) {
        this.tools = tools;
    }

    public ModelConfig getModel() {
        return model;
    }

    public void setModel(ModelConfig model) {
        this.model = model;
    }

    public MemoryConfig getMemory() {
        return memory;
    }

    public void setMemory(MemoryConfig memory) {
        this.memory = memory;
    }

    public boolean isOrchestrator() {
        return isOrchestrator;
    }

    public void setOrchestrator(boolean orchestrator) {
        isOrchestrator = orchestrator;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    /**
     * 模型配置内部类
     */
    public static class ModelConfig {
        private float temperature = 0.7f;
        private int maxIterations = 20;
        private String modelName;

        public float getTemperature() {
            return temperature;
        }

        public void setTemperature(float temperature) {
            this.temperature = temperature;
        }

        public int getMaxIterations() {
            return maxIterations;
        }

        public void setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }
    }

    /**
     * 记忆配置内部类
     */
    public static class MemoryConfig {
        private boolean enabled = false;
        private int topK = 5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }
}
