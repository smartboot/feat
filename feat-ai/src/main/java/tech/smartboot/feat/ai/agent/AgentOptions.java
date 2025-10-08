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

import tech.smartboot.feat.ai.agent.memory.AgentMemory;
import tech.smartboot.feat.ai.agent.memory.DefaultAgentMemory;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;


/**
 * Agent配置选项
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.1
 */
public class AgentOptions {

    private static final Logger logger = LoggerFactory.getLogger(AgentOptions.class.getName());

    /**
     * Agent名称
     */
    private String name = "DefaultAgent";

    /**
     * Agent描述
     */
    private String description = "默认Agent";

    /**
     * 模型供应商
     */
//    private ChatModelVendor vendor = ChatModelVendor.GiteeAI.Qwen3_235B_A22B_Instruct_2507;
    private ChatModelVendor vendor = ChatModelVendor.Ollama.Deepseek_r1_7B;

    /**
     * Agent记忆
     */
    private AgentMemory memory = new DefaultAgentMemory();

    /**
     * 是否启用智能记忆检索
     */
    private boolean enableSmartMemory = true;

    /**
     * 记忆检索阈值
     */
    private double memoryRetrievalThreshold = 0.5;

    /**
     * 最大记忆检索数量
     */
    private int maxMemoryRetrievalCount = 5;
    private Prompt prompt;

    public static AgentOptions create() {
        return new AgentOptions();
    }

    public AgentOptions name(String name) {
        this.name = name;
        return this;
    }

    public AgentOptions description(String description) {
        this.description = description;
        return this;
    }

    public AgentOptions vendor(ChatModelVendor vendor) {
        this.vendor = vendor;
        return this;
    }

    public AgentOptions memory(AgentMemory memory) {
        this.memory = memory;
        logger.info("设置Agent记忆");
        return this;
    }

    /**
     * 启用智能记忆检索
     *
     * @return 当前实例
     */
    public AgentOptions enableSmartMemory() {
        this.enableSmartMemory = true;
        logger.info("启用智能记忆检索");
        return this;
    }

    /**
     * 禁用智能记忆检索
     *
     * @return 当前实例
     */
    public AgentOptions disableSmartMemory() {
        this.enableSmartMemory = false;
        logger.info("禁用智能记忆检索");
        return this;
    }

    /**
     * 设置记忆检索阈值
     *
     * @param threshold 阈值 (0-1)
     * @return 当前实例
     */
    public AgentOptions memoryRetrievalThreshold(double threshold) {
        this.memoryRetrievalThreshold = Math.max(0, Math.min(1, threshold));
        logger.info("设置记忆检索阈值: " + threshold);
        return this;
    }

    /**
     * 设置最大记忆检索数量
     *
     * @param count 数量
     * @return 当前实例
     */
    public AgentOptions maxMemoryRetrievalCount(int count) {
        this.maxMemoryRetrievalCount = Math.max(1, count);
        logger.info("设置最大记忆检索数量: " + count);
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ChatModelVendor getVendor() {
        return vendor;
    }

    public AgentMemory getMemory() {
        return memory;
    }

    public boolean isEnableSmartMemory() {
        return enableSmartMemory;
    }

    public double getMemoryRetrievalThreshold() {
        return memoryRetrievalThreshold;
    }

    public int getMaxMemoryRetrievalCount() {
        return maxMemoryRetrievalCount;
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public AgentOptions prompt(Prompt prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * 验证配置选项
     *
     * @return 验证结果
     */
    public boolean validate() {
        if (name == null || name.trim().isEmpty()) {
            logger.warn("Agent名称不能为空");
            return false;
        }

        if (vendor == null) {
            logger.warn("模型供应商不能为空");
            return false;
        }

        if (memory == null) {
            logger.warn("Agent记忆不能为空");
            return false;
        }

        logger.info("Agent配置验证通过: " + name);
        return true;
    }

    /**
     * 获取配置摘要
     *
     * @return 配置摘要
     */
    public String getSummary() {
        return String.format(
                "Agent配置摘要:\n" +
                        "- 名称: %s\n" +
                        "- 描述: %s\n" +
                        "- 模型: %s\n" +
                        "- 智能记忆: %s\n" +
                        "- 记忆阈值: %.2f\n" +
                        "- 最大检索数: %d",
                name, description, vendor, enableSmartMemory ? "启用" : "禁用",
                memoryRetrievalThreshold, maxMemoryRetrievalCount
        );
    }
}