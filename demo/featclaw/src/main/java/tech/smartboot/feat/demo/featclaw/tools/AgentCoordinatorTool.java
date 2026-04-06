/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.featclaw.tools;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.demo.featclaw.coordination.AgentCoordinator;
import tech.smartboot.feat.demo.featclaw.registry.AgentRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Agent协调工具 - 用于Agent之间的协调和任务委派
 * <p>
 * 该工具允许一个Agent调用其他Agent来完成特定任务，实现多Agent协同工作。
 * 支持以下操作：
 * 1. delegate_task - 委派任务给其他Agent
 * 2. execute_parallel - 并行执行多个Agent
 * 3. execute_sequential - 串行执行多个Agent
 * 4. query_agents - 查询可用Agent列表
 * 5. get_agent_info - 获取Agent详细信息
 * </p>
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class AgentCoordinatorTool implements AgentTool {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentCoordinatorTool.class);
    
    private static final String NAME = "agent_coordinator";
    private static final String DESCRIPTION = "协调多个Agent协同工作，支持任务委派、并行/串行执行、查询可用Agent等。";
    
    /**
     * Agent协调器实例
     */
    private final AgentCoordinator coordinator;
    
    /**
     * Agent注册表
     */
    private final AgentRegistry agentRegistry;
    
    public AgentCoordinatorTool(AgentCoordinator coordinator) {
        this.coordinator = coordinator;
        this.agentRegistry = AgentRegistry.getInstance();
    }
    
    @Override
    public CompletableFuture<String> execute(JSONObject parameters) {
        String action = parameters.getString("action");
        
        if (action == null) {
            return CompletableFuture.completedFuture("错误：必须提供 'action' 参数");
        }
        
        try {
            switch (action) {
                case "delegate_task":
                    return delegateTask(parameters);
                case "execute_parallel":
                    return executeParallel(parameters);
                case "execute_sequential":
                    return executeSequential(parameters);
                case "query_agents":
                    return queryAgents(parameters);
                case "get_agent_info":
                    return getAgentInfo(parameters);
                case "get_available_agents":
                    return getAvailableAgents();
                default:
                    return CompletableFuture.completedFuture("错误：不支持的操作 '" + action + "'");
            }
        } catch (Exception e) {
            logger.error("执行Agent协调操作时出错", e);
            return CompletableFuture.completedFuture("执行操作时出错: " + e.getMessage());
        }
    }
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public String getParametersSchema() {
        return "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"action\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\"delegate_task\", \"execute_parallel\", \"execute_sequential\", \"query_agents\", \"get_agent_info\", \"get_available_agents\"],\n" +
            "      \"description\": \"要执行的操作类型\"\n" +
            "    },\n" +
            "    \"agent_name\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"目标Agent名称（用于delegate_task和get_agent_info）\"\n" +
            "    },\n" +
            "    \"task\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"任务描述（用于delegate_task）\"\n" +
            "    },\n" +
            "    \"tasks\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"description\": \"任务映射，key为agent名称，value为任务（用于execute_parallel和execute_sequential）\"\n" +
            "    },\n" +
            "    \"skill\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"技能名称（用于query_agents）\"\n" +
            "    },\n" +
            "    \"timeout_seconds\": {\n" +
            "      \"type\": \"integer\",\n" +
            "      \"description\": \"超时时间（秒），默认为120秒\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\"action\"]\n" +
            "}";
    }
    
    /**
     * 委派任务给指定Agent
     */
    private CompletableFuture<String> delegateTask(JSONObject parameters) {
        String agentName = parameters.getString("agent_name");
        String task = parameters.getString("task");
        int timeout = parameters.getIntValue("timeout_seconds", 120);
        
        if (agentName == null || task == null) {
            return CompletableFuture.completedFuture(
                "错误：delegate_task 需要提供 agent_name 和 task 参数");
        }
        
        logger.info("委派任务给Agent '{}': {}", agentName, task);
        
        return coordinator.execute(agentName, task, timeout)
                .thenApply(result -> "Agent '" + agentName + "' 执行结果:\n" + result)
                .exceptionally(e -> "任务执行失败: " + e.getMessage());
    }
    
    /**
     * 并行执行多个Agent
     */
    private CompletableFuture<String> executeParallel(JSONObject parameters) {
        JSONObject tasksObj = parameters.getJSONObject("tasks");
        int timeout = parameters.getIntValue("timeout_seconds", 120);
        
        if (tasksObj == null || tasksObj.isEmpty()) {
            return CompletableFuture.completedFuture("错误：execute_parallel 需要提供 tasks 参数");
        }
        
        Map<String, String> tasks = new HashMap<>();
        for (String key : tasksObj.keySet()) {
            tasks.put(key, tasksObj.getString(key));
        }
        
        logger.info("并行执行 {} 个Agent任务", tasks.size());
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> results = coordinator.executeParallel(tasks, timeout);
            
            StringBuilder sb = new StringBuilder();
            sb.append("并行执行结果:\n");
            sb.append("========================================\n\n");
            
            for (Map.Entry<String, String> entry : results.entrySet()) {
                sb.append("【Agent: ").append(entry.getKey()).append("】\n");
                sb.append(entry.getValue()).append("\n\n");
            }
            
            return sb.toString();
        });
    }
    
    /**
     * 串行执行多个Agent
     */
    private CompletableFuture<String> executeSequential(JSONObject parameters) {
        JSONObject tasksObj = parameters.getJSONObject("tasks");
        
        if (tasksObj == null || tasksObj.isEmpty()) {
            return CompletableFuture.completedFuture("错误：execute_sequential 需要提供 tasks 参数");
        }
        
        // 将 JSON 对象转换为有序列表
        List<Map.Entry<String, String>> tasks = new ArrayList<>();
        for (String key : tasksObj.keySet()) {
            tasks.add(new java.util.AbstractMap.SimpleEntry<>(key, tasksObj.getString(key)));
        }
        
        logger.info("串行执行 {} 个Agent任务", tasks.size());
        
        return CompletableFuture.supplyAsync(() -> {
            String result = coordinator.executeSequential(tasks);
            return "串行执行结果:\n========================================\n\n" + result;
        });
    }
    
    /**
     * 查询支持特定技能的Agent
     */
    private CompletableFuture<String> queryAgents(JSONObject parameters) {
        String skill = parameters.getString("skill");
        
        if (skill == null) {
            // 返回所有Agent
            return getAvailableAgents();
        }
        
        java.util.Collection<tech.smartboot.feat.demo.featclaw.config.AgentConfig> agents = agentRegistry.findAgentsBySkill(skill);
        
        StringBuilder sb = new StringBuilder();
        sb.append("支持技能 '").append(skill).append("' 的Agent:\n");
        sb.append("========================================\n\n");
        
        if (agents.isEmpty()) {
            sb.append("没有找到支持该技能的Agent\n");
        } else {
            for (tech.smartboot.feat.demo.featclaw.config.AgentConfig agent : agents) {
                sb.append("- ").append(agent.getName());
                sb.append(" (").append(agent.getDisplayName()).append(")\n");
                sb.append("  描述: ").append(agent.getDescription()).append("\n\n");
            }
        }
        
        return CompletableFuture.completedFuture(sb.toString());
    }
    
    /**
     * 获取Agent详细信息
     */
    private CompletableFuture<String> getAgentInfo(JSONObject parameters) {
        String agentName = parameters.getString("agent_name");
        
        if (agentName == null) {
            return CompletableFuture.completedFuture("错误：get_agent_info 需要提供 agent_name 参数");
        }
        
        tech.smartboot.feat.demo.featclaw.config.AgentConfig config = agentRegistry.getAgent(agentName);
        
        if (config == null) {
            return CompletableFuture.completedFuture("错误：找不到Agent '" + agentName + "'");
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Agent详细信息: ").append(agentName).append("\n");
        sb.append("========================================\n\n");
        sb.append("名称: ").append(config.getName()).append("\n");
        sb.append("显示名: ").append(config.getDisplayName()).append("\n");
        sb.append("描述: ").append(config.getDescription()).append("\n");
        sb.append("角色: ").append(config.getRole()).append("\n");
        sb.append("是否为调度器: ").append(config.isOrchestrator() ? "是" : "否").append("\n\n");
        
        if (config.getSkills() != null) {
            sb.append("支持技能:\n");
            for (String skill : config.getSkills()) {
                sb.append("  - ").append(skill).append("\n");
            }
            sb.append("\n");
        }
        
        if (config.getTools() != null) {
            sb.append("可用工具:\n");
            for (String tool : config.getTools()) {
                sb.append("  - ").append(tool).append("\n");
            }
            sb.append("\n");
        }
        
        return CompletableFuture.completedFuture(sb.toString());
    }
    
    /**
     * 获取所有可用Agent列表
     */
    private CompletableFuture<String> getAvailableAgents() {
        java.util.Collection<tech.smartboot.feat.demo.featclaw.config.AgentConfig> agents = agentRegistry.getAllAgents();
        
        StringBuilder sb = new StringBuilder();
        sb.append("可用Agent列表:\n");
        sb.append("========================================\n\n");
        
        for (tech.smartboot.feat.demo.featclaw.config.AgentConfig agent : agents) {
            sb.append("- ").append(agent.getName());
            if (agent.isOrchestrator()) {
                sb.append(" [调度器]");
            }
            sb.append("\n");
            sb.append("  显示名: ").append(agent.getDisplayName()).append("\n");
            sb.append("  描述: ").append(agent.getDescription()).append("\n\n");
        }
        
        sb.append("\n共计: ").append(agents.size()).append(" 个Agent\n");
        
        return CompletableFuture.completedFuture(sb.toString());
    }
}
