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
 * Skill配置模型 - 定义技能的配置属性
 * <p>
 * 对应skills目录下的JSON配置文件，包含技能的基本信息、
 * 所需工具、提示词等。
 * </p>
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class SkillConfig {
    
    /**
     * Skill唯一标识名
     */
    private String name;
    
    /**
     * 显示名称
     */
    private String displayName;
    
    /**
     * Skill描述
     */
    private String description;
    
    /**
     * 技能类别
     */
    private String category;
    
    /**
     * 关键词列表，用于匹配用户意图
     */
    private List<String> keywords;
    
    /**
     * 所需的工具列表
     */
    private List<String> requiredTools;
    
    /**
     * 技能提示词
     */
    private String prompt;
    
    /**
     * 使用示例
     */
    private List<String> examples;
    
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getRequiredTools() {
        return requiredTools;
    }

    public void setRequiredTools(List<String> requiredTools) {
        this.requiredTools = requiredTools;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}
