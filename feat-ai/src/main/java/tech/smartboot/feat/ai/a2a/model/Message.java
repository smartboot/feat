/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.model;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.a2a.enums.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * A2A 消息类
 *
 * <p>表示智能体之间交换的消息，包含角色、内容部分列表和元数据。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class Message {
    /**
     * 消息角色（用户或智能体）
     */
    private Role role;

    /**
     * 消息内容部分列表
     */
    private List<Part> parts;

    /**
     * 消息元数据
     */
    private JSONObject metadata;

    public Message() {
        this.parts = new ArrayList<>();
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    public JSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    /**
     * 添加一个内容部分
     *
     * @param part 内容部分
     * @return 当前Message实例（链式调用）
     */
    public Message addPart(Part part) {
        if (this.parts == null) {
            this.parts = new ArrayList<>();
        }
        this.parts.add(part);
        return this;
    }

    /**
     * 创建用户消息
     *
     * @param text 文本内容
     * @return Message实例
     */
    public static Message userMessage(String text) {
        Message message = new Message();
        message.setRole(Role.USER);
        message.addPart(Part.text(text));
        return message;
    }

    /**
     * 创建智能体消息
     *
     * @param text 文本内容
     * @return Message实例
     */
    public static Message agentMessage(String text) {
        Message message = new Message();
        message.setRole(Role.AGENT);
        message.addPart(Part.text(text));
        return message;
    }

    /**
     * 创建包含文件的用户消息
     *
     * @param file 文件内容
     * @return Message实例
     */
    public static Message userMessageWithFile(FileContent file) {
        Message message = new Message();
        message.setRole(Role.USER);
        message.addPart(Part.file(file));
        return message;
    }

    /**
     * 创建包含函数调用的智能体消息
     *
     * @param functionCall 函数调用
     * @return Message实例
     */
    public static Message agentMessageWithFunction(FunctionCall functionCall) {
        Message message = new Message();
        message.setRole(Role.AGENT);
        message.addPart(Part.functionCall(functionCall));
        return message;
    }
}
