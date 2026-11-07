/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

/**
 * 结构化工具结果。
 * <p>
 * 与现有 {@code AgentTool} 直接返回字符串的最大区别：失败也是一等公民。
 * {@code DENIED} / {@code TIMEOUT} / {@code INVALID_ARGUMENT} 都会被回喂给模型，
 * 让它有机会自己换一条路——这比抛异常终止 run 有效得多，
 * 也是"Agent 看起来会自我纠错"的真实来源。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class ToolResult {

    public enum Status {
        OK,
        ERROR,
        DENIED,
        TIMEOUT,
        INVALID_ARGUMENT
    }

    private final Status status;
    private final String output;
    private final String error;
    private final long durationMillis;
    private final boolean truncated;
    private final String artifactRef;

    private ToolResult(Status status, String output, String error, long durationMillis,
                       boolean truncated, String artifactRef) {
        this.status = status;
        this.output = output;
        this.error = error;
        this.durationMillis = durationMillis;
        this.truncated = truncated;
        this.artifactRef = artifactRef;
    }

    public static ToolResult ok(String output, long durationMillis, boolean truncated, String artifactRef) {
        return new ToolResult(Status.OK, output, null, durationMillis, truncated, artifactRef);
    }

    public static ToolResult error(String error, long durationMillis) {
        return new ToolResult(Status.ERROR, null, error, durationMillis, false, null);
    }

    public static ToolResult denied(String reason) {
        return new ToolResult(Status.DENIED, null, reason, 0L, false, null);
    }

    public static ToolResult timeout(long timeoutMillis) {
        return new ToolResult(Status.TIMEOUT, null, "工具执行超时（" + timeoutMillis + "ms）",
                timeoutMillis, false, null);
    }

    public static ToolResult invalidArgument(String reason) {
        return new ToolResult(Status.INVALID_ARGUMENT, null, reason, 0L, false, null);
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    /**
     * 转换为回喂模型的消息内容：失败也要给出可操作的提示，而不只是错误码
     */
    public String toObservation() {
        if (isOk()) {
            if (truncated) {
                return output + "\n\n[输出已截断，全文见 " + artifactRef + "]";
            }
            return output;
        }
        switch (status) {
            case DENIED:
                return "该操作被拒绝：" + error + "。请调整方案后重试，或向用户说明需要什么权限。";
            case TIMEOUT:
                return error + "。请缩小操作范围或换用更轻量的方式。";
            case INVALID_ARGUMENT:
                return "参数不合法：" + error + "。请按工具的参数 schema 重新构造参数。";
            default:
                return "工具执行失败：" + error;
        }
    }

    public Status getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public String getArtifactRef() {
        return artifactRef;
    }
}
