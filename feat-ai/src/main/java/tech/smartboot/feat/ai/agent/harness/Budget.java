/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

/**
 * 四维运行预算：步数、工具调用数、token、墙钟时长。
 * <p>
 * 单次 run 一份实例，不做全局共享，避免并发会话互相扣减。
 * 除了"兜底防止无限烧钱"，预算更重要的作用是**给熔断器提供判断依据**：
 * 接近上限时应当先纠偏（提示模型收敛），而不是等它撞墙直接失败。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class Budget {

    private final int maxSteps;
    private final int maxToolCalls;
    private final int maxTokens;
    private final long maxMillis;

    private final long startNanos = System.nanoTime();
    private int steps;
    private int toolCalls;
    private int tokens;

    public Budget(int maxSteps, int maxToolCalls, int maxTokens, long maxMillis) {
        this.maxSteps = maxSteps;
        this.maxToolCalls = maxToolCalls;
        this.maxTokens = maxTokens;
        this.maxMillis = maxMillis;
    }

    public static Budget unlimited() {
        return new Budget(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE);
    }

    public void recordStep(int stepTokens) {
        steps++;
        tokens += Math.max(0, stepTokens);
    }

    public void recordTokens(int delta) {
        tokens += Math.max(0, delta);
    }

    public void recordToolCall() {
        toolCalls++;
    }

    public boolean isExhausted() {
        return exhaustedReason() != null;
    }

    /**
     * @return 触顶原因，未触顶返回 null
     */
    public String exhaustedReason() {
        if (steps >= maxSteps) {
            return "步数预算耗尽：" + steps + "/" + maxSteps;
        }
        if (toolCalls >= maxToolCalls) {
            return "工具调用预算耗尽：" + toolCalls + "/" + maxToolCalls;
        }
        if (tokens >= maxTokens) {
            return "token 预算耗尽：" + tokens + "/" + maxTokens;
        }
        if (elapsedMillis() >= maxMillis) {
            return "时长预算耗尽：" + elapsedMillis() + "ms/" + maxMillis + "ms";
        }
        return null;
    }

    /**
     * 剩余步数比例，用于熔断器提前纠偏
     */
    public double stepRemainRatio() {
        if (maxSteps == Integer.MAX_VALUE) {
            return 1.0d;
        }
        return Math.max(0d, 1.0d - (double) steps / maxSteps);
    }

    public long elapsedMillis() {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public int getSteps() {
        return steps;
    }

    public int getToolCalls() {
        return toolCalls;
    }

    public int getTokens() {
        return tokens;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public int getMaxToolCalls() {
        return maxToolCalls;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public long getMaxMillis() {
        return maxMillis;
    }
}
