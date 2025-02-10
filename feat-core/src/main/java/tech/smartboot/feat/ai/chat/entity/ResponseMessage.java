package tech.smartboot.feat.ai.chat.entity;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Collection;

public class ResponseMessage extends Message {
    private boolean success;
    private String error;
    @JSONField(name = "tool_calls")
    private Collection<ToolCall> toolCalls;

    @JSONField(deserialize = false, serialize = false)
    private Usage usage;
    @JSONField(deserialize = false, serialize = false)
    private String promptLogprobs;

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

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public String getPromptLogprobs() {
        return promptLogprobs;
    }

    public void setPromptLogprobs(String promptLogprobs) {
        this.promptLogprobs = promptLogprobs;
    }
}
