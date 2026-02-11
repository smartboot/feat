/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent;

/**
 * Agent动作内部类
 * <p>
 * 用于封装Agent在ReAct循环中的一次完整动作，包括：
 * 1. Thought（思考）: Agent的推理内容
 * 2. Action（行动）: 要执行的操作
 * 3. Action Input（行动输入）: 操作的参数
 * </p>
 */
public class AgentAction {
    static final String FINAL_ANSWER = "Final Answer:";
    /**
     * 思考内容
     * <p>
     * Agent对当前情况的分析和下一步行动计划的思考。
     * </p>
     */
    private String thought;

    /**
     * 动作名称
     * <p>
     * 要执行的具体操作或工具名称。
     * </p>
     */
    private String action;

    /**
     * 动作输入参数
     * <p>
     * 传递给动作或工具的参数信息。
     * </p>
     */
    private String actionInput;

    private AgentAction() {
    }

    public static AgentAction toolAction(String toolName, String input, String thought) {
        AgentAction agentAction = new AgentAction();
        agentAction.setThought(thought);
        agentAction.setAction(toolName);
        agentAction.setActionInput(input);
        return agentAction;
    }

    public static AgentAction finalAnswer(String answer) {
        AgentAction agentAction = new AgentAction();
        agentAction.setAction(FINAL_ANSWER);
        agentAction.setActionInput(answer);
        return agentAction;
    }

    /**
     * 获取思考内容
     *
     * @return 思考内容
     */
    public String getThought() {
        return thought;
    }

    /**
     * 设置思考内容
     *
     * @param thought 思考内容
     */
    public void setThought(String thought) {
        this.thought = thought;
    }

    /**
     * 获取动作名称
     *
     * @return 动作名称
     */
    public String getAction() {
        return action;
    }

    /**
     * 设置动作名称
     *
     * @param action 动作名称
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 获取动作输入参数
     *
     * @return 动作输入参数
     */
    public String getActionInput() {
        return actionInput;
    }

    /**
     * 设置动作输入参数
     *
     * @param actionInput 动作输入参数
     */
    public void setActionInput(String actionInput) {
        this.actionInput = actionInput;
    }
}