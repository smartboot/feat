/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

/**
 * 一次验证的结论。
 * <p>
 * 刻意分成三态而不是布尔：{@link Status#SKIP} 表示"验证器自己没法判断"
 * （命令不存在、环境缺失、验证器抛异常），它必须被当作**放行**而不是否决——
 * 否则基础设施抖动会把本已正确的答案判成失败，这是最坏的一种误杀。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class VerifyResult {

    public enum Status {
        /**
         * 验证通过
         */
        PASS,
        /**
         * 验证未通过，反馈会回灌给模型重做
         */
        FAIL,
        /**
         * 弃权：无法判断，按通过处理
         */
        SKIP
    }

    private final Status status;
    private final String verifier;
    private final String feedback;
    private final long durationMillis;

    private VerifyResult(Status status, String verifier, String feedback, long durationMillis) {
        this.status = status;
        this.verifier = verifier;
        this.feedback = feedback;
        this.durationMillis = durationMillis;
    }

    public static VerifyResult pass(String verifier, String detail, long durationMillis) {
        return new VerifyResult(Status.PASS, verifier, detail, durationMillis);
    }

    public static VerifyResult fail(String verifier, String feedback, long durationMillis) {
        return new VerifyResult(Status.FAIL, verifier, feedback, durationMillis);
    }

    public static VerifyResult skip(String verifier, String reason) {
        return new VerifyResult(Status.SKIP, verifier, reason, 0L);
    }

    /**
     * 是否放行（PASS 与 SKIP 都放行）
     */
    public boolean isPassed() {
        return status != Status.FAIL;
    }

    /**
     * 是否构成阻塞（只有 FAIL 构成阻塞）
     */
    public boolean isBlocking() {
        return status == Status.FAIL;
    }

    public Status getStatus() {
        return status;
    }

    public String getVerifier() {
        return verifier;
    }

    public String getFeedback() {
        return feedback;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}
