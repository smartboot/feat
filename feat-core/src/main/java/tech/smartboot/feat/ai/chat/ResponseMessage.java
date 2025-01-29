package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

public class ResponseMessage extends Message {
    @JSONField(name = "tool_calls")
    private List<ToolCall> toolCalls;

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }
}
