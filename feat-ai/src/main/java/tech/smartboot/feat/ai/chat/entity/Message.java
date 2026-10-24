/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * 消息类，表示聊天过程中的一条消息
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Message {
    /**
     * 用户角色，表示用户的问题或指令。
     */
    public static final String ROLE_USER = "user";
    /**
     * 系统角色，通常用于设定 AI 的行为和性格，比如 "你是一个专家"。
     */
    public static final String ROLE_SYSTEM = "system";
    /**
     * 助手角色，表示 AI 的回答，可以用来模拟多轮对话。
     */
    public static final String ROLE_ASSISTANT = "assistant";
    /**
     * 工具角色，表示工具函数的执行结果，需与 {@link #ROLE_ASSISTANT} 消息的
     * {@code tool_calls} 配对使用（tool_call_id 对应）。
     */
    public static final String ROLE_TOOL = "tool";

    /**
     * 消息角色，可以是用户、系统、助手或工具
     */
    private String role;

    /**
     * 工具调用列表，携带在 assistant 消息上，表示模型发起的工具调用
     */
    @JSONField(name = "tool_calls")
    private List<ToolCall> toolCalls;

    /**
     * 工具调用标识，携带在 tool 消息上，与 assistant 消息中 tool_calls 的 id 对应
     */
    @JSONField(name = "tool_call_id")
    private String toolCallId;


    /**
     * 创建一条用户消息
     *
     * @param content 消息内容
     * @return Message实例
     */
    public static Message ofUser(String content) {
        TextMessage message = new TextMessage();
        message.setRole(ROLE_USER);
        message.setContent(content);
        return message;
    }

    /**
     * 创建一条系统消息
     *
     * @param content 消息内容
     * @return Message实例
     */
    public static Message ofSystem(String content) {
        TextMessage message = new TextMessage();
        message.setRole(ROLE_SYSTEM);
        message.setContent(content);
        return message;
    }

    /**
     * 创建一条助手消息
     *
     * @param content 消息内容
     * @return Message实例
     */
    public static Message ofAssistant(String content) {
        TextMessage message = new TextMessage();
        message.setRole(ROLE_ASSISTANT);
        message.setContent(content);
        return message;
    }

    /**
     * 创建一条携带工具调用的助手消息，用于多轮工具对话的上下文回传
     *
     * @param content   消息内容，可为 null（纯工具调用场景）
     * @param toolCalls 模型发起的工具调用列表（通常来自上一轮 {@code ChatResponse#getToolCalls()}）
     * @return Message实例
     */
    public static Message ofAssistant(String content, List<ToolCall> toolCalls) {
        TextMessage message = new TextMessage();
        message.setRole(ROLE_ASSISTANT);
        message.setContent(content);
        message.setToolCalls(toolCalls);
        return message;
    }

    /**
     * 创建一条工具结果消息，用于将工具执行结果回传给模型，
     * 需与对应 assistant 消息中 tool_calls 的 id 匹配
     *
     * @param toolCallId 对应的工具调用标识
     * @param content    工具执行结果
     * @return Message实例
     */
    public static Message ofTool(String toolCallId, String content) {
        TextMessage message = new TextMessage();
        message.setRole(ROLE_TOOL);
        message.setToolCallId(toolCallId);
        message.setContent(content);
        return message;
    }

//    public static Message ofImage()

    /**
     * 获取消息角色
     *
     * @return 消息角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置消息角色
     *
     * @param role 消息角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取工具调用列表
     *
     * @return 工具调用列表，非 assistant 消息为 null
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
     * 获取工具调用标识
     *
     * @return 工具调用标识，非 tool 消息为 null
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * 设置工具调用标识
     *
     * @param toolCallId 工具调用标识
     */
    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

}