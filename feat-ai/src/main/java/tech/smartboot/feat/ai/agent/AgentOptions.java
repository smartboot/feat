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

import tech.smartboot.feat.ai.agent.hook.Hook;
import tech.smartboot.feat.ai.agent.memory.Memory;
import tech.smartboot.feat.ai.agent.memory.MemoryOptions;
import tech.smartboot.feat.ai.agent.memory.VectorMemoryOptions;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


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
    private Hook hook = new Hook() {
    };
    /**
     * 最大推理迭代次数
     * <p>
     * 限制Agent在解决问题时的最大尝试次数，防止无限循环。
     * 当Agent需要通过多次思考和工具调用来完成任务时，
     * 此参数控制整个过程的最大迭代次数。
     * </p>
     */
    private int maxIterations = 20;
    private ActionParse actionParse = new DefaultActionParse();
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
     * 记忆系统
     * <p>
     * 用于存储和检索Agent的历史交互记录。
     * 支持短期记忆和长期记忆两种模式。
     * </p>
     */
    private Memory memory;

    /**
     * 是否启用记忆检索
     */
    private boolean memoryEnabled = false;

    /**
     * 记忆检索数量
     */
    private int memoryTopK = 5;

    /**
     * 会话ID，用于隔离不同会话的记忆
     */
    private String sessionId;

    AgentOptions() {
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
    public AgentOptions tool(AgentTool executor) {
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

    ActionParse actionParse() {
        return actionParse;
    }

    public Hook hook() {
        return hook;
    }

    public AgentOptions hook(Hook hook) {
        this.hook = hook;
        return this;
    }

    // ==================== 记忆系统相关方法 ====================

    /**
     * 设置记忆系统
     * <p>
     * 配置Agent使用的记忆存储实现，支持InMemoryMemory和VectorMemory。
     * </p>
     *
     * @param memory 记忆系统实例
     * @return 当前实例
     */
    public AgentOptions memory(Memory memory) {
        this.memory = memory;
        this.memoryEnabled = true;
        logger.info("设置记忆系统: {}", memory.getClass().getSimpleName());
        return this;
    }

    /**
     * 创建基于内存的记忆系统
     * <p>
     * 使用内存存储记忆，适合开发和测试场景。
     * </p>
     *
     * @param consumer 配置选项消费者
     * @return 当前实例
     */
    public AgentOptions memory(Consumer<MemoryOptions> consumer) {
        this.memory = Memory.inMemory(consumer);
        this.memoryEnabled = true;
        logger.info("创建InMemoryMemory记忆系统");
        return this;
    }

    /**
     * 创建基于向量的记忆系统
     * <p>
     * 使用向量数据库存储记忆，支持语义检索，适合生产环境。
     * 需要配置VectorStore和EmbeddingModel。
     * </p>
     *
     * @param consumer 配置选项消费者
     * @return 当前实例
     */
    public AgentOptions vectorMemory(Consumer<VectorMemoryOptions> consumer) {
        VectorMemoryOptions options = new VectorMemoryOptions();
        consumer.accept(options);
        // 将Agent的sessionId同步到VectorMemoryOptions
        if (this.sessionId != null && options.getSessionId() == null) {
            options.sessionId(this.sessionId);
        }
        this.memory = Memory.vector(opts -> {
            opts.vectorStore(options.getVectorStore())
                .embeddingModel(options.getEmbeddingModel())
                .collectionName(options.getCollectionName())
                .vectorDimension(options.getVectorDimension())
                .sessionId(options.getSessionId());
            // 同步其他配置
            if (options.getSessionId() != null) {
                opts.disableSessionIsolation();
            }
        });
        this.memoryEnabled = true;
        logger.info("创建VectorMemory记忆系统");
        return this;
    }

    /**
     * 禁用记忆系统
     * <p>
     * 关闭记忆检索和存储功能。
     * </p>
     *
     * @return 当前实例
     */
    public AgentOptions disableMemory() {
        this.memoryEnabled = false;
        logger.info("禁用记忆系统");
        return this;
    }

    /**
     * 启用记忆系统
     * <p>
     * 如果尚未配置记忆系统，会自动创建一个默认的InMemoryMemory。
     * </p>
     *
     * @return 当前实例
     */
    public AgentOptions enableMemory() {
        if (this.memory == null) {
            this.memory = Memory.inMemory(opts -> {});
        }
        this.memoryEnabled = true;
        logger.info("启用记忆系统");
        return this;
    }

    /**
     * 设置记忆检索数量
     * <p>
     * 控制每次检索返回的记忆条数。
     * </p>
     *
     * @param topK 检索数量
     * @return 当前实例
     */
    public AgentOptions memoryTopK(int topK) {
        this.memoryTopK = Math.max(1, topK);
        return this;
    }

    /**
     * 设置会话ID
     * <p>
     * 用于隔离不同会话的记忆。设置后会同步到记忆系统（如果已配置）。
     * 对于VectorMemory，会动态更新其sessionId配置。
     * </p>
     *
     * @param sessionId 会话ID
     * @return 当前实例
     */
    public AgentOptions sessionId(String sessionId) {
        this.sessionId = sessionId;
        // 如果记忆系统已配置且是VectorMemory，同步更新sessionId
        if (this.memory instanceof tech.smartboot.feat.ai.agent.memory.VectorMemory) {
            ((tech.smartboot.feat.ai.agent.memory.VectorMemory) this.memory).setSessionId(sessionId);
        }
        return this;
    }

    // ==================== Getter方法 ====================

    /**
     * 获取记忆系统
     *
     * @return Memory实例，可能为null
     */
    public Memory getMemory() {
        return memory;
    }

    /**
     * 判断是否启用了记忆系统
     *
     * @return true表示启用
     */
    public boolean isMemoryEnabled() {
        return memoryEnabled;
    }

    /**
     * 获取记忆检索数量
     *
     * @return 检索数量
     */
    public int getMemoryTopK() {
        return memoryTopK;
    }

    /**
     * 获取会话ID
     *
     * @return 会话ID
     */
    public String getSessionId() {
        return sessionId;
    }
}