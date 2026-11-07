/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import tech.smartboot.feat.ai.chat.entity.Usage;

/**
 * 一次 harness 运行的终态。
 * <p>
 * {@link RunStatus} 是调用方唯一需要分支判断的字段：
 * 它把「模型没答出来」「预算烧完了」「被审批拦住了」「崩了」区分开，
 * 上层可以据此决定重试、加预算、换策略还是转人工，而不是笼统地当成失败。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class HarnessResult {

    public enum RunStatus {
        /**
         * 模型给出了最终答案
         */
        SUCCESS,
        /**
         * 步数预算耗尽
         */
        MAX_STEPS,
        /**
         * token / 时长 / 工具调用数预算耗尽
         */
        BUDGET_EXCEEDED,
        /**
         * 外部取消
         */
        CANCELLED,
        /**
         * 关键动作被审批拒绝且模型未能绕开
         */
        DENIED,
        /**
         * 模型给出了答案，但机械验证始终未通过且已达重试上限。
         * 与 {@link #SUCCESS} 区分开的意义在于：答案是"有保留的可用"，
         * 调用方可以选择人工复核，而不是把它当成已验收的成品。
         */
        UNVERIFIED,
        /**
         * 不可恢复异常或熔断终止
         */
        ERROR
    }

    private final String sessionId;
    private final RunStatus status;
    private final String answer;
    private final String stopDetail;
    private final int steps;
    private final int toolCalls;
    private final Usage usage;
    private final long startedAt;
    private final long finishedAt;
    private final Throwable error;
    /**
     * 答案是否经过机械验证（未配置验证器时视为 true）
     */
    private final boolean verified;

    public HarnessResult(String sessionId, RunStatus status, String answer, String stopDetail,
                         int steps, int toolCalls, Usage usage,
                         long startedAt, long finishedAt, Throwable error, boolean verified) {
        this.sessionId = sessionId;
        this.status = status;
        this.answer = answer;
        this.stopDetail = stopDetail;
        this.steps = steps;
        this.toolCalls = toolCalls;
        this.usage = usage;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.error = error;
        this.verified = verified;
    }

    public static HarnessResult success(String sessionId, String answer, int steps, int toolCalls,
                                        Usage usage, long startedAt) {
        return success(sessionId, answer, steps, toolCalls, usage, startedAt, true);
    }

    public static HarnessResult success(String sessionId, String answer, int steps, int toolCalls,
                                        Usage usage, long startedAt, boolean verified) {
        return new HarnessResult(sessionId, RunStatus.SUCCESS, answer, null, steps, toolCalls,
                usage, startedAt, System.currentTimeMillis(), null, verified);
    }

    /**
     * 有答案但未通过验证：答案照常返回，只是标记为未验收
     */
    public static HarnessResult unverified(String sessionId, String answer, String stopDetail,
                                           int steps, int toolCalls, Usage usage, long startedAt) {
        return new HarnessResult(sessionId, RunStatus.UNVERIFIED, answer, stopDetail, steps, toolCalls,
                usage, startedAt, System.currentTimeMillis(), null, false);
    }

    public static HarnessResult failed(String sessionId, RunStatus status, String stopDetail,
                                       int steps, int toolCalls, Usage usage, long startedAt, Throwable error) {
        return new HarnessResult(sessionId, status, null, stopDetail, steps, toolCalls,
                usage, startedAt, System.currentTimeMillis(), error, false);
    }

    public boolean isSuccess() {
        return status == RunStatus.SUCCESS;
    }

    /**
     * 是否有可直接交付的答案（成功或未验收但给出了答案）
     */
    public boolean hasAnswer() {
        return answer != null && !answer.isEmpty();
    }

    public boolean isVerified() {
        return verified;
    }

    /**
     * 是否可以通过追加预算继续跑（对调用方的重试决策有用）。
     * <p>
     * {@link RunStatus#UNVERIFIED} 不在此列：它不是一个"差一点就成功"的中间态，
     * 而是一个明确结论——答案在，验收没通过。继续重跑同样的配置只会得到同样的结果，
     * 要改的是验证环境或任务本身。
     * </p>
     */
    public boolean isResumable() {
        return status == RunStatus.MAX_STEPS
                || status == RunStatus.BUDGET_EXCEEDED
                || status == RunStatus.CANCELLED
                || status == RunStatus.DENIED;
    }

    public String getSessionId() {
        return sessionId;
    }

    public RunStatus getStatus() {
        return status;
    }

    public String getAnswer() {
        return answer;
    }

    public String getStopDetail() {
        return stopDetail;
    }

    public int getSteps() {
        return steps;
    }

    public int getToolCalls() {
        return toolCalls;
    }

    public Usage getUsage() {
        return usage;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public long getDurationMillis() {
        return finishedAt - startedAt;
    }

    public Throwable getError() {
        return error;
    }
}
