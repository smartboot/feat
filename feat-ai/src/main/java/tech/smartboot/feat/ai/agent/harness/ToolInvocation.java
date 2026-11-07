/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import com.alibaba.fastjson2.JSONObject;

/**
 * 一次工具调用请求。
 * <p>
 * 与 {@code AgentTool#execute(JSONObject)} 的入参相比，多了 {@code callId} 与会话信息：
 * 前者用于把结果与模型发起的 tool_call 配对（原生协议必需），
 * 后者让策略与审批能拿到上下文（谁、第几步、哪个会话）。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class ToolInvocation {

    private final String callId;
    private final String toolName;
    private final JSONObject arguments;
    private final String sessionId;
    private final int stepIndex;

    public ToolInvocation(String callId, String toolName, JSONObject arguments, String sessionId, int stepIndex) {
        this.callId = callId;
        this.toolName = toolName;
        this.arguments = arguments == null ? new JSONObject() : arguments;
        this.sessionId = sessionId;
        this.stepIndex = stepIndex;
    }

    public String getCallId() {
        return callId;
    }

    public String getToolName() {
        return toolName;
    }

    public JSONObject getArguments() {
        return arguments;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    /**
     * 用于事件流与熔断器的动作指纹（判断"是否在重复同一个动作"）
     */
    public String fingerprint() {
        return toolName + '|' + arguments.toJSONString();
    }

    @Override
    public String toString() {
        return "ToolInvocation{" + toolName + " " + arguments + '}';
    }
}
