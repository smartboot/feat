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

import java.util.List;

/**
 * A2A 智能体卡片类
 *
 * <p>AgentCard是A2A协议的核心概念，描述了智能体的元数据、能力、接口等信息。
 * 客户端通过AgentCard发现和了解智能体的能力。</p>
 *
 * <p>主要包含以下信息：</p>
 * <ul>
 *   <li>智能体基本信息（名称、描述、版本）</li>
 *   <li>智能体能力（流式、推送通知等）</li>
 *   <li>认证信息</li>
 *   <li>端点URL</li>
 *   <li>技能列表</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class AgentCard {
    /**
     * 智能体名称
     */
    private String name;

    /**
     * 智能体描述
     */
    private String description;

    /**
     * 智能体版本
     */
    private String version;

    /**
     * 智能体URL
     */
    private String url;

    /**
     * 智能体文档URL
     */
    private String documentationUrl;

    /**
     * 提供商信息
     */
    private Provider provider;

    /**
     * 智能体能力
     */
    private AgentCapability capabilities;

    /**
     * 认证配置
     */
    private AgentAuthentication authentication;

    /**
     * 技能列表
     */
    private List<Skill> skills;

    /**
     * 输入类型列表（mime types）
     */
    private List<String> inputModes;

    /**
     * 输出类型列表（mime types）
     */
    private List<String> outputModes;

    /**
     * 默认的输入类型
     */
    private String defaultInputMode;

    /**
     * 默认的输出类型
     */
    private String defaultOutputMode;

    /**
     * 扩展属性
     */
    private JSONObject extensions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public AgentCapability getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(AgentCapability capabilities) {
        this.capabilities = capabilities;
    }

    public AgentAuthentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AgentAuthentication authentication) {
        this.authentication = authentication;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }

    public List<String> getInputModes() {
        return inputModes;
    }

    public void setInputModes(List<String> inputModes) {
        this.inputModes = inputModes;
    }

    public List<String> getOutputModes() {
        return outputModes;
    }

    public void setOutputModes(List<String> outputModes) {
        this.outputModes = outputModes;
    }

    public String getDefaultInputMode() {
        return defaultInputMode;
    }

    public void setDefaultInputMode(String defaultInputMode) {
        this.defaultInputMode = defaultInputMode;
    }

    public String getDefaultOutputMode() {
        return defaultOutputMode;
    }

    public void setDefaultOutputMode(String defaultOutputMode) {
        this.defaultOutputMode = defaultOutputMode;
    }

    public JSONObject getExtensions() {
        return extensions;
    }

    public void setExtensions(JSONObject extensions) {
        this.extensions = extensions;
    }
}
