/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import tech.smartboot.feat.ai.chat.entity.Message;

import java.util.Collections;
import java.util.List;

/**
 * 步进上下文：harness 传给 {@link AgentLoop} 的只读视图。
 * <p>
 * 刻意只暴露"够用就好"的信息：原始历史、组装后的窗口、可用工具、预算、步号。
 * 不暴露 SessionStore 与 ToolRuntime 的写能力，保证循环实现无法绕过治理。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class StepContext {

    private final String sessionId;
    private final String input;
    private final List<Message> messages;
    private final List<Message> window;
    private final List<ToolSpec> tools;
    private final Budget budget;
    private final int stepIndex;
    private final List<Step> history;
    private final HarnessOptions options;

    public StepContext(String sessionId, String input, List<Message> messages, List<Message> window,
                       List<ToolSpec> tools, Budget budget, int stepIndex, List<Step> history,
                       HarnessOptions options) {
        this.sessionId = sessionId;
        this.input = input;
        this.messages = messages;
        this.window = window;
        this.tools = tools;
        this.budget = budget;
        this.stepIndex = stepIndex;
        this.history = history;
        this.options = options;
    }

    public static StepContext empty(HarnessOptions options) {
        return new StepContext("", "", Collections.<Message>emptyList(), Collections.<Message>emptyList(),
                Collections.<ToolSpec>emptyList(), Budget.unlimited(), 0, Collections.<Step>emptyList(), options);
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInput() {
        return input;
    }

    /**
     * 原始历史（append-only，不因压缩而裁剪）
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * 本次调用实际发送给模型的窗口（可能是原始历史的裁剪/折叠视图）
     */
    public List<Message> getWindow() {
        return window;
    }

    public List<ToolSpec> getTools() {
        return tools;
    }

    public Budget getBudget() {
        return budget;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public List<Step> getHistory() {
        return history;
    }

    public HarnessOptions getOptions() {
        return options;
    }
}
