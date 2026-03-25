/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.featclaw.coordination;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.demo.featclaw.config.AgentConfig;
import tech.smartboot.feat.demo.featclaw.registry.AgentRegistry;
import tech.smartboot.feat.demo.featclaw.tools.AgentCoordinatorTool;
import tech.smartboot.feat.demo.featclaw.tools.CodeGeneratorTool;
import tech.smartboot.feat.demo.featclaw.tools.ProjectAnalyzerTool;
import tech.smartboot.feat.demo.featclaw.tools.ShellExecuteTool;
import tech.smartboot.feat.ai.agent.tools.FileOperationTool;
import tech.smartboot.feat.ai.agent.tools.SearchTool;
import tech.smartboot.feat.ai.agent.tools.WebPageReaderTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Agent协调器 - 管理多Agent协作
 * <p>
 * 负责：
 * 1. 从配置创建和管理Agent实例
 * 2. 协调Agent之间的通信
 * 3. 分配任务给合适的Agent
 * 4. 收集和整合执行结果
 * 5. 管理工作流状态
 * </p>
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class AgentCoordinator {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentCoordinator.class);
    
    /**
     * Agent实例缓存 Map<agentName, FeatAgent>
     */
    private final Map<String, FeatAgent> agentInstances = new ConcurrentHashMap<>();
    
    /**
     * 任务执行结果缓存 Map<taskId, TaskResult>
     */
    private final Map<String, TaskResult> taskResults = new ConcurrentHashMap<>();
    
    /**
     * Agent注册表
     */
    private final AgentRegistry agentRegistry;
    
    /**
     * 全局AI配置
     */
    private Consumer<tech.smartboot.feat.ai.agent.AgentOptions> globalConfig;
    
    /**
     * 默认超时时间（秒）
     */
    private int defaultTimeoutSeconds = 120;
    
    /**
     * 构造函数
     */
    public AgentCoordinator() {
        this.agentRegistry = AgentRegistry.getInstance();
        this.globalConfig = opts -> {}; // 默认空配置
    }
    
    /**
     * 设置全局AI配置
     */
    public void setGlobalConfig(Consumer<tech.smartboot.feat.ai.agent.AgentOptions> config) {
        this.globalConfig = config;
    }
    
    /**
     * 设置默认超时时间
     */
    public void setDefaultTimeout(int seconds) {
        this.defaultTimeoutSeconds = seconds;
    }
    
    /**
     * 初始化所有配置的Agent
     */
    public void initializeAgents() {
        logger.info("开始初始化Agent实例...");
        
        for (AgentConfig config : agentRegistry.getAllAgents()) {
            createAgent(config);
        }
        
        logger.info("Agent实例初始化完成，共 {} 个", agentInstances.size());
    }
    
    /**
     * 根据配置创建Agent实例
     */
    private void createAgent(AgentConfig config) {
        try {
            FeatAgent agent = FeatAI.agent(opts -> {
                // 应用全局配置
                globalConfig.accept(opts);
                
                // 应用Agent特定配置
                if (config.getSystemPrompt() != null) {
                    opts.systemPrompt(config.getSystemPrompt());
                }
                
                // 配置模型参数
                if (config.getModel() != null) {
                    opts.chatOptions().temperature(config.getModel().getTemperature());
                    opts.maxIterations(config.getModel().getMaxIterations());
                }
                
                // 注册工具
                registerTools(opts, config);
                
                // 配置记忆
                if (config.getMemory() != null && config.getMemory().isEnabled()) {
                    opts.enableMemory();
                }
            });
            
            agentInstances.put(config.getName(), agent);
            logger.info("已创建Agent实例: {}", config.getName());
            
        } catch (Exception e) {
            logger.error("创建Agent实例失败: {}", config.getName(), e);
        }
    }
    
    /**
     * 注册Agent所需的工具
     */
    private void registerTools(tech.smartboot.feat.ai.agent.AgentOptions opts, AgentConfig config) {
        if (config.getTools() == null) {
            return;
        }
        
        for (String toolName : config.getTools()) {
            AgentTool tool = createTool(toolName);
            if (tool != null) {
                opts.tool(tool);
                logger.debug("Agent {} 注册工具: {}", config.getName(), toolName);
            }
        }
    }
    
    /**
     * 创建工具实例
     */
    private AgentTool createTool(String toolName) {
        switch (toolName) {
            case "project_analyzer":
                return new ProjectAnalyzerTool();
            case "code_generator":
                return new CodeGeneratorTool();
            case "shell_execute":
                return new ShellExecuteTool();
            case "file_operation":
                return new FileOperationTool();
            case "search":
                return new SearchTool();
            case "web_page_reader":
                return new WebPageReaderTool();
            case "agent_coordinator":
                return new AgentCoordinatorTool(this);
            default:
                logger.warn("未知的工具类型: {}", toolName);
                return null;
        }
    }
    
    /**
     * 获取Agent实例
     */
    public FeatAgent getAgent(String name) {
        FeatAgent agent = agentInstances.get(name);
        if (agent == null) {
            // 尝试从配置创建
            AgentConfig config = agentRegistry.getAgent(name);
            if (config != null) {
                createAgent(config);
                agent = agentInstances.get(name);
            }
        }
        return agent;
    }
    
    /**
     * 向指定Agent发送任务请求
     *
     * @param agentName Agent名称
     * @param task 任务描述
     * @return 任务执行结果
     */
    public CompletableFuture<String> execute(String agentName, String task) {
        return execute(agentName, task, defaultTimeoutSeconds);
    }
    
    /**
     * 向指定Agent发送任务请求（带超时）
     */
    public CompletableFuture<String> execute(String agentName, String task, int timeoutSeconds) {
        FeatAgent agent = getAgent(agentName);
        if (agent == null) {
            return CompletableFuture.completedFuture(
                "错误：找不到Agent '" + agentName + "'");
        }
        
        String taskId = generateTaskId();
        logger.info("执行任务 [{}] -> Agent: {}, 任务: {}", taskId, agentName, task);
        
        return agent.execute(task)
//                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        logger.error("任务执行失败 [{}]: {}", taskId, error.getMessage());
                    } else {
                        logger.info("任务执行成功 [{}]", taskId);
                        // 缓存结果
                        taskResults.put(taskId, new TaskResult(taskId, agentName, result, error));
                    }
                });
    }
    
    /**
     * 并行执行多个Agent
     *
     * @param tasks Map<agentName, task>
     * @return Map<agentName, result>
     */
    public Map<String, String> executeParallel(Map<String, String> tasks) {
        return executeParallel(tasks, defaultTimeoutSeconds);
    }
    
    /**
     * 并行执行多个Agent（带超时）
     */
    public Map<String, String> executeParallel(Map<String, String> tasks, int timeoutSeconds) {
        Map<String, CompletableFuture<String>> futures = new HashMap<>();
        
        // 启动所有任务
        for (Map.Entry<String, String> entry : tasks.entrySet()) {
            String agentName = entry.getKey();
            String task = entry.getValue();
            futures.put(agentName, execute(agentName, task, timeoutSeconds));
        }
        
        // 等待所有任务完成
        Map<String, String> results = new HashMap<>();
        for (Map.Entry<String, CompletableFuture<String>> entry : futures.entrySet()) {
            try {
                String result = entry.getValue().get();
                results.put(entry.getKey(), result);
            } catch (InterruptedException | ExecutionException e) {
                results.put(entry.getKey(), "执行失败: " + e.getMessage());
            }
        }
        
        return results;
    }
    
    /**
     * 串行执行多个Agent
     *
     * @param tasks List<Map.Entry<agentName, task>>
     * @return 最后一个Agent的执行结果
     */
    public String executeSequential(List<Map.Entry<String, String>> tasks) {
        String lastResult = null;
        
        for (Map.Entry<String, String> task : tasks) {
            try {
                lastResult = execute(task.getKey(), task.getValue()).get();
            } catch (InterruptedException | ExecutionException e) {
                return "执行失败: " + e.getMessage();
            }
        }
        
        return lastResult;
    }
    
    /**
     * 发送消息给Agent
     */
    public void sendMessage(AgentMessage message) {
        logger.debug("发送消息: {}", message);
        
        if (message.isBroadcast()) {
            // 广播给所有Agent
            for (Map.Entry<String, FeatAgent> entry : agentInstances.entrySet()) {
                if (!entry.getKey().equals(message.getFrom())) {
                    handleMessage(entry.getValue(), message);
                }
            }
        } else {
            // 发送给指定Agent
            FeatAgent agent = getAgent(message.getTo());
            if (agent != null) {
                handleMessage(agent, message);
            } else {
                logger.warn("消息发送失败，Agent不存在: {}", message.getTo());
            }
        }
    }
    
    /**
     * 处理消息
     */
    private void handleMessage(FeatAgent agent, AgentMessage message) {
        // 这里可以根据消息类型做不同处理
        // 目前简单地将消息内容作为任务执行
        if (message.getType() == AgentMessage.MessageType.TASK_REQUEST ||
            message.getType() == AgentMessage.MessageType.TASK_ASSIGN) {
            agent.execute(message.getContent());
        }
    }
    
    /**
     * 获取任务结果
     */
    public TaskResult getTaskResult(String taskId) {
        return taskResults.get(taskId);
    }
    
    /**
     * 清除任务结果缓存
     */
    public void clearTaskResults() {
        taskResults.clear();
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 获取所有Agent名称
     */
    public List<String> getAvailableAgents() {
        return new ArrayList<>(agentInstances.keySet());
    }
    
    /**
     * 获取调度器Agent
     */
    public FeatAgent getOrchestratorAgent() {
        AgentConfig orchestratorConfig = agentRegistry.findOrchestrator();
        if (orchestratorConfig != null) {
            return getAgent(orchestratorConfig.getName());
        }
        return null;
    }
    
    /**
     * 任务结果内部类
     */
    public static class TaskResult {
        private final String taskId;
        private final String agentName;
        private final String result;
        private final Throwable error;
        private final long timestamp;
        
        public TaskResult(String taskId, String agentName, String result, Throwable error) {
            this.taskId = taskId;
            this.agentName = agentName;
            this.result = result;
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public String getAgentName() {
            return agentName;
        }
        
        public String getResult() {
            return result;
        }
        
        public Throwable getError() {
            return error;
        }
        
        public boolean isSuccess() {
            return error == null;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
