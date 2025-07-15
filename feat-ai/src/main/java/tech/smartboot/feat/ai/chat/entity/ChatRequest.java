/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;


import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatRequest {
    private String model;
    private boolean stream;
    private List<Message> messages;
    /**
     * 用于定义工具列表，
     */
    @JSONField(serializeFeatures = JSONWriter.Feature.IgnoreNoneSerializable)
    private List<Tool> tools;
    @JSONField(name = "tool_choice", serializeFeatures = JSONWriter.Feature.IgnoreNoneSerializable)
    private String toolChoice;

    @JSONField(name = "response_format")
    private ResponseFormat responseFormat;

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

    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }
}
