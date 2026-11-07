/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.support;

import tech.smartboot.feat.ai.agent.harness.ContextEngine;
import tech.smartboot.feat.ai.agent.harness.StepContext;
import tech.smartboot.feat.ai.agent.harness.TokenEstimator;
import tech.smartboot.feat.ai.chat.entity.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 滑动窗口上下文：P0 的默认实现，先解决"爆窗"，再谈"摘要"。
 * <p>
 * 策略很朴素——保留全部 system 消息，其余历史从尾部向前累计到预算为止。
 * 两个必须处理的细节：
 * <ol>
 *     <li>不能把 tool 消息与它的 assistant(tool_calls) 消息拆开，否则原生协议会报配对错误；</li>
 *     <li>裁剪后要显式告诉模型"前面有内容被省略"，避免它误以为任务刚开始。</li>
 * </ol>
 * </p>
 * <p>
 * <b>注意它天生对 KV-cache 不友好</b>：从头部裁剪会让请求前缀每一步都重新分叉。
 * 这里只做了两件止损的事——省略说明的文本恒定（不含"省略了 N 条"这类变动数字），
 * 裁剪边界按 {@link #CUT_GRANULARITY} 量化（边界只在跨过整块时才移动）。
 * 需要长任务与成本敏感时请用 {@link StablePrefixContextEngine}。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public class SlidingWindowContextEngine implements ContextEngine {

    /**
     * 为模型输出预留的 token
     */
    private static final int OUTPUT_RESERVE = 4_096;

    /**
     * 裁剪边界量化粒度：边界只按整块移动，避免每加一条消息就整体前移一格
     */
    private static final int CUT_GRANULARITY = 8;

    /**
     * 省略说明。文本恒定是刻意的——见类注释。
     */
    private static final String TRIM_MARKER =
            "[CTX-TRIM v1] 更早的消息已省略。需要细节请用工具重新读取，不要凭记忆推断。";

    private final int windowTokens;
    private final double compactRatio;
    private final TokenEstimator estimator;

    public SlidingWindowContextEngine(int windowTokens, double compactRatio) {
        this(windowTokens, compactRatio, TokenEstimator.heuristic());
    }

    public SlidingWindowContextEngine(int windowTokens, double compactRatio, TokenEstimator estimator) {
        this.windowTokens = windowTokens;
        this.compactRatio = compactRatio <= 0 || compactRatio >= 1 ? 0.7d : compactRatio;
        this.estimator = estimator;
    }

    @Override
    public int windowTokens() {
        return windowTokens;
    }

    @Override
    public double compactRatio() {
        return compactRatio;
    }

    @Override
    public List<Message> assemble(StepContext context) {
        return trim(context.getMessages(), usableBudget());
    }

    @Override
    public List<Message> compact(List<Message> messages, int targetTokens) {
        return trim(messages, Math.max(1_024, targetTokens));
    }

    private int usableBudget() {
        return Math.max(1_024, (int) (windowTokens * compactRatio) - OUTPUT_RESERVE);
    }

    private List<Message> trim(List<Message> messages, int budget) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<Message>();
        }
        int used = 0;
        int cut = messages.size();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            int cost = estimator.estimate(message);
            if (used + cost > budget) {
                break;
            }
            used += cost;
            cut = i;
        }
        // 边界量化：只按整块移动，减少前缀被打断的频率
        if (cut > 0 && cut < messages.size()) {
            cut = Math.min(messages.size(), ((cut + CUT_GRANULARITY - 1) / CUT_GRANULARITY) * CUT_GRANULARITY);
        }
        // 保证 tool 消息不与其配对的 assistant 消息分离
        while (cut > 0 && cut < messages.size() && Message.ROLE_TOOL.equals(messages.get(cut).getRole())) {
            cut--;
            used += estimator.estimate(messages.get(cut));
            if (used > budget) {
                cut++;
                break;
            }
        }

        List<Message> window = new ArrayList<Message>();
        for (Message message : messages) {
            if (Message.ROLE_SYSTEM.equals(message.getRole())) {
                window.add(message);
            }
        }
        if (cut > 0) {
            window.add(Message.ofSystem(TRIM_MARKER));
        }
        window.addAll(messages.subList(cut, messages.size()));
        return window;
    }
}
