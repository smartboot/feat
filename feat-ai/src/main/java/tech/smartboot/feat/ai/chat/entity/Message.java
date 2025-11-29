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
     * 消息角色，可以是用户、系统或助手
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 推理内容，某些模型可能会输出推理过程
     */
    @JSONField(name = "reasoning_content")
    private String reasoningContent;

    /**
     * 创建一条用户消息
     *
     * @param content 消息内容
     * @return Message实例
     */
    public static Message ofUser(String content) {
        Message message = new Message();
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
        Message message = new Message();
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
        Message message = new Message();
        message.setRole(ROLE_ASSISTANT);
        message.setContent(content);
        return message;
    }

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
     * 获取消息内容
     *
     * @return 消息内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容
     *
     * @param content 消息内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取推理内容
     *
     * @return 推理内容
     */
    public String getReasoningContent() {
        return reasoningContent;
    }

    /**
     * 设置推理内容
     *
     * @param reasoningContent 推理内容
     */
    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }
}