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
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * Agent配置选项
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.1
 */
public class AgentOptions {

    private static final Logger logger = LoggerFactory.getLogger(AgentOptions.class.getName());


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

    /**
     * 最大推理迭代次数
     */
    private int maxIterations = 20;

    /**
     * 工具执行器映射
     */
    private final Map<String, AgentTool> toolExecutors = new HashMap<>();

    private final ChatOptions chatOptions = new ChatOptions();

    public static AgentOptions create() {
        return new AgentOptions();
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

    /**
     * 设置最大推理迭代次数
     *
     * @param maxIterations 最大迭代次数
     * @return 当前实例
     */
    public AgentOptions maxIterations(int maxIterations) {
        this.maxIterations = Math.max(1, maxIterations);
        logger.info("设置最大推理迭代次数: " + maxIterations);
        return this;
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

    public int getMaxIterations() {
        return maxIterations;
    }

    Prompt getPrompt() {
        return prompt;
    }

    public AgentOptions prompt(Prompt prompt) {
        this.prompt = prompt;
        return this;
    }

    public AgentOptions prompt(String prompt) {
        return prompt(new Prompt(prompt));
    }

    /**
     * 添加工具执行器
     *
     * @param executor 工具执行器
     */
    public AgentOptions addTool(AgentTool executor) {
        toolExecutors.put(executor.getName(), executor);
        logger.info("添加工具执行器: " + executor.getName());
        return this;
    }

    AgentTool getToolExecutor(String name) {
        return toolExecutors.get(name);
    }

    Map<String, AgentTool> getToolExecutors() {
        return toolExecutors;
    }


    public ChatOptions chatOptions() {
        return chatOptions;
    }
}