/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import java.util.concurrent.CompletableFuture;

/**
 * 人工审批出口。
 * <p>
 * 刻意做成异步：审批可能来自 Web 弹窗、企业微信卡片、甚至运维工单，
 * 阻塞式回调会把整个 agent 线程挂死。约定**超时即拒绝**，
 * 因为"不确定"在生产里等价于"不允许"。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public interface ApprovalGate {

    /**
     * @param invocation 待审批的工具调用
     * @param reason     策略给出的审批理由（可直接展示给用户）
     * @return true 放行，false 拒绝
     */
    CompletableFuture<Boolean> approve(ToolInvocation invocation, String reason);

    /**
     * 一律拒绝（安全默认值）
     */
    ApprovalGate DENY_ALL = (invocation, reason) -> CompletableFuture.completedFuture(Boolean.FALSE);

    /**
     * 一律放行（仅建议在受控测试环境使用）
     */
    ApprovalGate ALLOW_ALL = (invocation, reason) -> CompletableFuture.completedFuture(Boolean.TRUE);
}
