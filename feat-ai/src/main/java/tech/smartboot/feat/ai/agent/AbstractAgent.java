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

import tech.smartboot.feat.ai.agent.memory.AgentMemory;
import tech.smartboot.feat.ai.agent.memory.Memory;
import tech.smartboot.feat.ai.agent.tool.ToolExecutionManager;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AI Agent抽象实现类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.1
 */
public abstract class AbstractAgent implements Agent {
    
    /**
     * Agent名称
     */
    private final String name;
    
    /**
     * Agent描述
     */
    private String description;
    
    /**
     * 底层ChatModel
     */
    protected final ChatModel chatModel;
    
    /**
     * Agent记忆
     */
    protected final AgentMemory memory;
    
    /**
     * 工具执行器映射
     */
    protected final Map<String, ToolExecutor> toolExecutors = new HashMap<>();
    
    /**
     * 工具执行管理器
     */
    protected final ToolExecutionManager toolExecutionManager = new ToolExecutionManager();
    
    /**
     * 日志记录器
     */
    protected static final Logger logger = LoggerFactory.getLogger(AbstractAgent.class.getName());
    
    public AbstractAgent(String name, ChatModel chatModel, AgentMemory memory) {
        this.name = name;
        this.chatModel = chatModel;
        this.memory = memory;
        this.description = "基于 " + chatModel.getClass().getSimpleName() + " 的AI Agent";
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ChatModel getChatModel() {
        return chatModel;
    }
    
    @Override
    public AgentMemory getMemory() {
        return memory;
    }
    
    @Override
    public void addTool(ToolExecutor executor) {
        toolExecutors.put(executor.getName(), executor);
        toolExecutionManager.addToolExecutor(executor.getName(), executor);
        logger.info("添加工具执行器: " + executor.getName());
    }
    
    @Override
    public void removeTool(String toolName) {
        toolExecutors.remove(toolName);
        toolExecutionManager.removeToolExecutor(toolName);
        logger.info("移除工具执行器: " + toolName);
    }
    
    @Override
    public CompletableFuture<ResponseMessage> chat(String content, ChatOptions options) {
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        chat(content, options, future::complete);
        return future;
    }
    
    @Override
    public void chatStream(String content, ChatOptions options, StreamResponseCallback callback) {
        logger.info("开始执行对话: " + content.substring(0, Math.min(100, content.length())) + "...");
        
        if (options != null && options.isEnableTools()) {
            // 带工具调用的流式对话
            registerToolsWithChatModel();
            List<String> toolNames = options.getToolNames().isEmpty() ? 
                new ArrayList<>(toolExecutors.keySet()) : options.getToolNames();
            logger.info("可用工具: " + toolNames);
            
            chatModel.chatStream(content, toolNames, callback);
        } else {
            // 普通流式对话
            chatModel.chatStream(content, callback);
        }
    }
    
    /**
     * 执行对话（支持工具调用和记忆检索）
     *
     * @param content 用户输入内容
     * @param options 对话选项
     * @param callback 回调函数
     */
    protected void chat(String content, ChatOptions options, Consumer<ResponseMessage> callback) {
        logger.info("开始执行对话: " + content.substring(0, Math.min(100, content.length())) + "...");
        
        String finalContent = content;
        
        // 处理记忆检索
        if (options != null && options.isEnableMemory()) {
            List<Memory> relevantMemories = retrieveRelevantMemories(content, options);
            finalContent = buildEnhancedContent(content, relevantMemories);
        }
        
        if (options != null && options.isEnableTools()) {
            // 带工具调用的对话
            registerToolsWithChatModel();
            List<String> toolNames = options.getToolNames().isEmpty() ? 
                new ArrayList<>(toolExecutors.keySet()) : options.getToolNames();
            logger.info("可用工具: " + toolNames);
            
            chatModel.chat(finalContent, toolNames, response -> {
                toolExecutionManager.handleToolResponse(chatModel, response, callback);
            });
        } else {
            // 普通对话
            chatModel.chat(finalContent, callback);
        }
    }
    
    @Override
    public void addMemory(Memory memory) {
        this.memory.addMemory(memory);
        logger.info("添加记忆到Agent");
    }
    
    @Override
    public void clearHistory() {
        int historySize = chatModel.getHistory().size();
        chatModel.getHistory().clear();
        logger.info("清空对话历史，之前记录数: " + historySize);
    }
    
    @Override
    public List<Message> getHistory() {
        return chatModel.getHistory();
    }
    
    /**
     * 将工具执行器注册到ChatModel中
     */
    private void registerToolsWithChatModel() {
        // 清除旧的工具定义
        chatModel.getOptions().functions().clear();
        
        // 添加当前所有工具执行器到ChatModel
        toolExecutors.forEach((name, executor) -> {
            // 创建Function对象
            tech.smartboot.feat.ai.chat.entity.Function function = 
                new tech.smartboot.feat.ai.chat.entity.Function(name)
                    .description(executor.getDescription());
            
            // 解析参数定义并设置到Function中
            try {
                String schema = executor.getParametersSchema();
                if (schema != null && !schema.trim().isEmpty()) {
                    // 使用JSON库解析参数schema
                    Object parsedSchema = parseParametersSchema(schema);
                    if (parsedSchema instanceof java.util.Map) {
                        applyParametersToFunction(function, (java.util.Map<String, Object>) parsedSchema);
                    }
                }
            } catch (Exception e) {
                // 如果解析失败，至少添加工具名称和描述
                function.description(executor.getDescription() + " (参数解析失败: " + e.getMessage() + ")");
            }
            
            // 将Function添加到ChatModel选项中
            chatModel.getOptions().addFunction(function);
        });
    }
    
    /**
     * 解析参数schema
     */
    private Object parseParametersSchema(String schema) {
        // 使用反射调用JSON解析库
        try {
            Class<?> jsonClass = Class.forName("com.alibaba.fastjson2.JSONObject");
            java.lang.reflect.Method parseMethod = jsonClass.getMethod("parseObject", String.class);
            return parseMethod.invoke(null, schema);
        } catch (Exception e) {
            // 如果fastjson2不可用，尝试其他JSON库或简单解析
            return parseSimpleSchema(schema);
        }
    }
    
    /**
     * 简单schema解析（备用方案）
     */
    private java.util.Map<String, Object> parseSimpleSchema(String schema) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        // 这里可以实现简单的JSON解析逻辑
        // 暂时返回空Map
        return result;
    }
    
    /**
     * 将参数应用到Function对象
     */
    private void applyParametersToFunction(tech.smartboot.feat.ai.chat.entity.Function function, 
                                         java.util.Map<String, Object> schema) {
        java.util.Map<String, Object> properties = (java.util.Map<String, Object>) schema.get("properties");
        if (properties != null) {
            for (java.util.Map.Entry<String, Object> entry : properties.entrySet()) {
                String paramName = entry.getKey();
                java.util.Map<String, Object> param = (java.util.Map<String, Object>) entry.getValue();
                String type = (String) param.get("type");
                String description = (String) param.get("description");
                boolean required = false;
                
                // 检查是否为必需参数
                java.util.List<String> requiredList = (java.util.List<String>) schema.get("required");
                if (requiredList != null) {
                    required = requiredList.contains(paramName);
                }
                
                // 根据类型添加参数
                if ("string".equals(type)) {
                    function.addStringParam(paramName, description, required);
                } else if ("integer".equals(type)) {
                    function.addIntParam(paramName, description, required);
                } else if ("float".equals(type) || "double".equals(type)) {
                    function.addDoubleParam(paramName, description, required);
                } else {
                    // 默认作为字符串处理
                    function.addStringParam(paramName, description, required);
                }
            }
        }
    }
    
    /**
     * 检索相关记忆
     */
    private List<Memory> retrieveRelevantMemories(String content, ChatOptions options) {
        if (options.isEnableMemory()) {
            return memory.getMemoriesByImportance(options.getMemoryRetrievalThreshold());
        }
        return new ArrayList<>();
    }
    
    /**
     * 构建增强的对话内容
     */
    private String buildEnhancedContent(String originalContent, List<Memory> memories) {
        if (memories.isEmpty()) {
            return originalContent;
        }
        
        StringBuilder enhanced = new StringBuilder();
        enhanced.append(originalContent).append("\n\n");
        enhanced.append("相关上下文信息:\n");
        
        for (int i = 0; i < Math.min(memories.size(), 5); i++) {
            Memory memory = memories.get(i);
            enhanced.append("- ").append(memory.getContent()).append("\n");
        }
        
        enhanced.append("\n请基于以上上下文信息回答我的问题。");
        
        return enhanced.toString();
    }
}