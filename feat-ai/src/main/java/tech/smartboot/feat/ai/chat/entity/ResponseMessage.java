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

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Collection;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class ResponseMessage extends Message {
    private boolean success;
    private String error;
    @JSONField(name = "tool_calls")
    private Collection<ToolCall> toolCalls;

    @JSONField(deserialize = false, serialize = false)
    private Usage usage;
    @JSONField(deserialize = false, serialize = false)
    private String promptLogprobs;
    /**
     * 是否丢弃该消息
     */
    private boolean discard = false;

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

    public boolean isDiscard() {
        return discard;
    }

    public void discard() {
        this.discard = true;
    }
}
