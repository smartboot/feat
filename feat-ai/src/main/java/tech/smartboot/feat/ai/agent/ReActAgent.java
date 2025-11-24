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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.memory.Memory;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;
import tech.smartboot.feat.ai.agent.tool.standard.StandardToolsRegistry;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.ai.chat.prompt.PromptTemplate;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于ReAct范式的AI Agent实现
 * ReAct = Reasoning + Acting
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ReActAgent extends FeatAgent {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(ReActAgent.class.getName());

    // Agent当前状态
    private AgentState state = AgentState.IDLE;

    /**
     * 构造函数，初始化时注册标准工具集
     */
    public ReActAgent() {
        // 注册标准工具集
        StandardToolsRegistry.registerStandardTools(toolExecutionManager);
        toolExecutors.putAll(toolExecutionManager.getToolExecutors());
    }

    // 匹配思考步骤的正则表达式
    private static final Pattern THOUGHT_PATTERN = Pattern.compile("Thought:\\s*(.+)");

    // 匹配动作步骤的正则表达式
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*([\\w_]+)");

    // 匹配动作输入的正则表达式
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input:\\s*(.+)");

    // 匹配最终答案的正则表达式
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("AI:\\s*(.+)");

    /**
     * Agent执行入口
     *
     * @param input 用户输入
     * @return 执行结果
     */
    @Override
    public String execute(String input) {
        // 设置状态为运行中
        setState(AgentState.RUNNING);

        try {
            // 初始化Agent状态
            StringBuilder fullResponse = new StringBuilder();
//            List<Message> conversationHistory = new ArrayList<>();

            // 加载规划者模板
            Prompt plannerPrompt = new Prompt(PromptTemplate.loadPrompt("feat_agent_planner.tpl"));

            // 准备模板数据
            Map<String, String> templateData = new HashMap<>();
            templateData.put("date", new Date().toString());
            templateData.put("input", input);
            templateData.put("tool_descriptions", getToolDescriptions());
            templateData.put("tool_names", getToolNames());
//            templateData.put("history", formatConversationHistory(conversationHistory));
            templateData.put("agent_scratchpad", "");

            // 设置系统提示
            options.setSystemPrompt(plannerPrompt.prompt(templateData));
//            conversationHistory.add(Message.ofSystem(options.systemPrompt()));
            // 构造初始消息
//            Message userMessage = Message.ofUser(input);
//            conversationHistory.add(userMessage);

            // 执行推理循环
            int maxIterations = options.getMaxIterations(); // 使用配置的最大迭代次数
            for (int i = 0; i < maxIterations; i++) {
                fullResponse.setLength(0);
                CompletableFuture<Boolean> isDone = new CompletableFuture<>();
                // 调用模型
                callStream(Collections.singletonList(Message.ofUser(options.systemPrompt())), new StreamResponseCallback() {

                    @Override
                    public void onCompletion(ResponseMessage responseMessage) {
                        isDone.complete(true);
                    }

                    @Override
                    public void onStreamResponse(String content) {
                        System.out.print(content);
                        fullResponse.append(content);
                    }
                });
                isDone.get();

                String response = fullResponse.append("\n").toString();

                // 解析响应并决定下一步行动
                AgentAction action = parseAgentResponse(response);
                if (action == null) {
                    // 如果无法解析动作，则结束循环
                    break;
                }

                if ("Final Answer".equals(action.getAction())) {
                    // 如果是最终答案，则结束
                    break;
                }

                // 设置状态为工具执行
                setState(AgentState.TOOL_EXECUTION);

                // 执行工具
                String observation = executeTool(action.getAction(), action.getActionInput());

                // 将动作和观察结果添加到历史记录中
                String scratchpadEntry = String.format("Thought: %s\nAction: %s\nAction Input: %s\nObservation: %s\n", action.getThought(), action.getAction(), action.getActionInput(), observation);

                // 更新scratchpad数据
                String currentScratchpad = templateData.getOrDefault("agent_scratchpad", "");
                templateData.put("agent_scratchpad", currentScratchpad + scratchpadEntry);

                // 更新系统提示
                options.setSystemPrompt(plannerPrompt.prompt(templateData));

                // 恢复运行状态
                setState(AgentState.RUNNING);
            }

            // 将完整交互添加到记忆中
            Memory memory = new Memory() {
                @Override
                public String getContent() {
                    return "User: " + input + "\nAssistant: " + fullResponse.toString();
                }

                @Override
                public long getTimestamp() {
                    return System.currentTimeMillis();
                }

                @Override
                public double getImportance() {
                    return 0.5; // 默认重要性
                }
            };

            getMemory().addMemory(memory);

            // 设置状态为完成
            setState(AgentState.FINISHED);

            return fullResponse.toString();
        } catch (Exception e) {
            // 设置状态为错误
            setState(AgentState.ERROR);
            logger.error("Agent执行出错: " + e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 获取Agent当前状态
     *
     * @return Agent状态
     */
    public AgentState getState() {
        return state;
    }

    /**
     * 设置Agent状态
     *
     * @param state 新状态
     */
    protected void setState(AgentState state) {
        this.state = state;
        logger.info("Agent状态变更: " + state);
    }

    /**
     * 解析Agent响应，提取动作信息
     *
     * @param response 响应文本
     * @return Agent动作对象
     */
    private AgentAction parseAgentResponse(String response) {
        AgentAction action = new AgentAction();

        // 查找最终答案
        Matcher finalAnswerMatcher = FINAL_ANSWER_PATTERN.matcher(response);
        if (finalAnswerMatcher.find()) {
            action.setAction("Final Answer");
            action.setActionInput(finalAnswerMatcher.group(1));
            return action;
        }

        // 查找思考步骤
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(response);
        if (thoughtMatcher.find()) {
            action.setThought(thoughtMatcher.group(1));
        }

        // 查找动作
        Matcher actionMatcher = ACTION_PATTERN.matcher(response);
        if (actionMatcher.find()) {
            action.setAction(actionMatcher.group(1).trim());
        }

        // 查找动作输入
        Matcher actionInputMatcher = ACTION_INPUT_PATTERN.matcher(response);
        if (actionInputMatcher.find()) {
            action.setActionInput(actionInputMatcher.group(1).trim());
        }

        // 如果有动作但没有输入，则返回null
        if (action.getAction() != null && action.getActionInput() == null) {
            return null;
        }

        return action.getAction() != null ? action : null;
    }

    /**
     * 执行工具
     *
     * @param toolName 工具名称
     * @param input    工具输入
     * @return 工具执行结果
     */
    private String executeTool(String toolName, String input) {
        ToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            return "Error: Tool '" + toolName + "' not found.";
        }

        try {
            // 使用工具执行管理器来执行工具
            return toolExecutionManager.executeTool(toolName, JSONObject.parse(input));
        } catch (Exception e) {
            return "Error executing tool '" + toolName + "': " + e.getMessage();
        }
    }

    /**
     * 获取所有工具的描述信息
     *
     * @return 工具描述字符串
     */
    private String getToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        for (ToolExecutor executor : toolExecutors.values()) {
            sb.append(String.format("- %s: %s\n", executor.getName(), executor.getDescription()));
            // 添加参数信息
            String parametersSchema = executor.getParametersSchema();
            if (parametersSchema != null && !parametersSchema.isEmpty()) {
                sb.append(String.format("  参数: %s\n", parametersSchema));
            }
        }
        return sb.toString();
    }

    /**
     * 获取所有工具名称
     *
     * @return 工具名称列表
     */
    private String getToolNames() {
        return String.join(", ", toolExecutors.keySet());
    }

    /**
     * 格式化对话历史
     *
     * @param history 对话历史
     * @return 格式化后的字符串
     */
    private String formatConversationHistory(List<Message> history) {
        StringBuilder sb = new StringBuilder();
        for (Message message : history) {
            sb.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Agent动作内部类
     */
    private static class AgentAction {
        private String thought;
        private String action;
        private String actionInput;

        public String getThought() {
            return thought;
        }

        public void setThought(String thought) {
            this.thought = thought;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getActionInput() {
            return actionInput;
        }

        public void setActionInput(String actionInput) {
            this.actionInput = actionInput;
        }
    }
}