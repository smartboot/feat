package tech.smartboot.feat.ai.chat;


import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

public class ChatRequest {
    private String model;
    private boolean stream;
    private List<Message> messages;
    /**
     * 用于定义工具列表，
     */
    private List<Tool> tools;
    @JSONField(name = "tool_choice")
    private String toolChoice = "auto";

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public String getToolChoice() {
        return toolChoice;
    }

    public void setToolChoice(String toolChoice) {
        this.toolChoice = toolChoice;
    }
}
