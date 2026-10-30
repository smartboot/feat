/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model.sampling;

/**
 * sampling/createMessage 的返回结果。
 * <p>
 * content 可能是单个内容块，也可能是内容块数组（模型并行发起多个 tool_use 时）。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class CreateMessageResult {
    public static final String STOP_END_TURN = "endTurn";
    public static final String STOP_MAX_TOKENS = "maxTokens";
    public static final String STOP_STOP_SEQUENCE = "stopSequence";
    public static final String STOP_TOOL_USE = "toolUse";

    private String role = "assistant";
    private Object content;
    private String model;
    private String stopReason;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }
}
