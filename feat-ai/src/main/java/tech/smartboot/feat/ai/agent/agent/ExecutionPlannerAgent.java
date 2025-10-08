/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.agent;

import tech.smartboot.feat.ai.agent.Agent;
import tech.smartboot.feat.ai.agent.AgentOptions;
import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 执行计划规划师 Agent
 * 该 Agent 负责根据用户需求分析并制定执行计划，协调其他 Agents 完成任务
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ExecutionPlannerAgent extends FeatAgent {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionPlannerAgent.class.getName());
    private static final Function<String, String> PROMPT = new Function<String, String>() {
        @Override
        public String apply(String s) {
            return "";
        }
    };
    /**
     * 可用的 Agents 列表
     */
    private final List<Agent> availableAgents = new ArrayList<>();

    public ExecutionPlannerAgent(Consumer<AgentOptions> opt) {
        super(opt);
    }

    /**
     * 添加可用的 Agent
     *
     * @param agent 可用的 Agent
     */
    public void addAvailableAgent(Agent agent) {
        availableAgents.add(agent);
        logger.info("添加可用Agent: " + agent.getName());
    }

    /**
     * 获取所有可用 Agents 的描述信息
     *
     * @return Agents 描述信息
     */
    private String getAgentsDescription() {
        if (availableAgents.isEmpty()) {
            return "当前没有可用的 Agents。";
        }

        StringBuilder agentsInfo = new StringBuilder("# 可用的 Agents\n\n");
        for (Agent agent : availableAgents) {
            agentsInfo.append("## ").append(agent.getName()).append("\n");
            agentsInfo.append("- **描述**: ").append(agent.getDescription()).append("\n");

            // 如果 Agent 是 FeatAgent 类型，获取其工具信息
            if (agent instanceof FeatAgent) {
                FeatAgent featAgent = (FeatAgent) agent;
                String toolsPrompts = featAgent.getToolsPrompts();
                if (toolsPrompts != null && !toolsPrompts.isEmpty()) {
                    agentsInfo.append("- **工具**: \n").append(toolsPrompts).append("\n");
                } else {
                    agentsInfo.append("- **工具**: 无\n\n");
                }
            } else {
                agentsInfo.append("- **工具**: 未知\n\n");
            }
        }

        return agentsInfo.toString();
    }


    @Override
    public void execute(Map<String, String> input, StreamResponseCallback callback) {

        // 合并输入参数
        Map<String, String> mergedInput = new HashMap<>(input);
        mergedInput.put("agents", getAgentsDescription());

        chatModel.chatStream(options.getPrompt(), data -> data.putAll(mergedInput), callback);
    }
}