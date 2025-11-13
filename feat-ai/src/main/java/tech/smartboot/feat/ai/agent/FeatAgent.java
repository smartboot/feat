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
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
        return "基于 " + options.getDescription() + " 的AI Agent";
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
    public void execute(Map<String, String> params, StreamResponseCallback callback) {
//        if (options.getPrompt().isNoneParam()) {
//            throw new IllegalArgumentException("当前接入仅适用于有参数的Prompt");
//        }
//        // 带工具调用的流式对话
//        registerToolsWithChatModel();
//        List<String> toolNames = new ArrayList<>(toolExecutors.keySet());
//        logger.info("可用工具: " + toolNames);
//
//        chatModel.chatStream(options.getPrompt(), data -> data.putAll(params), callback);

    }

    public void execute(String input, StreamResponseCallback callback) throws ExecutionException, InterruptedException {
        if (!options.getPrompt().isNoneParam()) {
            throw new IllegalArgumentException("当前接入仅适用于无参数的Prompt");
        }
        StringBuilder builder = new StringBuilder(options.getPrompt().prompt(Collections.singletonMap(Prompt.CONTENT_PARAM_NAME, input)));
        builder.append(getThinkMessage(input));
        builder.append("\r\n");
        builder.append(currentStepEnvMessage());
        if (!toolExecutors.isEmpty()) {
            builder.append("\r\nAvailable Tool List: ");
            toolExecutors.forEach((name, executor) -> builder.append("\r\n- ").append(name).append(": ").append(executor.getDescription()));
        }
        ChatModel model = FeatAI.chatModel(chatOptions -> chatOptions.debug(false).model(options.getVendor()));
        model.chatStream(builder.toString(), callback);
    }

    private int currentStepIndex;
    private String planStatus;
    private String stepText;
    private String extraParams;
    private String executePrams;

    private String currentStepEnvMessage() {
        return "- Current step environment information";
    }

    protected String getThinkMessage(String input) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 基本信息\n");

        builder.append("系统信息:\r\n");
        builder.append("操作系统: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append(" ").append(System.getProperty("os.arch")).append("\r\n");

        builder.append("- 当前日期:\r\n").append(new Date()).append("\r\n");

        //执行计划
        builder.append("- 用户原始需求 (这是用户的初始输入，可以参考这些信息，但在当前交互轮次中只需要完成当前步骤的需求!) :\n");
        builder.append(input).append("\n\n");
        builder.append("- 执行参数: \n");
        if (FeatUtils.isNotBlank(executePrams)) {
            builder.append(executePrams).append("\n\n");
        } else {
            builder.append("未提供执行参数。\n\n");
        }
        builder.append("- 历史已执行步骤记录:\n");

        builder.append("\r\n\r\n");

        builder.append("- 当前步骤需求 (这一步需要由你来完成！这是用户原始请求所要求的，但如果当前步骤没有要求，则无需在此步骤中完成):\r\n");
        builder.append("步骤 ").append(currentStepIndex).append(": ").append(stepText).append("\r\n");
        builder.append("\r\n");

        builder.append("- 操作步骤说明:\r\n");
        if (FeatUtils.isBlank(extraParams)) {
            builder.append("无");
        } else {
            builder.append(extraParams);
        }
        builder.append("\r\n\r\n");

        builder.append("- 重要注意事项:\r\n");
        builder.append("1. 使用工具调用时，必须提供解释说明使用该工具的原因和背后的思考过程").append("\r\n");
        builder.append("2. 简要描述之前所有步骤完成了什么").append("\r\n");
        builder.append("3. 只做且 exactly 做当前步骤需求中要求的事情").append("\r\n");
        builder.append("4. 如果当前步骤需求已完成，请调用终止工具来结束当前步骤。").append("\r\n");
        builder.append("5. 用户的原始请求是为了全局理解，不要在当前步骤中完成这个用户的原始请求。").append("\r\n");

        builder.append("\r\n");
        builder.append("# 响应规则:\r\n");
        builder.append("- 当操作步骤说明为设置时，先响应操作步骤。\r\n");
        builder.append("- 当需要调用工具时，每次必须只调用一个工具。不允许同时调用多个工具。\r\n");
//        builder.append("- 在你的响应中，必须调用且仅调用一个工具，这是一个不可缺少的操作步骤。\r\n");

        builder.append("# 任务\n");

        builder.append("基于当前环境信息和提示做出下一步决策");
        builder.append("\n");
        return builder.toString();
    }

    @Override
    public void addMemory(Memory memory) {
        this.options.getMemory().addMemory(memory);
        logger.info("添加记忆到Agent");
    }

    @Override
    public void clearHistory() {
//        int historySize = chatModel.getHistory().size();
//        chatModel.getHistory().clear();
//        logger.info("清空对话历史，之前记录数: " + historySize);
        throw new UnsupportedOperationException("不支持清空历史记录");
    }

    @Override
    public List<Message> getHistory() {
//        return chatModel.getHistory();
        throw new UnsupportedOperationException("不支持获取历史记录");
    }

    /**
     * 将工具执行器注册到ChatModel中
     */
    private void registerToolsWithChatModel() {
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
//            chatModel.getOptions().addFunction(function);
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