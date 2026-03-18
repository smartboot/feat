/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.memory;

/**
 * 记忆角色类型枚举 - 定义记忆消息的来源角色
 * <p>
 * 不同角色的消息在记忆系统中有不同的用途和处理方式。
 * 角色信息帮助Agent理解记忆的上下文和重要性。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see MemoryMessage 记忆消息实体
 */
public enum MemoryRole {
    /**
     * 用户输入消息
     * <p>
     * 表示用户的原始输入或请求，是交互的起点。
     * 通常具有最高的检索优先级。
     * </p>
     */
    USER("User"),

    /**
     * AI助手回复消息
     * <p>
     * 表示AI Agent生成的回复内容。
     * 包含最终答案或中间推理结果。
     * </p>
     */
    ASSISTANT("AI"),

    /**
     * 系统消息
     * <p>
     * 表示系统级别的提示或指令。
     * 通常包含重要但不需要频繁检索的信息。
     * </p>
     */
    SYSTEM("System"),

    /**
     * 工具调用结果消息
     * <p>
     * 表示外部工具执行后的返回结果。
     * 包含具体的数据和信息，对后续决策很重要。
     * </p>
     */
    TOOL("Tool"),

    /**
     * 思考过程消息
     * <p>
     * 表示AI Agent的推理和思考过程。
     * 有助于理解Agent的决策逻辑，但检索优先级较低。
     * </p>
     */
    THOUGHT("Thought"),

    /**
     * 动作消息
     * <p>
     * 表示Agent决定执行的具体动作。
     * 通常与工具调用相关联。
     * </p>
     */
    ACTION("Action"),

    /**
     * 观察消息
     * <p>
     * 表示Agent对执行结果的观察和总结。
     * 包含对环境的感知信息。
     * </p>
     */
    OBSERVATION("Observation");

    /**
     * 角色显示名称
     */
    private final String displayName;

    MemoryRole(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取角色的显示名称
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据显示名称查找对应的角色类型
     *
     * @param displayName 显示名称
     * @return 对应的MemoryRole，找不到则返回null
     */
    public static MemoryRole fromDisplayName(String displayName) {
        for (MemoryRole role : values()) {
            if (role.displayName.equalsIgnoreCase(displayName)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 判断该角色是否为AI相关的角色（需要AI生成内容）
     *
     * @return true如果是AI生成内容的角色
     */
    public boolean isAiRole() {
        return this == ASSISTANT || this == THOUGHT || this == ACTION;
    }

    /**
     * 判断该角色是否为用户输入相关角色
     *
     * @return true如果是用户输入相关角色
     */
    public boolean isUserRole() {
        return this == USER || this == OBSERVATION;
    }

    /**
     * 获取该角色的默认重要性权重
     *
     * @return 重要性权重值
     */
    public double getDefaultImportance() {
        switch (this) {
            case USER:
            case TOOL:
                return 1.5;
            case ASSISTANT:
                return 1.2;
            case OBSERVATION:
                return 1.0;
            case SYSTEM:
                return 0.8;
            case ACTION:
                return 0.9;
            case THOUGHT:
                return 0.6;
            default:
                return 1.0;
        }
    }
}
