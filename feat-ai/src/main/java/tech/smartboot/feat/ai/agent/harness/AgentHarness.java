/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.harness.HarnessResult.RunStatus;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.ai.chat.entity.Usage;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Agent Harness 编排壳。
 * <p>
 * 它是整个运行期**唯一知道"现在第几步、预算还剩多少、该不该收手"的角色**。
 * 与之配套的职责切分：
 * <ul>
 *     <li>{@link AgentLoop} 只回答"下一步想干什么"；</li>
 *     <li>{@link ToolRuntime} 只回答"这一步干成了没有"；</li>
 *     <li>{@link ContextEngine} 只回答"这一轮模型能看到什么"；</li>
 *     <li>{@link SessionStore} 只负责"把发生过的事记下来"。</li>
 * </ul>
 * </p>
 * <p>
 * 三个工程约束：
 * <ol>
 *     <li>递归改异步链，取消在每次 step 边界检查，不再依赖布尔标志穿透递归栈；</li>
 *     <li>任何异常都收敛成 {@link HarnessResult}，调用方不需要 try-catch 一堆 Throwable；</li>
 *     <li>观测失败不影响主流程——事件写盘异常只记日志。</li>
 * </ol>
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class AgentHarness {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentHarness.class.getName());

    /**
     * 为模型输出预留的 token：压缩判定必须把它算进去，否则刚好塞满的窗口会让模型无话可说
     */
    private static final int OUTPUT_RESERVE = 4_096;

    private final HarnessOptions options;
    private final Map<String, RunContext> running = new ConcurrentHashMap<String, RunContext>();

    public AgentHarness(HarnessOptions options) {
        if (options.getLoop() == null) {
            throw new FeatException("HarnessOptions.loop 未配置");
        }
        if (options.getToolRuntime() == null) {
            throw new FeatException("HarnessOptions.toolRuntime 未配置");
        }
        this.options = options;
    }

    public HarnessResult run(HarnessRequest request) {
        try {
            return runAsync(request).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FeatException("运行被中断", e);
        } catch (ExecutionException e) {
            throw new FeatException("运行失败", e.getCause());
        }
    }

    public CompletableFuture<HarnessResult> runAsync(HarnessRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            return failedFuture("sessionId 不能为空：resume / replay / 事件归集都依赖它");
        }
        if (running.containsKey(request.getSessionId())) {
            return failedFuture("会话已在运行：" + request.getSessionId());
        }
        RunContext ctx = new RunContext(request, options.newBudget());
        running.put(request.getSessionId(), ctx);

        emit(ctx, AgentEvent.Type.RUN_STARTED, 0, new JSONObject().fluentPut("input", request.getInput()));
        options.getHook().preCall(request.getInput());
        ctx.messages.add(Message.ofUser(request.getInput()));
        return step(ctx);
    }

    /**
     * 取消：在下一个 step 边界生效。工具执行中的取消由 ToolRuntime 的 future 传播。
     */
    public void cancel(String sessionId) {
        RunContext ctx = running.get(sessionId);
        if (ctx != null) {
            ctx.cancelled = true;
            LOGGER.info("会话 {} 收到取消信号", sessionId);
        }
    }

    /**
     * 恢复运行。
     * <p>
     * 当前实现为"best-effort 重建"：从事件流还原出可直接喂给模型的文本轨迹，
     * 足以让文本协议循环继续；原生 tool_calls 的精确配对（assistant.tool_calls ↔ tool.tool_call_id）
     * 需要事件里保留原始 tool_call 结构，留待 P1 补齐。
     * </p>
     */
    public CompletableFuture<HarnessResult> resumeAsync(String sessionId) {
        List<AgentEvent> events = options.getSessionStore().load(sessionId);
        if (events == null || events.isEmpty()) {
            return failedFuture("会话不存在或事件流为空：" + sessionId);
        }
        if (running.containsKey(sessionId)) {
            return failedFuture("会话仍在运行，无法 resume：" + sessionId);
        }
        RunContext ctx = new RunContext(HarnessRequest.of(sessionId, ""), options.newBudget());
        replay(events, ctx);
        running.put(sessionId, ctx);
        emit(ctx, AgentEvent.Type.RUN_STARTED, ctx.budget.getSteps(), new JSONObject().fluentPut("resume", true));
        return step(ctx);
    }

    /**
     * 从事件流重建消息轨迹（只保留决策相关信息）
     */
    private void replay(List<AgentEvent> events, RunContext ctx) {
        for (AgentEvent event : events) {
            JSONObject payload = event.getPayload();
            switch (event.getType()) {
                case MODEL_RESPONDED:
                    String tool = payload.getString("tool");
                    String thought = payload.getString("thought");
                    if (tool != null) {
                        ctx.messages.add(Message.ofAssistant("Thought: " + thought + "\nAction: " + tool
                                + "\nAction Input: " + payload.getString("arguments")));
                    } else if (payload.getString("content") != null) {
                        ctx.messages.add(Message.ofAssistant(payload.getString("content")));
                    }
                    break;
                case TOOL_COMPLETED:
                    ctx.messages.add(Message.ofTool(payload.getString("callId"),
                            payload.getString("observation")));
                    break;
                default:
                    break;
            }
        }
        ctx.budget.recordStep(0);
        LOGGER.info("会话 {} 恢复完成，重放 {} 条事件", ctx.sessionId, events.size());
    }

    // ==================== 步进主循环 ====================

    private CompletableFuture<HarnessResult> step(RunContext ctx) {
        if (ctx.cancelled) {
            return completed(finish(ctx, RunStatus.CANCELLED, "外部取消", null));
        }
        String exhausted = ctx.budget.exhaustedReason();
        if (exhausted != null) {
            RunStatus status = exhausted.startsWith("步数") ? RunStatus.MAX_STEPS : RunStatus.BUDGET_EXCEEDED;
            return completed(finish(ctx, status, exhausted, null));
        }

        StepContext stepContext = buildContext(ctx);
        return options.getLoop().next(stepContext)
                .handle((step, error) -> error != null
                        ? completed(finish(ctx, RunStatus.ERROR, "模型调用失败", error))
                        : onStep(ctx, step))
                .thenCompose(future -> future);
    }

    private StepContext buildContext(RunContext ctx) {
        List<Message> messages = Collections.unmodifiableList(new ArrayList<Message>(ctx.messages));
        StepContext probe = new StepContext(ctx.sessionId, ctx.input, messages, messages,
                options.getToolRuntime().specs(), ctx.budget, ctx.budget.getSteps(),
                Collections.unmodifiableList(new ArrayList<Step>(ctx.history)), options);

        boolean willCompact = options.getContextEngine().needCompact(probe, reservedTokens());
        // 先判断是否压缩，再组装视图：反过来的话压缩结果会被 assemble 覆盖，白做一遍
        if (willCompact) {
            options.getContextEngine().compact(probe, (int) (options.getContextEngine().windowTokens() * 0.5));
        }
        List<Message> window = options.getContextEngine().assemble(probe);
        if (willCompact) {
            emit(ctx, AgentEvent.Type.CONTEXT_COMPACTED, ctx.budget.getSteps(),
                    new JSONObject().fluentPut("before", messages.size())
                            .fluentPut("after", window.size()));
        }
        trackPrefix(ctx, window);
        StepContext stepContext = new StepContext(ctx.sessionId, ctx.input, messages, window,
                options.getToolRuntime().specs(), ctx.budget, ctx.budget.getSteps(),
                Collections.unmodifiableList(new ArrayList<Step>(ctx.history)), options);
        // 验证器需要看到"模型实际看到的那份视图"，复用它可以省掉一次重复组装，
        // 也避免多记录一条无意义的前缀指纹
        ctx.lastStepContext = stepContext;
        return stepContext;
    }

    /**
     * 输出预留量。
     * <p>
     * 必须夹到窗口的 25% 以内：预留量一旦大于可用预算，
     * {@code needCompact} 会永远为真——于是每一步都压缩一次，
     * 等于每一步都把前缀缓存清空，比不压缩还糟。小窗口场景（本地小模型）很容易踩到。
     * </p>
     */
    private int reservedTokens() {
        int usable = (int) (options.getContextEngine().windowTokens()
                * options.getContextEngine().compactRatio());
        return Math.max(256, Math.min(OUTPUT_RESERVE, usable / 4));
    }

    /**
     * 记录前缀指纹：**只在变化时发事件**。
     * <p>
     * 于是"CONTEXT_ASSEMBLED 出现了几次"就等于"缓存被打断了几次"，
     * 这是长任务成本最直接的观测指标，比事后看账单快得多。
     * </p>
     */
    private void trackPrefix(RunContext ctx, List<Message> window) {
        String signature = ContextSignature.of(window);
        if (signature.equals(ctx.lastPrefixSig)) {
            return;
        }
        boolean changed = ctx.lastPrefixSig != null;
        ctx.lastPrefixSig = signature;
        emit(ctx, AgentEvent.Type.CONTEXT_ASSEMBLED, ctx.budget.getSteps(),
                new JSONObject().fluentPut("signature", signature)
                        .fluentPut("changed", changed)
                        .fluentPut("windowMessages", window.size())
                        .fluentPut("estimatedTokens", TokenEstimator.heuristic().estimate(window)));
    }

    private CompletableFuture<HarnessResult> onStep(RunContext ctx, Step step) {
        ctx.history.add(step);
        ctx.budget.recordStep(step.totalTokens());
        ctx.consecutiveThink = step.getType() == Step.Type.THINK ? ctx.consecutiveThink + 1 : 0;

        JSONObject payload = new JSONObject()
                .fluentPut("type", step.getType().name())
                .fluentPut("thought", step.getThought())
                .fluentPut("tokens", step.totalTokens());
        if (step.getType() == Step.Type.TOOL_CALL) {
            payload.fluentPut("tool", step.getToolName())
                    .fluentPut("arguments", step.getArguments() == null ? null : step.getArguments().toJSONString());
        } else {
            payload.fluentPut("content", step.getContent());
        }
        emit(ctx, AgentEvent.Type.MODEL_RESPONDED, ctx.budget.getSteps(), payload);
        options.getHook().onModelReasoning(step.getThought());

        switch (step.getType()) {
            case FINAL:
                return onFinal(ctx, step);
            case TOOL_CALL:
                return onToolCall(ctx, step);
            default:
                return onThink(ctx, step);
        }
    }

    /**
     * 最终答案的验收闸门。
     * <p>
     * 模型说"我做完了"不等于做完了。这里在 ACK 之前插一道机械验证：
     * 测试、编译、lint 的退出码是外部事实，比模型的自我评价可信得多。
     * 不过就把验证输出原样回灌，让它带着具体错误重做——而不是笼统地说"再检查一下"。
     * </p>
     * <p>
     * 三道兜底：
     * <ol>
     *     <li>验证器抛异常 → 视为弃权（SKIP 放行），不能因为验证设施故障误杀正确答案；</li>
     *     <li>连续否决达上限 → 以 {@link RunStatus#UNVERIFIED} 收尾，答案照常返回但标记未验收；</li>
     *     <li>未配置验证器 → 保持旧行为，直接成功。</li>
     * </ol>
     * </p>
     */
    private CompletableFuture<HarnessResult> onFinal(RunContext ctx, Step step) {
        String answer = step.getContent();
        ctx.messages.add(Message.ofAssistant(answer));

        if (options.getVerifier() == Verifier.NONE) {
            ctx.verified = true;
            options.getHook().postCall(answer);
            return completed(finish(ctx, RunStatus.SUCCESS, null, null));
        }
        if (ctx.verifyAttempts >= options.getMaxVerifyAttempts()) {
            options.getHook().postCall(answer);
            return completed(finish(ctx, RunStatus.UNVERIFIED,
                    "机械验证连续 " + ctx.verifyAttempts + " 次未通过，已达上限", null));
        }
        StepContext probe = ctx.lastStepContext != null ? ctx.lastStepContext : buildContext(ctx);
        return options.getVerifier().verify(probe, answer)
                .handle((result, error) -> {
                    if (error != null || result == null) {
                        return VerifyResult.skip("verifier", "验证器执行异常，按弃权处理：" + error);
                    }
                    return result;
                })
                .thenCompose(result -> onVerified(ctx, answer, result));
    }

    private CompletableFuture<HarnessResult> onVerified(RunContext ctx, String answer, VerifyResult result) {
        ctx.verifyAttempts++;
        emit(ctx, AgentEvent.Type.VERIFY_DECIDED, ctx.budget.getSteps(), new JSONObject()
                .fluentPut("status", result.getStatus().name())
                .fluentPut("verifier", result.getVerifier())
                .fluentPut("durationMillis", result.getDurationMillis())
                .fluentPut("attempt", ctx.verifyAttempts)
                .fluentPut("feedback", truncate(result.getFeedback(), 1_000)));

        if (result.isPassed()) {
            ctx.verified = true;
            options.getHook().postCall(answer);
            return completed(finish(ctx, RunStatus.SUCCESS, null, null));
        }
        // 反馈要能直接指导修改：带上原始报错，别只说"没通过"
        ctx.messages.add(Message.ofSystem("验证未通过（第 " + ctx.verifyAttempts + "/"
                + options.getMaxVerifyAttempts() + " 次）。请针对下面的具体错误修正后重新给出结论；"
                + "若你认为验证结论本身有问题，请指出依据，不要重复提交同样的答案。\n\n"
                + truncate(result.getFeedback(), 3_000)));
        return step(ctx);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    /**
     * 只有推理没有行动：连续两次就注入纠偏提示，而不是干等到预算耗尽
     */
    private CompletableFuture<HarnessResult> onThink(RunContext ctx, Step step) {
        if (ctx.consecutiveThink >= 2) {
            ctx.messages.add(Message.ofSystem("你已经连续多轮只推理未行动。请立刻二选一："
                    + "调用工具推进任务，或给出明确结论并说明还缺什么信息。"));
            ctx.consecutiveThink = 0;
        }
        return step(ctx);
    }

    private CompletableFuture<HarnessResult> onToolCall(RunContext ctx, Step step) {
        final String callId = step.getToolCallId() != null ? step.getToolCallId() : "call_" + ctx.budget.getSteps();
        ToolInvocation invocation = new ToolInvocation(callId, step.getToolName(), step.getArguments(),
                ctx.sessionId, ctx.budget.getSteps());

        // 原生协议要求 assistant(tool_calls) 与 tool(tool_call_id) 成对出现
        ctx.messages.add(Message.ofAssistant(step.getThought(), Collections.singletonList(toToolCall(step, callId))));

        // 重复动作软纠偏：先给信号，再考虑终止
        if (invocation.fingerprint().equals(ctx.lastFingerprint)) {
            ctx.repeatCount++;
            if (ctx.repeatCount >= 3) {
                ctx.messages.add(Message.ofSystem("你已经连续 3 次执行完全相同的动作，结果不会改变。"
                        + "请换用其他工具/参数，或直接给出结论。"));
                ctx.repeatCount = 0;
            }
        } else {
            ctx.lastFingerprint = invocation.fingerprint();
            ctx.repeatCount = 0;
        }

        emit(ctx, AgentEvent.Type.TOOL_INVOKED, ctx.budget.getSteps(),
                new JSONObject().fluentPut("callId", callId).fluentPut("tool", step.getToolName())
                        .fluentPut("arguments", step.getArguments()));

        ToolSpec spec = findSpec(step.getToolName());
        ToolPolicy.Decision decision = options.getToolPolicy().check(invocation, spec);
        emit(ctx, AgentEvent.Type.POLICY_DECIDED, ctx.budget.getSteps(),
                new JSONObject().fluentPut("decision", decision.getKind().name())
                        .fluentPut("reason", decision.getReason()));

        if (decision.getKind() == ToolPolicy.Decision.Kind.DENY) {
            return completeToolCall(ctx, invocation, ToolResult.denied(decision.getReason()));
        }
        if (decision.getKind() == ToolPolicy.Decision.Kind.ASK) {
            return options.getApprovalGate().approve(invocation, decision.getReason())
                    .thenCompose(approved -> Boolean.TRUE.equals(approved)
                            ? executeTool(ctx, invocation, spec)
                            : completeToolCall(ctx, invocation, ToolResult.denied("用户未批准：" + decision.getReason())));
        }
        return executeTool(ctx, invocation, spec);
    }

    private CompletableFuture<HarnessResult> executeTool(RunContext ctx, ToolInvocation invocation, ToolSpec spec) {
        if (options.getToolRuntime().isDisabled(invocation.getToolName())) {
            return completeToolCall(ctx, invocation, ToolResult.denied("该工具因连续失败已被临时禁用"));
        }
        ctx.budget.recordToolCall();
        return options.getToolRuntime().invoke(invocation)
                .handle((result, error) -> result != null ? result : ToolResult.error(String.valueOf(error), 0L))
                .thenCompose(result -> completeToolCall(ctx, invocation, result));
    }

    private CompletableFuture<HarnessResult> completeToolCall(RunContext ctx, ToolInvocation invocation, ToolResult result) {
        String observation = result.toObservation();
        ctx.messages.add(Message.ofTool(invocation.getCallId(), observation));

        emit(ctx, AgentEvent.Type.TOOL_COMPLETED, ctx.budget.getSteps(), new JSONObject()
                .fluentPut("callId", invocation.getCallId())
                .fluentPut("tool", invocation.getToolName())
                .fluentPut("status", result.getStatus().name())
                .fluentPut("durationMillis", result.getDurationMillis())
                .fluentPut("truncated", result.isTruncated())
                .fluentPut("observation", observation));

        if (!result.isOk()) {
            Integer failures = ctx.toolFailures.get(invocation.getToolName());
            failures = failures == null ? 1 : failures + 1;
            ctx.toolFailures.put(invocation.getToolName(), failures);
            if (failures >= 3) {
                options.getToolRuntime().disable(invocation.getToolName(), "连续失败 " + failures + " 次");
                ctx.messages.add(Message.ofSystem("工具 " + invocation.getToolName()
                        + " 已被禁用（连续失败 3 次），请改用其他方式。"));
            }
        }
        // 连续被拒说明模型没能绕开受限动作，继续跑只是在烧预算，直接如实收尾
        if (result.getStatus() == ToolResult.Status.DENIED) {
            ctx.denials++;
            if (ctx.denials >= 3) {
                return completed(finish(ctx, RunStatus.DENIED, "连续 " + ctx.denials + " 次动作被拒绝", null));
            }
        } else {
            ctx.denials = 0;
        }
        return step(ctx);
    }

    // ==================== 收尾与工具方法 ====================

    private HarnessResult finish(RunContext ctx, RunStatus status, String detail, Throwable error) {
        running.remove(ctx.sessionId);
        options.getContextEngine().onRunFinished(ctx.sessionId);
        Usage usage = new Usage();
        usage.setPromptTokens(ctx.promptTokens);
        usage.setCompletionTokens(ctx.completionTokens);
        usage.setTotalTokens(ctx.promptTokens + ctx.completionTokens);

        emit(ctx, AgentEvent.Type.RUN_FINISHED, ctx.budget.getSteps(),
                new JSONObject().fluentPut("status", status.name()).fluentPut("detail", detail)
                        .fluentPut("verified", ctx.verified));

        String answer = null;
        for (int i = ctx.history.size() - 1; i >= 0; i--) {
            Step step = ctx.history.get(i);
            if (step.getType() == Step.Type.FINAL) {
                answer = step.getContent();
                break;
            }
        }
        if (status == RunStatus.SUCCESS) {
            return HarnessResult.success(ctx.sessionId, answer, ctx.budget.getSteps(),
                    ctx.budget.getToolCalls(), usage, ctx.startedAt, ctx.verified);
        }
        if (status == RunStatus.UNVERIFIED) {
            // 答案照常带回：它对人有价值，只是没通过机械验收
            return HarnessResult.unverified(ctx.sessionId, answer, detail, ctx.budget.getSteps(),
                    ctx.budget.getToolCalls(), usage, ctx.startedAt);
        }
        return HarnessResult.failed(ctx.sessionId, status, detail, ctx.budget.getSteps(),
                ctx.budget.getToolCalls(), usage, ctx.startedAt, error);
    }

    private ToolSpec findSpec(String toolName) {
        for (ToolSpec spec : options.getToolRuntime().specs()) {
            if (spec.getName().equals(toolName)) {
                return spec;
            }
        }
        return null;
    }

    private ToolCall toToolCall(Step step, String callId) {
        ToolCall toolCall = new ToolCall();
        toolCall.setId(callId);
        toolCall.setName(step.getToolName());
        toolCall.setArguments(step.getArguments() == null ? "{}" : step.getArguments().toJSONString());
        return toolCall;
    }

    /**
     * 观测不参与业务正确性：事件落盘失败只告警，不中断 run
     */
    private void emit(RunContext ctx, AgentEvent.Type type, int stepIndex, JSONObject payload) {
        try {
            options.getSessionStore().append(AgentEvent.of(type, ctx.sessionId, stepIndex, payload));
        } catch (Exception e) {
            LOGGER.warn("事件写入失败 type={} session={} err={}", type, ctx.sessionId, e.getMessage());
        }
    }

    private static CompletableFuture<HarnessResult> completed(HarnessResult result) {
        return CompletableFuture.completedFuture(result);
    }

    private static CompletableFuture<HarnessResult> failedFuture(String reason) {
        CompletableFuture<HarnessResult> future = new CompletableFuture<HarnessResult>();
        future.completeExceptionally(new FeatException(reason));
        return future;
    }

    /**
     * 单次运行的内部状态，不与其它会话共享
     */
    private static final class RunContext {
        private final String sessionId;
        private final String input;
        private final Budget budget;
        private final long startedAt = System.currentTimeMillis();
        private final List<Message> messages = new ArrayList<Message>();
        private final List<Step> history = new ArrayList<Step>();
        private final Map<String, Integer> toolFailures = new ConcurrentHashMap<String, Integer>();

        private volatile boolean cancelled;
        /**
         * 是否已通过机械验证（未配置验证器时置 true，语义是"没有可验证项"）
         */
        private boolean verified;
        private int verifyAttempts;
        private String lastPrefixSig;
        private StepContext lastStepContext;
        private int consecutiveThink;
        private int repeatCount;
        private int denials;
        private String lastFingerprint;
        private int promptTokens;
        private int completionTokens;

        private RunContext(HarnessRequest request, Budget budget) {
            this.sessionId = request.getSessionId();
            this.input = request.getInput();
            this.budget = budget;
        }
    }
}
