/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import tech.smartboot.feat.ai.agent.harness.support.InMemorySessionStore;
import tech.smartboot.feat.ai.agent.harness.support.StablePrefixContextEngine;
import tech.smartboot.feat.ai.agent.hook.Hook;

import java.util.concurrent.CompletableFuture;

/**
 * Harness 组件装配。
 * <p>
 * 所有能力通过显式注入完成，不做注解扫描与反射，保持 feat 框架的 AOT 友好特性。
 * 除 {@link #loop(AgentLoop)} 与 {@link #toolRuntime(ToolRuntime)} 外均有默认实现，
 * 最小可用配置只需指定循环实现。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public class HarnessOptions {

    /**
     * 单步决策协议，必填
     */
    private AgentLoop loop;

    /**
     * 上下文组装策略。默认前缀稳定引擎：压缩后进入"冻结前缀 + 只追加尾部"模式，
     * 使两次压缩之间的每一步请求前缀字节一致，从而真正吃到 provider 侧的 KV-cache。
     */
    private ContextEngine contextEngine = new StablePrefixContextEngine(128_000, 0.7);

    /**
     * 工具运行时，必填
     */
    private ToolRuntime toolRuntime;

    /**
     * 事件存储，默认仅内存（生产建议替换为 JsonlSessionStore）
     */
    private SessionStore sessionStore = new InMemorySessionStore();

    /**
     * 工具政策，默认按副作用分级：READ 放行、WRITE/EXEC 审批、NETWORK 白名单
     */
    private ToolPolicy toolPolicy = ToolPolicy.DEFAULT;

    /**
     * 审批出口，默认一律拒绝——安全默认值，避免忘记配置时静默放行写操作
     */
    private ApprovalGate approvalGate = ApprovalGate.DENY_ALL;

    /**
     * 结果验证器，默认为无（保持"模型说完成即完成"的旧行为；生产环境强烈建议配置）
     */
    private Verifier verifier = Verifier.NONE;

    /**
     * FINAL 被验证器否决后允许重做的次数，默认 2。
     * <p>
     * 不要设太大：连续多次过不了验证，通常说明任务本身有歧义或环境缺东西，
     * 再让模型重试只是在烧预算——此时 {@link HarnessResult.RunStatus#UNVERIFIED} 更有价值。
     * </p>
     */
    private int maxVerifyAttempts = 2;

    /**
     * 生命周期钩子，可组合多个观察者
     */
    private Hook hook = new Hook() {
    };

    /**
     * 叠加在 AgentLoop 提示词之上的系统提示
     */
    private String systemPrompt;

    /**
     * 优先使用原生 tool_calls 协议
     */
    private boolean preferNativeToolCalling = true;

    private int maxSteps = 40;
    private int maxToolCalls = 60;
    private int maxTokens = 200_000;
    private long maxMillis = 10 * 60 * 1000L;

    public HarnessOptions loop(AgentLoop loop) {
        this.loop = loop;
        return this;
    }

    public HarnessOptions contextEngine(ContextEngine contextEngine) {
        this.contextEngine = contextEngine;
        return this;
    }

    public HarnessOptions toolRuntime(ToolRuntime toolRuntime) {
        this.toolRuntime = toolRuntime;
        return this;
    }

    public HarnessOptions sessionStore(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
        return this;
    }

    public HarnessOptions toolPolicy(ToolPolicy toolPolicy) {
        this.toolPolicy = toolPolicy;
        return this;
    }

    public HarnessOptions approvalGate(ApprovalGate approvalGate) {
        this.approvalGate = approvalGate;
        return this;
    }

    public HarnessOptions verifier(Verifier verifier) {
        this.verifier = verifier == null ? Verifier.NONE : verifier;
        return this;
    }

    public HarnessOptions maxVerifyAttempts(int maxVerifyAttempts) {
        this.maxVerifyAttempts = Math.max(0, maxVerifyAttempts);
        return this;
    }

    public HarnessOptions hook(Hook hook) {
        this.hook = hook;
        return this;
    }

    public HarnessOptions systemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public HarnessOptions preferNativeToolCalling(boolean preferNativeToolCalling) {
        this.preferNativeToolCalling = preferNativeToolCalling;
        return this;
    }

    public HarnessOptions maxSteps(int maxSteps) {
        this.maxSteps = Math.max(1, maxSteps);
        return this;
    }

    public HarnessOptions maxToolCalls(int maxToolCalls) {
        this.maxToolCalls = Math.max(1, maxToolCalls);
        return this;
    }

    public HarnessOptions maxTokens(int maxTokens) {
        this.maxTokens = Math.max(1, maxTokens);
        return this;
    }

    public HarnessOptions maxMillis(long maxMillis) {
        this.maxMillis = Math.max(1, maxMillis);
        return this;
    }

    /**
     * 为一次 run 创建独立预算，避免并发 run 之间互相污染计数
     */
    public Budget newBudget() {
        return new Budget(maxSteps, maxToolCalls, maxTokens, maxMillis);
    }

    public AgentLoop getLoop() {
        return loop;
    }

    public ContextEngine getContextEngine() {
        return contextEngine;
    }

    public ToolRuntime getToolRuntime() {
        return toolRuntime;
    }

    public SessionStore getSessionStore() {
        return sessionStore;
    }

    public ToolPolicy getToolPolicy() {
        return toolPolicy;
    }

    public ApprovalGate getApprovalGate() {
        return approvalGate;
    }

    public Verifier getVerifier() {
        return verifier;
    }

    public int getMaxVerifyAttempts() {
        return maxVerifyAttempts;
    }

    public Hook getHook() {
        return hook;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public boolean isPreferNativeToolCalling() {
        return preferNativeToolCalling;
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
