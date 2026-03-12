/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.server;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.a2a.model.AgentCard;
import tech.smartboot.feat.ai.a2a.model.AgentCapability;
import tech.smartboot.feat.ai.a2a.model.Provider;
import tech.smartboot.feat.ai.a2a.model.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * A2A 服务器配置选项类
 *
 * <p>用于配置A2A服务器的各项参数，包括智能体信息、能力、技能等。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class A2AServerOptions {
    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 智能体描述
     */
    private String agentDescription;

    /**
     * 智能体版本
     */
    private String agentVersion = "1.0.0";

    /**
     * 智能体URL
     */
    private String agentUrl;

    /**
     * 提供商信息
     */
    private Provider provider;

    /**
     * 智能体能力
     */
    private AgentCapability capabilities;

    /**
     * 技能列表
     */
    private List<Skill> skills = new ArrayList<>();

    /**
     * 输入类型列表
     */
    private List<String> inputModes = new ArrayList<>();

    /**
     * 输出类型列表
     */
    private List<String> outputModes = new ArrayList<>();

    /**
     * 默认输入类型
     */
    private String defaultInputMode = "text/plain";

    /**
     * 默认输出类型
     */
    private String defaultOutputMode = "text/plain";

    /**
     * 扩展配置
     */
    private JSONObject extensions;

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentDescription() {
        return agentDescription;
    }

    public void setAgentDescription(String agentDescription) {
        this.agentDescription = agentDescription;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getAgentUrl() {
        return agentUrl;
    }

    public void setAgentUrl(String agentUrl) {
        this.agentUrl = agentUrl;
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

    /**
     * 添加技能
     *
     * @param skill 技能
     * @return 当前A2AServerOptions实例（链式调用）
     */
    public A2AServerOptions addSkill(Skill skill) {
        if (this.skills == null) {
            this.skills = new ArrayList<>();
        }
        this.skills.add(skill);
        return this;
    }

    /**
     * 添加输入类型
     *
     * @param inputMode 输入类型
     * @return 当前A2AServerOptions实例（链式调用）
     */
    public A2AServerOptions addInputMode(String inputMode) {
        if (this.inputModes == null) {
            this.inputModes = new ArrayList<>();
        }
        this.inputModes.add(inputMode);
        return this;
    }

    /**
     * 添加输出类型
     *
     * @param outputMode 输出类型
     * @return 当前A2AServerOptions实例（链式调用）
     */
    public A2AServerOptions addOutputMode(String outputMode) {
        if (this.outputModes == null) {
            this.outputModes = new ArrayList<>();
        }
        this.outputModes.add(outputMode);
        return this;
    }

    /**
     * 构建AgentCard
     *
     * @return AgentCard实例
     */
    public AgentCard buildAgentCard() {
        AgentCard card = new AgentCard();
        card.setName(agentName);
        card.setDescription(agentDescription);
        card.setVersion(agentVersion);
        card.setUrl(agentUrl);
        card.setProvider(provider);
        card.setCapabilities(capabilities);
        card.setSkills(skills);
        card.setInputModes(inputModes);
        card.setOutputModes(outputModes);
        card.setDefaultInputMode(defaultInputMode);
        card.setDefaultOutputMode(defaultOutputMode);
        card.setExtensions(extensions);
        return card;
    }
}
