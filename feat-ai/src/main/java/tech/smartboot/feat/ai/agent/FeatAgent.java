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

import tech.smartboot.feat.ai.FeatAI;
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
import java.util.function.Consumer;

/**
 * AI Agent抽象实现类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.1
 */
public class FeatAgent implements Agent {
    protected final AgentOptions options;
    private AgentState state = AgentState.IDLE;

    /**
     * 底层ChatModel
     */
    protected final ChatModel chatModel;


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
    protected static final Logger logger = LoggerFactory.getLogger(FeatAgent.class.getName());

    private String prompt;

    public FeatAgent(String prompt, Consumer<AgentOptions> opt) {
        this.prompt = prompt;
        this.options = new AgentOptions();
        opt.accept(options);
        this.chatModel = FeatAI.chatModel(chatOptions -> chatOptions.debug(false).model(options.getVendor()));
    }

    public FeatAgent(Consumer<AgentOptions> opt) {
        this("", opt);
    }

    @Override
    public String getName() {
        return options.getName();
    }

    @Override
    public String getDescription() {
        return "基于 " + chatModel.getClass().getSimpleName() + " 的AI Agent";
    }

    @Override
    public ChatModel getChatModel() {
        return chatModel;
    }

    @Override
    public AgentMemory getMemory() {
        return options.getMemory();
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

//    @Override
//    public CompletableFuture<ResponseMessage> chat(String content, ChatOptions options) {
//        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
//        chat(content, options, future::complete);
//        return future;
//    }

    @Override
    public void execute(Map<String, String> input, StreamResponseCallback callback) {

        // 带工具调用的流式对话
        registerToolsWithChatModel();
        List<String> toolNames = new ArrayList<>(toolExecutors.keySet());
        logger.info("可用工具: " + toolNames);

        chatModel.chatStream(options.getPrompt(), data -> data.putAll(input), callback);

    }

    @Override
    public void addMemory(Memory memory) {
        this.options.getMemory().addMemory(memory);
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
            tech.smartboot.feat.ai.chat.entity.Function function = new tech.smartboot.feat.ai.chat.entity.Function(name).description(executor.getDescription());

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
    private void applyParametersToFunction(tech.smartboot.feat.ai.chat.entity.Function function, java.util.Map<String, Object> schema) {
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
            return this.options.getMemory().getMemoriesByImportance(options.getMemoryRetrievalThreshold());
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

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public String getToolsPrompts() {
        if (toolExecutors.isEmpty()) {
            return "当前没有可用的工具。";
        }

        StringBuilder prompts = new StringBuilder("# 可用工具列表\n\n");
        prompts.append("你可以根据需要使用以下工具来完成任务:\n\n");

        for (ToolExecutor toolExecutor : toolExecutors.values()) {
            prompts.append("## ").append(toolExecutor.getName()).append("\n");
            prompts.append("- **描述**: ").append(toolExecutor.getDescription()).append("\n");

            String parametersSchema = toolExecutor.getParametersSchema();
            if (parametersSchema != null && !parametersSchema.trim().isEmpty()) {
                prompts.append("- **参数**: ").append(parametersSchema).append("\n");
            } else {
                prompts.append("- **参数**: 无\n");
            }
            prompts.append("\n");
        }

        prompts.append("请根据任务需求选择合适的工具。如果任务不需要使用工具，请直接回答问题。");
        return prompts.toString();
    }
}