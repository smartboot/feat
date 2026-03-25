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
     * 匹配思考步骤的正则表达式
     * <p>
     * 用于从AI模型的响应中提取Thought部分的内容。
     * 格式示例：Thought: 我需要搜索相关信息
     * </p>
     */
    private static final Pattern THOUGHT_PATTERN = Pattern.compile("Thought:\\s*(.+)");

    @Override
    public ToolCaller parse(String response) {
        int lastAI = response.lastIndexOf("AI:");
        int lastAction = response.lastIndexOf("Action:");

        // 查找最终答案
        if (lastAI > lastAction) {
            return ToolCaller.finalAnswer(response.substring(lastAI + 3));
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
        int index = response.indexOf('\n');
        if (index > 0) {
            action = response.substring(7, index).trim();
        }

        // 查找动作输入
        int startInput = response.indexOf("Action Input:");
        if (startInput > 0) {
            startInput = startInput + "Action Input:".length();
            actionInput = response.substring(startInput).trim();
        }

        // 如果有动作但没有输入，则返回null
        if (action != null && actionInput != null) {
            return ToolCaller.toolAction(action, actionInput, thought);
        } else {
            return null;
        }
    }
}
