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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.enums.RoleEnum;
import tech.smartboot.feat.ai.mcp.model.Tool;

import java.util.ArrayList;
import java.util.List;

/**
 * sampling/createMessage 的请求参数。
 * <p>
 * 2025-11-25 起支持在采样中调用工具：服务端传 tools + toolChoice，
 * 模型可能返回 tool_use 内容，服务端执行后再以 tool_result 消息发起下一轮。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class CreateMessageRequest {
    private List<Object> messages = new ArrayList<>();
    private JSONObject modelPreferences;
    private String systemPrompt;
    private Integer maxTokens;
    private List<Tool> tools;
    private ToolChoice toolChoice;

    public static CreateMessageRequest of() {
        return new CreateMessageRequest();
    }

    public CreateMessageRequest message(RoleEnum role, Object content) {
        messages.add(JSONObject.of("role", role.getRole(), "content", content));
        return this;
    }

    /**
     * 回传工具执行结果。content 只能是 ToolResultContent 的集合，不可混入其他内容。
     */
    public CreateMessageRequest toolResults(ToolResultContent... results) {
        List<Object> blocks = new ArrayList<>();
        if (results != null) {
            for (ToolResultContent result : results) {
                blocks.add(result);
            }
        }
        return message(RoleEnum.User, blocks);
    }

    public CreateMessageRequest tools(Tool... tools) {
        this.tools = new ArrayList<>();
        if (tools != null) {
            for (Tool tool : tools) {
                this.tools.add(tool);
            }
        }
        return this;
    }

    public CreateMessageRequest toolChoice(ToolChoice toolChoice) {
        this.toolChoice = toolChoice;
        return this;
    }

    public CreateMessageRequest modelPreferences(JSONObject modelPreferences) {
        this.modelPreferences = modelPreferences;
        return this;
    }

    public CreateMessageRequest systemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public CreateMessageRequest maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public List<Object> getMessages() {
        return messages;
    }

    public JSONObject getModelPreferences() {
        return modelPreferences;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public ToolChoice getToolChoice() {
        return toolChoice;
    }
}
