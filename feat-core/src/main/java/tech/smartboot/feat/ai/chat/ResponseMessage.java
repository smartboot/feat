package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Collection;

public class ResponseMessage extends Message {
    private boolean success;
    private String error;
    @JSONField(name = "tool_calls")
    private Collection<ToolCall> toolCalls;

    public Collection<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(Collection<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
