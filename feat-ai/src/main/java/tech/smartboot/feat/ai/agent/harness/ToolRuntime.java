/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import tech.smartboot.feat.ai.agent.AgentTool;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 工具运行时：把 {@code AgentTool} 从"可调用函数"升级为"受治理的能力"。
 * <p>
 * 统一承担六件事，任何一件都不该由工具实现自己操心：
 * 注册与元信息、政策判定、参数校验、超时、输出截断与引用化、结果结构化。
 * </p>
 * <p>
 * 参考实现见 {@code support.SimpleToolRuntime}。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public interface ToolRuntime {

    /**
     * 当前可用工具（受熔断影响：被禁用的工具不应再出现在这里）
     */
    List<ToolSpec> specs();

    /**
     * 执行一次工具调用。实现必须保证不抛异常，所有失败都转成 {@link ToolResult}。
     */
    CompletableFuture<ToolResult> invoke(ToolInvocation invocation);

    void register(AgentTool tool, ToolSpec spec);

    /**
     * 熔断：本 run 内禁用某工具
     */
    default void disable(String toolName, String reason) {
    }

    default boolean isDisabled(String toolName) {
        return false;
    }
}
