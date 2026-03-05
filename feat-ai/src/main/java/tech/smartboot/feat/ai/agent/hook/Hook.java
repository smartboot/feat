/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.hook;

import tech.smartboot.feat.ai.agent.ToolCaller;
import tech.smartboot.feat.ai.chat.entity.Message;

import java.util.List;

/**
 * Agent 执行钩子接口，提供在 Agent 生命周期各个关键节点的回调方法
 * <p>
 * Hook 接口允许开发者在 Agent 执行的不同阶段插入自定义逻辑，实现以下功能：
 * </p>
 * <ul>
 *     <li>消息处理前后的预处理和后处理</li>
 *     <li>工具调用前后的拦截和监控</li>
 *     <li>推理过程的实时跟踪</li>
 *     <li>流式响应的内容捕获</li>
 * </ul>
 * <p>
 * 所有方法都提供了默认空实现，开发者可以根据需要选择性地重写特定方法。
 * </p>
 * <p>
 * 典型使用场景包括：
 * </p>
 * <ul>
 *     <li>日志记录和审计</li>
 *     <li>性能监控和指标收集</li>
 *     <li>消息内容的修改或增强</li>
 *     <li>工具调用的权限验证</li>
 *     <li>实时进度展示</li>
 * </ul>
 *
 * @author 三刀
 * @version v1.0 2/10/26
 * @see tech.smartboot.feat.ai.agent.AgentOptions#hook(Hook) 设置 Hook 的方法
 * @see tech.smartboot.feat.ai.agent.ReActAgent ReActAgent 中的实际使用示例
 */
public interface Hook {
    /**
     * 在调用 AI 模型前执行的预处理回调方法
     * <p>
     * 此方法在 Agent 准备向 AI 模型发送请求之前被调用，可以用于：
     * </p>
     * <ul>
     *     <li>检查和验证消息列表的完整性</li>
     *     <li>对消息内容进行预处理或增强</li>
     *     <li>记录请求前的状态信息</li>
     *     <li>添加额外的上下文信息到消息中</li>
     * </ul>
     * <p>
     * 注意：在此阶段修改消息列表可能会影响 AI 模型的响应结果。
     * </p>
     *
     * @param message 即将发送给 AI 模型的消息列表，包含用户输入和历史对话记录
     *                消息类型可以是用户消息 (user)、系统消息 (system) 或助手消息 (assistant)
     * @see Message 消息类定义
     * @see tech.smartboot.feat.ai.chat.ChatModel#chatStream 流式聊天请求
     */
    default void preCall(List<Message> message) {
    }

    /**
     * 在 AI 模型调用完成后执行的后处理回调方法
     * <p>
     * 此方法在 Agent 接收到 AI 模型的完整响应后被调用，可以用于：
     * </p>
     * <ul>
     *     <li>处理和转换 AI 的输出内容</li>
     *     <li>记录响应结果用于日志或审计</li>
     *     <li>提取响应中的关键信息</li>
     *     <li>对响应内容进行后处理（如格式化、过滤等）</li>
     * </ul>
     * <p>
     * 此时消息已包含 AI 的完整回答，但还未返回给用户。
     * </p>
     *
     * @param message AI 模型返回的响应消息，包含最终的回答内容
     *                通常为助手角色 (assistant) 的消息
     * @see Message 消息类定义
     */
    default void postCall(Message message) {
    }

    /**
     * 在工具调用前执行的预处理回调方法
     * <p>
     * 当 Agent 决定调用某个工具（如文件操作、网络搜索等）时，
     * 此方法会在实际执行工具之前被调用，可以用于：
     * </p>
     * <ul>
     *     <li>验证工具调用的合法性和权限</li>
     *     <li>记录工具调用的详细日志</li>
     *     <li>修改或增强工具的输入参数</li>
     *     <li>实施速率限制或其他控制策略</li>
     * </ul>
     * <p>
     * 此阶段是监控和控制 Agent 行为的关键节点。
     * </p>
     *
     * @param toolCaller 工具调用信息对象，包含：
     *                   - thought: Agent 的思考过程
     *                   - action: 要执行的工具名称
     *                   - actionInput: 传递给工具的参数
     * @see ToolCaller 工具调用者类定义
     * @see tech.smartboot.feat.ai.agent.AgentTool 工具执行器接口
     */
    default void preTool(ToolCaller toolCaller) {
    }

    /**
     * 在工具调用完成后执行的后处理回调方法
     * <p>
     * 此方法在工具执行完毕并获得观察结果后被调用，可以用于：
     * </p>
     * <ul>
     *     <li>记录工具执行的结果和性能指标</li>
     *     <li>处理工具执行过程中的异常情况</li>
     *     <li>分析和统计工具使用情况</li>
     *     <li>根据执行结果决定是否需要后续操作</li>
     * </ul>
     * <p>
     * 此时 toolCaller 已包含工具的观察结果 (observation) 和可能的异常信息 (throwable)。
     * </p>
     *
     * @param toolCaller 工具调用信息对象，此时已包含：
     *                   - observation: 工具执行的观察结果或输出
     *                   - throwable: 如果执行失败，包含相关的异常信息
     * @see ToolCaller 工具调用者类定义
     */
    default void postTool(ToolCaller toolCaller) {
    }

    /**
     * 在流式响应中处理不完整的推理内容时触发的回调方法
     * <p>
     * 此方法在 ReAct 模式的流式响应解析过程中，当推理内容尚未以换行符结束时被调用。
     * 具体场景如下：
     * </p>
     * <ul>
     *     <li>在 ReAct 模式中，当模型输出 "Thought:" 后，在遇到第一个换行符之前，</li>
     *        持续累积的推理内容会通过此方法回调，此时内容尚不完整</li>
     * </ul>
     * <p>
     * 与 {@link #onModelReasoning(String)} 的区别：
     * </p>
     * <ul>
     *     <li>onAgentReasoning: 当推理内容流中未找到换行符（内容不完整）时被调用</li>
     *     <li>onModelReasoning: 当推理内容流中找到换行符（内容完整）时被调用</li>
     * </ul>
     * <p>
     * 典型使用场景：
     * </p>
     * <ul>
     *     <li>实时展示 Agent 正在进行的思考过程</li>
     *     <li>实现打字机效果的实时输出</li>
     *     <li>监控推理过程的中间状态</li>
     * </ul>
     *
     * @param agentAction 当前接收到的推理内容片段（尚未以换行符结束）
     *                    可能是一个不完整的句子或思考过程
     * @see #onModelReasoning(String) 完整推理内容回调
     * @see tech.smartboot.feat.ai.chat.entity.Message#getReasoningContent() 获取推理内容
     */
    default void onAgentReasoning(String agentAction) {
    }

    /**
     * 在获得完整推理内容后触发的回调方法
     * <p>
     * 此方法在两种场景被调用：
     * </p>
     * <ol>
     *     <li>ReAct 模式流式响应解析中：当推理内容以换行符结束时（即完整的 Thought 内容）</li>
     *     <li>推理模型 API 中：当使用支持推理能力的模型（如 DeepSeek-R1 等）时，
     *         模型在推理内容字段 (reasoning_content) 中输出的内容会通过此方法回调</li>
     * </ol>
     * <p>
     * 典型使用场景：
     * </p>
     * <ul>
     *     <li>记录完整的推理内容用于分析和调试</li>
     *     <li>提取关键的推理步骤或决策点</li>
     *     <li>实现思维链 (Chain of Thought) 的可视化</li>
     *     <li>展示 Agent 对当前问题的分析和解决方案思路</li>
     * </ul>
     *
     * @param agentAction 推理内容字符串，来源不同含义不同：
     *                    - ReAct 模式：完整的 Thought 内容（以换行符结束）
     *                    - 推理模型：模型输出的推理/思考过程
     * @see #onAgentReasoning(String) 不完整推理内容回调
     * @see tech.smartboot.feat.ai.chat.entity.Message#getReasoningContent() 获取推理内容
     */
    default void onModelReasoning(String agentAction) {
    }

    /**
     * 在接收到最终答案内容时触发的回调方法
     * <p>
     * 此方法在 ReAct 模式的流式响应解析中，当模型输出最终答案时被调用。
     * 具体来说，当响应中出现 "\nAI:" 标记后，后续的内容被视为最终答案部分。
     * </p>
     * <p>
     * 典型使用场景：
     * </p>
     * <ul>
     *     <li>实时展示 Agent 给出的最终答案</li>
     *     <li>实现打字机效果的答案输出</li>
     *     <li>对最终答案进行即时处理或转换</li>
     *     <li>日志记录答案内容</li>
     * </ul>
     * <p>
     * 注意：此方法可能被多次调用，每次传递一部分答案内容，
     * 直到完整的响应全部传输完毕。
     * </p>
     *
     * @param content 当前接收到的答案内容片段，通常是文本字符串
     *                多次调用的内容拼接起来构成完整的最终答案
     * @see tech.smartboot.feat.ai.chat.ChatModel#chatStream 流式聊天方法
     * @see tech.smartboot.feat.ai.chat.entity.StreamResponseCallback 流式响应回调接口
     */
    default void onFinalAnswer(String content) {
    }
}
