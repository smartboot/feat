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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 三刀
 * @version v1.0 2/11/26
 */
public class DefaultActionParse implements ActionParse {
    /**
     * 匹配动作输入的正则表达式
     * <p>
     * 用于从AI模型的响应中提取Action Input部分的内容，即传递给工具的参数。
     * 格式示例：Action Input: {"query": "最新的人工智能技术"}
     * </p>
     */
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input:\\s*(.+)", Pattern.DOTALL);
    /**
     * 匹配动作步骤的正则表达式
     * <p>
     * 用于从AI模型的响应中提取Action部分的内容，即要执行的工具名称。
     * 格式示例：Action: search
     * </p>
     */
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*([\\w_]+)");
    /**
     * 匹配思考步骤的正则表达式
     * <p>
     * 用于从AI模型的响应中提取Thought部分的内容。
     * 格式示例：Thought: 我需要搜索相关信息
     * </p>
     */
    private static final Pattern THOUGHT_PATTERN = Pattern.compile("Thought:\\s*(.+)");
    /**
     * 匹配最终答案的正则表达式
     * <p>
     * 用于从AI模型的响应中提取最终答案。
     * 格式示例：AI: 这是问题的答案...
     * </p>
     */
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("AI:\\s*(.+)");

    @Override
    public AgentAction parse(String response) {
        int lastAI = response.lastIndexOf("AI:");
        int lastAction = response.lastIndexOf("Action:");

        // 查找最终答案
        if (lastAI > lastAction) {
            return AgentAction.finalAnswer(response.substring(lastAI + 3));
        }

        String thought = null;
        String action = null;
        String actionInput = null;
        // 查找思考步骤
        int lastThought = response.lastIndexOf("Thought:");
        if (lastThought > 0) {
            Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(response.substring(lastThought));
            if (thoughtMatcher.find()) {
                thought = thoughtMatcher.group(1);
            }
        }

        response = response.substring(lastAction);

        // 查找动作
        Matcher actionMatcher = ACTION_PATTERN.matcher(response);
        if (actionMatcher.find()) {
            action = actionMatcher.group(1).trim();
        }

        // 查找动作输入
        Matcher actionInputMatcher = ACTION_INPUT_PATTERN.matcher(response);
        if (actionInputMatcher.find()) {
            // 将换行符标准化为 \n
            actionInput = actionInputMatcher.group(1).trim().replaceAll("\\R", "\n");
        }

        // 如果有动作但没有输入，则返回null
        if (action != null && actionInput != null) {
            return AgentAction.toolAction(action, actionInput, thought);
        } else {
            return null;
        }
    }
}
