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

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.memory.AgentMemory;
import tech.smartboot.feat.ai.agent.memory.DefaultAgentMemory;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 新版Agent构建器
 * 提供更简洁的API和更好的类型安全
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v2.0.0
 */
public class AgentBuilder {

    private String name;
    private String description;
    private ChatModelVendor vendor = ChatModelVendor.GiteeAI.Qwen3_4B;
    private AgentMemory memory = new DefaultAgentMemory();
    private final List<ToolExecutor> tools = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(AgentBuilder.class.getName());

    private AgentBuilder() {
    }

    public static AgentBuilder create() {
        return new AgentBuilder();
    }

    public AgentBuilder name(String name) {
        this.name = name;
        return this;
    }

    public AgentBuilder description(String description) {
        this.description = description;
        return this;
    }

    public AgentBuilder vendor(ChatModelVendor vendor) {
        this.vendor = vendor;
        return this;
    }

    public AgentBuilder memory(AgentMemory memory) {
        this.memory = memory;
        return this;
    }

    public AgentBuilder addTool(ToolExecutor tool) {
        tools.add(tool);
        return this;
    }

    public AgentBuilder addTools(List<ToolExecutor> toolList) {
        tools.addAll(toolList);
        return this;
    }

    /**
     * 构建简单Agent
     *
     * @return SimpleAgent实例
     */
    public SimpleAgent buildSimple() {
        validate();
        ChatModel chatModel = FeatAI.chatModel(options -> options.model(vendor));
        SimpleAgent agent = new SimpleAgent(name, chatModel, memory);
        tools.forEach(agent::addTool);
        logger.info("构建简单Agent: " + name);
        return agent;
    }

    /**
     * 构建增强Agent
     *
     * @return EnhancedAgent实例
     */
    public EnhancedAgent buildEnhanced() {
        validate();
        ChatModel chatModel = FeatAI.chatModel(options -> options.model(vendor));
        EnhancedAgent agent = new EnhancedAgent(name, chatModel, memory);
        tools.forEach(agent::addTool);
        logger.info("构建增强Agent: " + name);
        return agent;
    }

    /**
     * 构建可配置Agent
     *
     * @return ConfigurableAgent实例
     */
    public ConfigurableAgent buildConfigurable() {
        validate();
        AgentOptions options = AgentOptions.create()
                .name(name)
                .vendor(vendor)
                .memory(memory);
        ConfigurableAgent agent = new ConfigurableAgent(options);
        tools.forEach(agent::addTool);
        logger.info("构建可配置Agent: " + name);
        return agent;
    }

    private void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent名称不能为空");
        }
        if (vendor == null) {
            throw new IllegalArgumentException("模型供应商不能为空");
        }
        if (memory == null) {
            throw new IllegalArgumentException("Agent记忆不能为空");
        }
    }
}