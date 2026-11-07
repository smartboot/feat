/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.example;

import tech.smartboot.feat.ai.agent.harness.AgentLoop;
import tech.smartboot.feat.ai.agent.harness.Step;
import tech.smartboot.feat.ai.agent.harness.StepContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 脚本化循环：按预置顺序吐步，模拟"模型的决策"。
 * <p>
 * 存在的意义是让示例**不依赖 API key 就能跑**。真实使用时用
 * {@code NativeToolCallLoop}，本类只用于演示 harness 这一侧的行为
 * （审批、验证、预算、事件流），把模型变成可复现的输入。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class ScriptedLoop implements AgentLoop {

    private final String name;
    private final List<Step> steps;
    private final AtomicInteger cursor = new AtomicInteger();

    public ScriptedLoop(String name, List<Step> steps) {
        this.name = name;
        this.steps = Collections.unmodifiableList(new ArrayList<Step>(steps));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletableFuture<Step> next(StepContext context) {
        int index = cursor.getAndIncrement();
        if (index >= steps.size()) {
            // 脚本用尽仍被追问：给个结论收场，别把场景耗到预算上限，
            // 否则示例输出会被 MAX_STEPS 淹没，看不出真正要看的行为
            return CompletableFuture.completedFuture(
                    Step.finalAnswer("脚本已用尽", "（脚本已用尽，无更多动作）", null));
        }
        return CompletableFuture.completedFuture(steps.get(index));
    }
}
