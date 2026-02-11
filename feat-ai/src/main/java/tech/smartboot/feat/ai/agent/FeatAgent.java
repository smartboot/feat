package tech.smartboot.feat.ai.agent;

import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI Agent抽象基类，基于ReAct（Reasoning + Acting）范式实现
 * <p>
 * ReAct范式是一种先进的AI代理框架，它将推理(Reasoning)和行动(Acting)相结合，
 * 使AI代理能够像人类一样在思考和行动之间交替进行。在处理复杂任务时，
 * Agent会先进行推理分析任务需求，然后采取行动执行具体操作，
 * 根据执行结果再进行推理，如此循环直到任务完成。
 * 这种模式特别适用于需要多步骤操作、工具调用和动态决策的复杂场景。
 * </p>
 * <p>
 * 该抽象类定义了Agent的基本结构和核心机制，具体实现需要继承此类并实现
 * {@link #execute(String)}方法。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.1
 * @see <a href="https://arxiv.org/abs/2210.03350">ReAct论文</a>
 * @see AgentOptions 代理配置选项
 * @see AgentState 代理状态枚举
 * @see AgentTool 代理工具接口
 */
public abstract class FeatAgent {
    /**
     * 日志记录器实例，用于记录Agent运行过程中的关键信息、状态变更和调试信息
     * <p>
     * 使用LoggerFactory获取当前类的Logger实例，确保日志信息能够被正确分类和管理。
     * 日志记录对于调试、监控和问题排查非常重要，特别是在复杂的AI代理执行流程中。
     * 通过日志可以追踪Agent的状态变化、任务执行过程和潜在问题。
     * </p>
     */
    private static final Logger logger = LoggerFactory.getLogger(FeatAgent.class.getName());

    /**
     * Agent配置选项容器，包含该Agent实例的所有配置参数和组件引用
     * <p>
     * 该字段为protected访问级别，允许子类直接访问配置选项。
     * 包含的配置项有：
     * - Agent基本信息（名称、描述、角色名）
     * - AI模型配置（模型供应商、聊天选项）
     * - 记忆系统配置（记忆管理器、智能检索开关、检索阈值等）
     * - 工具执行器映射（可用工具列表）
     * - 执行控制参数（最大迭代次数等）
     * </p>
     * <p>
     * 通过options字段，Agent可以访问所有必要的配置和组件，
     * 实现灵活的运行时行为调整。
     * </p>
     *
     * @see AgentOptions 代理配置选项详情
     */
    protected final AgentOptions options = new AgentOptions();

    /**
     * Agent当前运行状态枚举值，反映Agent在ReAct循环中的具体阶段
     * <p>
     * 状态流转通常遵循以下模式：
     * IDLE → RUNNING → (TOOL_EXECUTION → RUNNING)* → FINISHED|ERROR
     * </p>
     * <p>
     * 各状态详细说明：
     * - {@link AgentState#IDLE IDLE}: 初始状态或任务完成/出错后的空闲状态，
     * 表示Agent已准备好接受新任务
     * - {@link AgentState#RUNNING RUNNING}: 核心推理阶段，Agent正在分析任务需求、
     * 制定执行计划或处理工具调用结果
     * - {@link AgentState#TOOL_EXECUTION TOOL_EXECUTION}: 工具执行阶段，
     * Agent正在调用外部工具（如计算器、搜索引擎等）执行具体操作
     * - {@link AgentState#FINISHED FINISHED}: 任务成功完成状态，
     * 表示Agent已成功解决用户提出的问题
     * - {@link AgentState#ERROR ERROR}: 错误状态，
     * 表示在执行过程中遇到无法恢复的异常
     * </p>
     *
     * @see AgentState 状态枚举定义
     */
    private AgentState state = AgentState.IDLE;
    protected boolean cancel = false;

    /**
     * 执行用户任务的核心抽象方法，需要由具体的Agent实现类提供实际逻辑
     * <p>
     * 该方法是Agent处理用户请求的入口点，子类需要根据ReAct范式实现完整的处理流程：
     * 1. 接收并理解用户输入
     * 2. 进入推理循环，交替进行思考和行动
     * 3. 根据需要调用工具执行具体操作
     * 4. 整合所有信息生成最终响应
     * </p>
     * <p>
     * 实现时应考虑：
     * - 合理使用记忆系统存储和检索历史信息
     * - 控制迭代次数避免无限循环
     * - 正确处理工具调用和异常情况
     * - 及时更新Agent状态
     * </p>
     *
     * @param input 用户输入的任务描述或问题，通常为自然语言文本
     * @return 经过处理后的结果字符串，应能准确回答用户问题或完成指定任务
     * @see #setState(AgentState) 状态更新方法
     */
    public CompletableFuture<String> execute(String input) {
        return execute(Collections.singletonList(Message.ofUser(input)));
    }

    public abstract CompletableFuture<String> execute(List<Message> messages);


    /**
     * 获取Agent当前运行状态，用于外部监控和流程控制
     * <p>
     * 通过检查Agent状态，调用方可以：
     * - 确定Agent是否可以接受新任务（IDLE状态）
     * - 监控任务执行进度
     * - 在适当的时候获取最终结果
     * - 处理异常情况
     * </p>
     * <p>
     * 注意：状态查询本身不会改变Agent的运行状态。
     * </p>
     *
     * @return AgentState Agent当前的状态枚举值
     * @see AgentState 状态枚举定义
     * @see #setState(AgentState) 状态设置方法
     */
    public AgentState getState() {
        return state;
    }

    /**
     * 设置Agent运行状态，仅限内部使用以确保状态变更的一致性
     * <p>
     * 该方法具有以下特点：
     * - 使用final修饰符防止子类重写，确保状态管理的统一性
     * - 使用protected访问级别，仅允许同包或子类调用
     * - 自动记录状态变更日志，便于调试和监控
     * - 是ReAct执行流程中的关键环节
     * </p>
     * <p>
     * 状态变更时机示例：
     * - 开始处理任务时：IDLE → RUNNING
     * - 调用工具时：RUNNING → TOOL_EXECUTION
     * - 工具执行完成后：TOOL_EXECUTION → RUNNING
     * - 任务完成时：RUNNING → FINISHED
     * - 发生错误时：任意状态 → ERROR
     * </p>
     *
     * @param state 新的状态值，不能为空
     * @throws IllegalArgumentException 当state参数为null时抛出
     * @see AgentState 状态枚举定义
     * @see #getState() 状态查询方法
     */
    protected final void setState(AgentState state) {
        if (state == null) {
            throw new IllegalArgumentException("Agent状态不能为null");
        }
        this.state = state;
        logger.info("Agent状态变更: " + state);
    }

    public AgentOptions options() {
        return options;
    }

    public void cancel() {
        this.cancel = true;
    }
}