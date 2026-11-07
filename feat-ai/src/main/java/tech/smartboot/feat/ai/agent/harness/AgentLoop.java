/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import java.util.concurrent.CompletableFuture;

/**
 * 单步决策协议。
 * <p>
 * 这是 harness 里唯一"懂模型协议"的组件。它只回答一个问题：**下一步打算干什么**。
 * 工具怎么执行、预算还剩多少、历史怎么裁剪，都不属于它的职责。
 * </p>
 * <p>
 * 内置规划中的两个实现：
 * <ul>
 *     <li>{@code NativeToolCallLoop}：使用 {@code ChatModel.chat(List<Message>, List<Tool>)} 的原生 tool_calls，
 *     结构由 provider 保证，解析失败率最低（推荐）</li>
 *     <li>{@code TextReActLoop}：复用 {@code feat_react_agent.tpl} 的文本协议，兼容不支持 tools 的网关</li>
 * </ul>
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public interface AgentLoop {

    /**
     * 循环实现名称，用于事件流与排查
     */
    String name();

    /**
     * 是否走原生 tool_calls 协议
     */
    default boolean nativeToolCalling() {
        return false;
    }

    /**
     * 决策下一步。
     * <p>
     * 实现方需要保证：不修改 {@code context} 中的历史；工具执行与预算扣减由 harness 负责。
     * 网络异常应通过 future 异常完成，由 harness 统一转为 {@link HarnessResult.RunStatus#ERROR}。
     * </p>
     *
     * @param context 只读步进上下文
     * @return 结构化的一步
     */
    CompletableFuture<Step> next(StepContext context);
}
