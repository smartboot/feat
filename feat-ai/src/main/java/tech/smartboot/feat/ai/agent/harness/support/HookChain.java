/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.support;

import tech.smartboot.feat.ai.agent.ToolCaller;
import tech.smartboot.feat.ai.agent.hook.Hook;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 多 Hook 组合器。
 * <p>
 * 现有 {@code Hook} 是单实例接口，而生产环境往往同时需要指标、审计、前端推送三类观察者。
 * 这里选择**组合而非改接口签名**：任何已有 {@code Hook} 实现零改动即可复用。
 * </p>
 * <p>
 * 每个回调独立 try-catch：一个观察者抛异常不能影响其它观察者，更不能中断 run。
 * 需要"否决能力"的场景请用 {@code ToolPolicy}，不要塞进 Hook。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class HookChain implements Hook {

    private final List<Hook> hooks = new CopyOnWriteArrayList<Hook>();

    public HookChain add(Hook hook) {
        if (hook != null) {
            hooks.add(hook);
        }
        return this;
    }

    public List<Hook> hooks() {
        return hooks;
    }

    @Override
    public void preCall(String message) {
        for (Hook hook : hooks) {
            try {
                hook.preCall(message);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void postCall(String message) {
        for (Hook hook : hooks) {
            try {
                hook.postCall(message);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void preTool(ToolCaller toolCaller) {
        for (Hook hook : hooks) {
            try {
                hook.preTool(toolCaller);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void postTool(ToolCaller toolCaller) {
        for (Hook hook : hooks) {
            try {
                hook.postTool(toolCaller);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void onAgentReasoning(String reasoning) {
        for (Hook hook : hooks) {
            try {
                hook.onAgentReasoning(reasoning);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void onModelReasoning(String reasoning) {
        for (Hook hook : hooks) {
            try {
                hook.onModelReasoning(reasoning);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void onFinalAnswer(String content) {
        for (Hook hook : hooks) {
            try {
                hook.onFinalAnswer(content);
            } catch (Exception ignore) {
            }
        }
    }
}
