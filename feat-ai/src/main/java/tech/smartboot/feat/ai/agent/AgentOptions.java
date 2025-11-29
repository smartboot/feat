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

import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * Agent配置选项
 * <p>
 * 该类用于配置和管理AI Agent的各项参数和组件，包括：
 * 1. Agent的基本配置（如最大迭代次数）
 * 2. 提示词模板
 * 3. 可用工具集合
 * 4. 聊天模型配置
 * </p>
 * <p>
 * 采用Builder模式设计，支持链式调用，便于灵活配置。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.1
 */
public class AgentOptions {

    private static final Logger logger = LoggerFactory.getLogger(AgentOptions.class.getName());

    /**
     * Agent使用的提示词模板
     * <p>
     * 提示词模板定义了Agent的行为模式、任务处理方式和输出格式，
     * 是控制Agent行为的核心要素之一。
     * </p>
     */
    private Prompt prompt;

    /**
     * 最大推理迭代次数
     * <p>
     * 限制Agent在解决问题时的最大尝试次数，防止无限循环。
     * 当Agent需要通过多次思考和工具调用来完成任务时，
     * 此参数控制整个过程的最大迭代次数。
     * </p>
     */
    private int maxIterations = 20;

    /**
     * 工具执行器映射
     * <p>
     * 存储Agent可用的所有工具，以工具名称为键，工具执行器为值。
     * 当Agent需要执行特定任务时，会根据名称查找并调用相应的工具。
     * </p>
     */
    private final Map<String, AgentTool> toolExecutors = new HashMap<>();

    /**
     * 聊天选项配置
     * <p>
     * 包含与AI模型交互相关的配置，如模型供应商、温度参数、最大token数等。
     * 这些配置直接影响AI模型的响应质量和性能。
     * </p>
     */
    private final ChatOptions chatOptions = new ChatOptions();

    /**
     * 创建AgentOptions实例的静态工厂方法
     * <p>
     * 提供一种标准的方式来创建AgentOptions实例，
     * 便于未来可能的扩展和修改。
     * </p>
     *
     * @return 新创建的AgentOptions实例
     */
    public static AgentOptions create() {
        return new AgentOptions();
    }


    /**
     * 设置最大推理迭代次数
     * <p>
     * 控制Agent在解决问题时的最大尝试次数，防止无限循环。
     * 如果设置的值小于1，则会被自动调整为1。
     * </p>
     *
     * @param maxIterations 最大迭代次数，建议根据任务复杂度合理设置
     * @return 当前实例，支持链式调用
     */
    public AgentOptions maxIterations(int maxIterations) {
        this.maxIterations = Math.max(1, maxIterations);
        logger.info("设置最大推理迭代次数: " + maxIterations);
        return this;
    }


    /**
     * 获取最大推理迭代次数
     *
     * @return 当前配置的最大迭代次数
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * 获取提示词模板
     * <p>
     * 提示词模板定义了Agent的行为模式和任务处理方式，
     * 是控制Agent行为的核心要素。
     * </p>
     *
     * @return 当前配置的提示词模板
     */
    Prompt getPrompt() {
        return prompt;
    }

    /**
     * 设置提示词模板对象
     *
     * @param prompt 提示词模板对象
     * @return 当前实例，支持链式调用
     */
    public AgentOptions prompt(Prompt prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * 通过字符串设置提示词模板
     * <p>
     * 将字符串转换为Prompt对象并设置为当前Agent的提示词模板。
     * </p>
     *
     * @param prompt 提示词模板字符串
     * @return 当前实例，支持链式调用
     */
    public AgentOptions prompt(String prompt) {
        return prompt(new Prompt(prompt));
    }

    /**
     * 添加工具执行器
     * <p>
     * 将指定的工具执行器添加到工具集合中，使其可以在Agent中被调用。
     * 如果已存在同名工具，新工具将覆盖旧工具。
     * </p>
     *
     * @param executor 工具执行器实例
     * @return 当前实例，支持链式调用
     */
    public AgentOptions addTool(AgentTool executor) {
        toolExecutors.put(executor.getName(), executor);
        logger.info("添加工具执行器: " + executor.getName());
        return this;
    }

    /**
     * 根据名称获取工具执行器
     *
     * @param name 工具名称
     * @return 对应的工具执行器，如果不存在则返回null
     */
    AgentTool getToolExecutor(String name) {
        return toolExecutors.get(name);
    }

    /**
     * 获取所有工具执行器映射
     *
     * @return 包含所有工具执行器的映射
     */
    Map<String, AgentTool> getToolExecutors() {
        return toolExecutors;
    }


    /**
     * 获取聊天选项配置
     * <p>
     * 返回用于配置AI模型交互参数的ChatOptions对象，
     * 可用于进一步配置模型相关参数。
     * </p>
     *
     * @return ChatOptions实例
     */
    public ChatOptions chatOptions() {
        return chatOptions;
    }
}