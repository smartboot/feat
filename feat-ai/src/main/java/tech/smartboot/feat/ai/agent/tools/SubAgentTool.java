/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentTool;

/**
 * 子代理工具，用于委托任务给专门的子代理
 * <p>
 * 该工具允许主Agent将复杂或专业任务委托给专门的子Agent处理，
 * 实现任务分解和专业化处理。通过子Agent可以实现更好的上下文隔离
 * 和更专业的任务处理能力。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SubAgentTool implements AgentTool {

    private static final String NAME = "sub_agent";
    private static final String DESCRIPTION = "将任务委托给专门的子代理，实现上下文隔离和专业任务执行";

    /**
     * 执行子代理工具
     * <p>
     * 将指定任务委托给命名的子代理执行。在实际实现中，这应该涉及
     * 创建或获取指定名称的子代理实例，并将任务传递给它处理。
     * </p>
     *
     * @param parameters 包含子代理名称和任务描述的参数
     * @return 子代理执行结果的模拟字符串
     */
    @Override
    public String execute(JSONObject parameters) {
        String agentName = parameters.getString("agent_name");
        String task = parameters.getString("task");

        if (agentName == null || agentName.isEmpty()) {
            return "错误：必须提供'agent_name'参数";
        }

        if (task == null || task.isEmpty()) {
            return "错误：必须提供'task'参数";
        }

        // 这里应该实际调用子代理来执行任务
        // 目前我们只是模拟这个过程
        return String.format("已将任务委托给子代理 '%s': %s\n结果: [子代理执行结果]", agentName, task);
    }

    /**
     * 获取工具名称
     *
     * @return 工具名称 "sub_agent"
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * 获取工具描述
     *
     * @return 工具功能描述
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * 获取工具参数的JSON Schema定义
     * <p>
     * 定义了子代理工具的参数格式，包括子代理名称和任务描述。
     * </p>
     *
     * @return 参数定义的JSON Schema字符串
     */
    @Override
    public String getParametersSchema() {
        return "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"agent_name\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"子代理名称\"\n" +
                "    },\n" +
                "    \"task\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"要委托的任务描述\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\"agent_name\", \"task\"]\n" +
                "}";
    }
}