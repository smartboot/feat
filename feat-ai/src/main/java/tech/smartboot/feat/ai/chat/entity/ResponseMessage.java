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

import java.util.List;

/**
 * 响应消息类，继承自Message，表示AI模型的响应消息
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ResponseMessage extends Message {
    /**
     * 请求是否成功
     */
    private boolean success;

    /**
     * 错误信息，当请求失败时包含错误详情
     */
    private String error;

    /**
     * 工具调用列表，当模型决定调用工具时包含相关信息
     */
    @JSONField(name = "tool_calls")
    private List<ToolCall> toolCalls;

    /**
     * 使用情况统计信息，不会参与序列化和反序列化
     */
    @JSONField(deserialize = false, serialize = false)
    private Usage usage;

    /**
     * 提示词对数概率信息，不会参与序列化和反序列化
     */
    @JSONField(deserialize = false, serialize = false)
    private String promptLogprobs;

    /**
     * 获取工具调用列表
     *
     * @return 工具调用列表
     */
    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    /**
     * 设置工具调用列表
     *
     * @param toolCalls 工具调用列表
     */
    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    /**
     * 判断请求是否成功
     *
     * @return 请求成功返回true，否则返回false
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置请求是否成功
     *
     * @param success 请求是否成功
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    public String getError() {
        return error;
    }

    /**
     * 设置错误信息
     *
     * @param error 错误信息
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * 获取使用情况统计信息
     *
     * @return 使用情况统计信息
     */
    public Usage getUsage() {
        return usage;
    }

    /**
     * 设置使用情况统计信息
     *
     * @param usage 使用情况统计信息
     */
    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    /**
     * 获取提示词对数概率信息
     *
     * @return 提示词对数概率信息
     */
    public String getPromptLogprobs() {
        return promptLogprobs;
    }

    /**
     * 设置提示词对数概率信息
     *
     * @param promptLogprobs 提示词对数概率信息
     */
    public void setPromptLogprobs(String promptLogprobs) {
        this.promptLogprobs = promptLogprobs;
    }
}