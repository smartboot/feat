/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.support;

import tech.smartboot.feat.ai.agent.harness.ContextEngine;
import tech.smartboot.feat.ai.agent.harness.Step;
import tech.smartboot.feat.ai.agent.harness.StepContext;
import tech.smartboot.feat.ai.agent.harness.TokenEstimator;
import tech.smartboot.feat.ai.chat.entity.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 前缀稳定的上下文引擎。
 * <p>
 * <b>为什么不能用"从尾部滑窗"</b>：KV-cache 是按**最长公共前缀**命中的。
 * 头部一裁，前缀立刻从第 0 条消息之后就分叉，后面全部要重新 prefill——
 * 也就是说滑动窗口等于"每走一步就把缓存清零一次"，越到长任务越贵。
 * 这类成本不会体现在 token 计数上（用量一样），只会体现在账单和首 token 延迟上。
 * </p>
 * <p>
 * 因此这里的策略是**两段式**：
 * </p>
 * <pre>
 *   [ 冻结前缀 ]  [ 只追加的尾部 ]
 *    系统消息       每步新增的模型/工具消息
 *    省略说明
 *    压缩摘要
 * </pre>
 * <p>
 * 冻结前缀只在真正需要压缩时重建一次，其余每一步都原样复用——所以两次压缩之间，
 * 请求前缀字节一致，缓存可以稳定命中。压缩本身就是一次"缓存全失效"，
 * 因此要**尽量少做、一次多做**：每次至少推进 25% 的增量，而不是每步挤掉一条。
 * </p>
 * <p>
 * 摘要的内容由 {@link Step} 历史结构化生成（不是让模型再写一遍），
 * 并且用滚动要点列表做了上界——第 100 次压缩后的摘要不会比第 1 次更大。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public class StablePrefixContextEngine implements ContextEngine {

    /**
     * 为模型输出预留的 token
     */
    private static final int OUTPUT_RESERVE = 4_096;

    /**
     * 冻结前缀的固定开销估计（说明 + 摘要 + 目标），用于反推尾部预算
     */
    private static final int PREFIX_RESERVE = 1_500;

    /**
     * 滚动要点上限：保证摘要体积有界
     */
    private static final int FACT_LIMIT = 12;

    /**
     * 省略说明。**文本必须恒定**——任何随步数变化的内容（"已省略 N 条"）
     * 都会让前缀在每一步都不同，把缓存彻底打穿。缺失条数对模型也不可行动，
     * 不值得为它付出这个代价。
     */
    private static final String TRIM_MARKER =
            "[CTX-TRIM v1] 更早的对话已折叠为下方的压缩摘要。"
                    + "摘要未覆盖的细节请用工具重新读取原文，不要凭记忆推断。";

    private final int windowTokens;
    private final double compactRatio;
    private final TokenEstimator estimator;

    /**
     * 会话级冻结状态。它只是"视图缓存"，丢了最多多压缩一次，不影响正确性。
     */
    private final Map<String, Frozen> states = new ConcurrentHashMap<String, Frozen>();

    public StablePrefixContextEngine(int windowTokens, double compactRatio) {
        this(windowTokens, compactRatio, TokenEstimator.heuristic());
    }

    public StablePrefixContextEngine(int windowTokens, double compactRatio, TokenEstimator estimator) {
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

    /**
     * 判据是"**当前窗口**塞不下了"，不是"历史太长了"。
     * <p>
     * 这个区别是这套方案成立的前提：折叠引擎的历史永远在长，若用历史做判据，
     * 每步都会压缩一次，前缀缓存全部作废——那就白做了。
     * </p>
     */
    @Override
    public boolean needCompact(StepContext context, int reservedTokens) {
        int budget = (int) (windowTokens * compactRatio);
        return estimator.estimate(assemble(context)) + reservedTokens > budget;
    }

    @Override
    public List<Message> assemble(StepContext context) {
        List<Message> messages = context.getMessages();
        Frozen frozen = states.get(context.getSessionId());
        if (frozen == null) {
            // 还没压缩过：原样返回，历史是纯追加的，前缀自然稳定
            return new ArrayList<Message>(messages);
        }
        List<Message> view = new ArrayList<Message>(frozen.prefix.size() + messages.size());
        view.addAll(frozen.prefix);
        if (frozen.fromIndex < messages.size()) {
            view.addAll(messages.subList(frozen.fromIndex, messages.size()));
        }
        return view;
    }

    @Override
    public List<Message> compact(List<Message> messages, int targetTokens) {
        return compactInternal(null, messages, targetTokens);
    }

    @Override
    public List<Message> compact(StepContext context, int targetTokens) {
        return compactInternal(context, context.getMessages(), targetTokens);
    }

    @Override
    public void onRunFinished(String sessionId) {
        states.remove(sessionId);
    }

    private List<Message> compactInternal(StepContext context, List<Message> messages, int targetTokens) {
        String sessionId = context == null ? "" : context.getSessionId();
        Frozen prev = states.get(sessionId);
        int from = prev == null ? 0 : prev.fromIndex;
        int size = messages == null ? 0 : messages.size();
        int budget = Math.max(1_024, targetTokens);

        if (from >= size) {
            return assemble(context == null ? emptyContext() : context);
        }

        // 1) 至少推进 25%（且不少于 4 条）：压缩本身就是一次缓存失效，做得越碎越亏
        int newFrom = Math.min(size, from + Math.max(4, (size - from) / 4));

        // 2) 尾部仍超预算就继续推进，直到塞得下
        while (newFrom < size && estimateRange(messages, newFrom, size) > budget - PREFIX_RESERVE) {
            int next = newFrom + Math.max(1, (size - newFrom) / 4);
            if (next == newFrom) {
                break;
            }
            newFrom = Math.min(size, next);
        }

        // 3) 尾部不能以 tool 消息开头，否则与已被折叠的 assistant(tool_calls) 拆开配对
        while (newFrom < size && Message.ROLE_TOOL.equals(messages.get(newFrom).getRole())) {
            newFrom++;
        }

        // 4) 生成摘要：滚动要点 + 任务目标 + 计数
        List<String> facts = mergeFacts(prev == null ? Collections.<String>emptyList() : prev.facts,
                extractFacts(context, prev));
        String digest = buildDigest(context, prev, facts, newFrom, size);
        List<Message> prefix = buildPrefix(messages, digest);

        Frozen frozen = new Frozen(prev, prefix, newFrom, facts,
                context == null || context.getHistory() == null ? 0 : context.getHistory().size());
        states.put(sessionId, frozen);

        List<Message> view = new ArrayList<Message>(prefix.size() + size - newFrom);
        view.addAll(prefix);
        view.addAll(messages.subList(newFrom, size));
        return view;
    }

    private StepContext emptyContext() {
        return StepContext.empty(null);
    }

    /**
     * 冻结前缀 = 任务原文（若有）+ 省略说明与摘要。
     * <p>
     * 任务原文单独保留是刻意的：它是最不能被摘要替代的东西，而且内容恒定，
     * 放在前缀里不花钱。
     * </p>
     */
    private List<Message> buildPrefix(List<Message> messages, String digest) {
        List<Message> prefix = new ArrayList<Message>(2);
        if (messages != null && !messages.isEmpty()
                && Message.ROLE_USER.equals(messages.get(0).getRole())) {
            prefix.add(messages.get(0));
        }
        prefix.add(Message.ofSystem(TRIM_MARKER + "\n\n" + digest));
        return prefix;
    }

    private String buildDigest(StepContext context, Frozen prev, List<String> facts,
                               int folded, int total) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("## 压缩摘要 v1（第 ").append(prev == null ? 1 : prev.generation + 1).append(" 次）\n");
        String goal = context == null ? null : context.getInput();
        sb.append("任务目标：").append(goal == null || goal.isEmpty() ? "（未记录）" : truncate(goal, 200)).append('\n');
        sb.append("进度：已完成 ").append(context == null || context.getHistory() == null
                ? 0 : context.getHistory().size()).append(" 步；已折叠前 ").append(folded)
                .append(" 条消息，保留最近 ").append(Math.max(0, total - folded)).append(" 条。\n");
        if (facts.isEmpty()) {
            sb.append("要点：（无）\n");
        } else {
            sb.append("要点：\n");
            for (String fact : facts) {
                sb.append("- ").append(fact).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 从结构化历史里提取"上一步之后新发生的事"，转成一行一句的要点
     */
    private List<String> extractFacts(StepContext context, Frozen prev) {
        if (context == null || context.getHistory() == null) {
            return Collections.emptyList();
        }
        List<Step> history = context.getHistory();
        int cursor = prev == null ? 0 : Math.min(prev.historyCursor, history.size());
        List<String> facts = new ArrayList<String>();
        for (int i = cursor; i < history.size(); i++) {
            Step step = history.get(i);
            switch (step.getType()) {
                case TOOL_CALL:
                    facts.add("调用 " + step.getToolName() + "("
                            + truncate(step.getArguments() == null ? "{}" : step.getArguments().toJSONString(), 80)
                            + ")");
                    break;
                case FINAL:
                    facts.add("给出结论：" + truncate(step.getContent(), 120));
                    break;
                default:
                    if (step.getThought() != null && !step.getThought().isEmpty()) {
                        facts.add("推理：" + truncate(step.getThought(), 80));
                    }
                    break;
            }
        }
        return facts;
    }

    private List<String> mergeFacts(List<String> previous, List<String> fresh) {
        if (fresh.isEmpty()) {
            return new ArrayList<String>(previous);
        }
        List<String> merged = new ArrayList<String>(previous.size() + fresh.size());
        merged.addAll(previous);
        merged.addAll(fresh);
        if (merged.size() > FACT_LIMIT) {
            merged = new ArrayList<String>(merged.subList(merged.size() - FACT_LIMIT, merged.size()));
        }
        return merged;
    }

    private int estimateRange(List<Message> messages, int from, int to) {
        if (from >= to) {
            return 0;
        }
        return estimator.estimate(messages.subList(from, to));
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String flat = text.replace('\n', ' ').trim();
        return flat.length() <= max ? flat : flat.substring(0, max) + "…";
    }

    /**
     * 会话级冻结视图
     */
    private static final class Frozen {
        private final List<Message> prefix;
        private final int fromIndex;
        private final List<String> facts;
        private final int historyCursor;
        private final int generation;

        private Frozen(Frozen prev, List<Message> prefix, int fromIndex, List<String> facts,
                       int historyCursor) {
            this.prefix = Collections.unmodifiableList(prefix);
            this.fromIndex = fromIndex;
            this.facts = Collections.unmodifiableList(facts);
            this.historyCursor = historyCursor;
            this.generation = prev == null ? 1 : prev.generation + 1;
        }
    }
}
