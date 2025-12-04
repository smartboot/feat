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
import tech.smartboot.feat.ai.agent.tools.FileOperationTool;
import tech.smartboot.feat.ai.agent.tools.SearchTool;
import tech.smartboot.feat.ai.agent.tools.SubAgentTool;
import tech.smartboot.feat.ai.agent.tools.TodoListTool;
import tech.smartboot.feat.ai.agent.tools.WebPageReaderTool;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.prompt.PromptTemplate;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于ReAct范式的AI Agent实现
 * <p>
 * ReAct = Reasoning + Acting，是一种结合推理和行动的AI代理框架。
 * 该实现在处理复杂任务时，Agent会交替进行思考（Reasoning）和行动（Acting）：
 * 1. Thought（思考）: 分析当前情况和下一步行动
 * 2. Action（行动）: 执行具体操作，如调用工具
 * 3. Observation（观察）: 观察行动结果
 * 4. 重复上述过程直到问题解决
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ReActAgent extends FeatAgent {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(ReActAgent.class.getName());

    /**
     * 匹配思考步骤的正则表达式
     * <p>
     * 用于从AI模型的响应中提取Thought部分的内容。
     * 格式示例：Thought: 我需要搜索相关信息
     * </p>
     */
    private static final Pattern THOUGHT_PATTERN = Pattern.compile("Thought:\\s*(.+)");

    /**
     * 匹配动作步骤的正则表达式
     * <p>
     * 用于从AI模型的响应中提取Action部分的内容，即要执行的工具名称。
     * 格式示例：Action: search
     * </p>
     */
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*([\\w_]+)");

    /**
     * 匹配动作输入的正则表达式
     * <p>
     * 用于从AI模型的响应中提取Action Input部分的内容，即传递给工具的参数。
     * 格式示例：Action Input: {"query": "最新的人工智能技术"}
     * </p>
     */
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input:\\s*(.+)", Pattern.DOTALL);

    /**
     * 匹配最终答案的正则表达式
     * <p>
     * 用于从AI模型的响应中提取最终答案。
     * 格式示例：AI: 这是问题的答案...
     * </p>
     */
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("AI:\\s*(.+)");

    /**
     * 构造函数，初始化时注册标准工具集
     * <p>
     * 在创建ReActAgent实例时，会自动注册一组常用的工具：
     * 1. TodoListTool: 任务列表管理工具
     * 2. FileOperationTool: 文件操作工具
     * 3. SearchTool: 网络搜索工具
     * 4. WebPageReaderTool: 网页内容读取工具
     * 5. SubAgentTool: 子代理工具
     * </p>
     * <p>
     * 同时设置默认的提示词模板和AI模型配置。
     * </p>
     */
    public ReActAgent() {
        this(opts -> opts.addTool(new TodoListTool()).addTool(new FileOperationTool()).addTool(new SearchTool()).addTool(new WebPageReaderTool()).addTool(new SubAgentTool()).chatOptions().model(ChatModelVendor.GiteeAI.DeepSeek_V32));

    }

    public ReActAgent(Consumer<AgentOptions> opts) {
        opts.accept(options);
        options.prompt(PromptTemplate.loadPrompt("feat_react_agent.tpl"));
    }

    /**
     * Agent执行入口
     * <p>
     * 处理用户输入的核心方法，实现ReAct循环流程：
     * 1. 初始化执行环境和上下文
     * 2. 进入推理循环（最大迭代次数由配置决定）
     * 3. 每次迭代中调用AI模型获取响应
     * 4. 解析响应并执行相应操作（工具调用或返回结果）
     * 5. 更新执行历史记录（scratchpad）
     * 6. 循环直至得到最终答案或达到最大迭代次数
     * </p>
     *
     * @param input 用户输入的任务描述或问题
     * @return 执行结果，包含问题的答案或处理过程的总结
     */
    @Override
    public String execute(String input) {
        // 设置状态为运行中
        setState(AgentState.RUNNING);

        try {
            // 初始化Agent状态
            StringBuilder fullResponse = new StringBuilder();

            // 准备模板数据
            Map<String, String> templateData = new HashMap<>();
            templateData.put("date", new Date().toString());
            templateData.put("input", input);
            templateData.put("tool_descriptions", getToolDescriptions());
            templateData.put("tool_names", getToolNames());


//            templateData.put("relevant_memories", memoryContext.toString());
            templateData.put("agent_scratchpad", "");

            // 执行推理循环
            int maxIterations = options.getMaxIterations(); // 使用配置的最大迭代次数
            for (int i = 0; i < maxIterations; i++) {
                fullResponse.setLength(0);
                CompletableFuture<Boolean> isDone = new CompletableFuture<>();
                // 创建ChatModel实例
                ChatModel model = new ChatModel(options.chatOptions());
                model.chatStream(Collections.singletonList(Message.ofUser(options.getPrompt().prompt(templateData))), new StreamResponseCallback() {

                    @Override
                    public void onCompletion(ResponseMessage responseMessage) {
                        if (responseMessage.isSuccess()) {
                            isDone.complete(true);
                        } else {
                            isDone.completeExceptionally(new FeatException(responseMessage.getError()));
                        }
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
                logger.info("执行工具: {}, 输入: {}, 观察结果: {}", action.getAction(), action.getActionInput(), observation);

                // 将动作和观察结果添加到历史记录中
                String scratchpadEntry = String.format("Thought: %s\nAction: %s\nAction Input: %s\nObservation: %s\n", action.getThought(), action.getAction(), action.getActionInput(), observation);

                // 更新scratchpad数据
                String currentScratchpad = templateData.getOrDefault("agent_scratchpad", "");
                templateData.put("agent_scratchpad", currentScratchpad + scratchpadEntry);

                // 恢复运行状态
                setState(AgentState.RUNNING);
            }

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
     * 解析Agent响应，提取动作信息
     * <p>
     * 从AI模型的响应文本中提取关键信息，包括：
     * 1. Thought（思考）: Agent的推理过程
     * 2. Action（行动）: 要执行的操作或工具
     * 3. Action Input（行动输入）: 传递给工具的参数
     * </p>
     * <p>
     * 如果响应中包含最终答案（以"AI:"开头），则将其识别为最终结果。
     * </p>
     *
     * @param response AI模型的原始响应文本
     * @return 解析得到的Agent动作对象，如果无法解析则返回null
     */
    private AgentAction parseAgentResponse(String response) {
        AgentAction action = new AgentAction();

        int lastAI = response.lastIndexOf("AI:");
        int lastAction = response.lastIndexOf("Action:");

        // 查找最终答案
        if (lastAI > lastAction) {
            Matcher finalAnswerMatcher = FINAL_ANSWER_PATTERN.matcher(response);
            if (finalAnswerMatcher.find()) {
                action.setAction("Final Answer");
                action.setActionInput(finalAnswerMatcher.group(1));
                return action;
            }
        }

        // 查找思考步骤
        int lastThought = response.lastIndexOf("Thought:");
        if (lastThought > 0) {
            Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(response.substring(lastThought));
            if (thoughtMatcher.find()) {
                action.setThought(thoughtMatcher.group(1));
            }
        }

        response = response.substring(lastAction);

        // 查找动作
        Matcher actionMatcher = ACTION_PATTERN.matcher(response);
        if (actionMatcher.find()) {
            action.setAction(actionMatcher.group(1).trim());
        }

        // 查找动作输入
        Matcher actionInputMatcher = ACTION_INPUT_PATTERN.matcher(response);
        if (actionInputMatcher.find()) {
            String actionInput = actionInputMatcher.group(1).trim();
            // 将换行符标准化为 \n
            actionInput = actionInput.replaceAll("\\R", "\n");
            action.setActionInput(actionInput);
        }

        // 如果有动作但没有输入，则返回null
        if (action.getAction() != null && action.getActionInput() == null) {
            return null;
        }

        return action.getAction() != null ? action : null;
    }

    /**
     * 执行工具
     * <p>
     * 根据工具名称和输入参数执行对应的工具操作。
     * 支持两种输入格式：
     * 1. 字符串格式：需要解析为JSON对象
     * 2. JSON对象格式：直接使用
     * </p>
     *
     * @param toolName 工具名称
     * @param input    工具输入（字符串格式）
     * @return 工具执行结果
     */
    private String executeTool(String toolName, String input) {
        AgentTool executor = options.getToolExecutor(toolName);
        if (executor == null) {
            return "Error: Tool '" + toolName + "' not found.";
        }
        int start = 0;
        for (; start < input.length(); start++) {
            if (input.charAt(start) != '`' && input.charAt(start) != ' ' && input.charAt(start) != '\n') {
                break;
            }
        }
        int end = input.length() - 1;
        for (; end > 0; end--) {
            if (input.charAt(end) != '`' && input.charAt(end) != ' ' && input.charAt(end) != '\n') {
                break;
            }
        }
        input = input.substring(start, end + 1);

        try {
            // 使用工具执行管理器来执行工具
            return executeTool(toolName, JSONObject.parse(input));
        } catch (Exception e) {
            return "Error executing tool '" + toolName + "': " + e.getMessage();
        }
    }

    /**
     * 执行工具
     * <p>
     * 根据工具名称和JSON参数执行对应的工具操作。
     * 这是工具执行的核心方法，负责调用具体工具的execute方法。
     * </p>
     *
     * @param toolName   工具名称
     * @param parameters 工具参数（JSON对象格式）
     * @return 工具执行结果
     */
    private String executeTool(String toolName, JSONObject parameters) {
        AgentTool executor = options.getToolExecutor(toolName);
        if (executor == null) {
            return "错误：未找到名为 '" + toolName + "' 的工具";
        }

        try {
            return executor.execute(parameters);
        } catch (Exception e) {
            return "执行工具 '" + toolName + "' 时出错: " + e.getMessage();
        }
    }

    /**
     * 获取所有工具的描述信息
     * <p>
     * 生成所有已注册工具的描述信息字符串，用于构建提示词模板的一部分，
     * 帮助AI模型了解可用工具及其功能。
     * </p>
     *
     * @return 工具描述字符串，每行包含工具名称和功能描述
     */
    private String getToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        for (AgentTool executor : options.getToolExecutors().values()) {
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
     * <p>
     * 生成已注册工具名称的逗号分隔列表，用于提示词模板中告知AI模型可用工具的范围。
     * </p>
     *
     * @return 工具名称列表字符串
     */
    private String getToolNames() {
        return String.join(", ", options.getToolExecutors().keySet());
    }

    /**
     * Agent动作内部类
     * <p>
     * 用于封装Agent在ReAct循环中的一次完整动作，包括：
     * 1. Thought（思考）: Agent的推理内容
     * 2. Action（行动）: 要执行的操作
     * 3. Action Input（行动输入）: 操作的参数
     * </p>
     */
    private static class AgentAction {
        /**
         * 思考内容
         * <p>
         * Agent对当前情况的分析和下一步行动计划的思考。
         * </p>
         */
        private String thought;

        /**
         * 动作名称
         * <p>
         * 要执行的具体操作或工具名称。
         * </p>
         */
        private String action;

        /**
         * 动作输入参数
         * <p>
         * 传递给动作或工具的参数信息。
         * </p>
         */
        private String actionInput;

        /**
         * 获取思考内容
         *
         * @return 思考内容
         */
        public String getThought() {
            return thought;
        }

        /**
         * 设置思考内容
         *
         * @param thought 思考内容
         */
        public void setThought(String thought) {
            this.thought = thought;
        }

        /**
         * 获取动作名称
         *
         * @return 动作名称
         */
        public String getAction() {
            return action;
        }

        /**
         * 设置动作名称
         *
         * @param action 动作名称
         */
        public void setAction(String action) {
            this.action = action;
        }

        /**
         * 获取动作输入参数
         *
         * @return 动作输入参数
         */
        public String getActionInput() {
            return actionInput;
        }

        /**
         * 设置动作输入参数
         *
         * @param actionInput 动作输入参数
         */
        public void setActionInput(String actionInput) {
            this.actionInput = actionInput;
        }
    }
}