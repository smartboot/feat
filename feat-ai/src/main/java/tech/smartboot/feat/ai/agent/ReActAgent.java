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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
        options.prompt(PromptTemplate.loadPrompt("feat_react_agent.tpl")).chatOptions().model(ChatModelVendor.GiteeAI.DeepSeek_V32);
        opts.accept(options);
    }

    private void internalExecute(Map<String, String> templateData, int iteration, CompletableFuture<String> future) {
        if (cancel) {
            future.completeExceptionally(new FeatException("canceled"));
            return;
        }
        if (iteration >= options.getMaxIterations()) {
            future.completeExceptionally(new FeatException("max iterations reached"));
            return;
        }
        // 恢复运行状态
        setState(AgentState.RUNNING);
        // 创建ChatModel实例
        ChatModel model = new ChatModel(options.chatOptions());
        model.chatStream(Collections.singletonList(Message.ofUser(options.getPrompt().prompt(templateData))), new StreamResponseCallback() {

            @Override
            public void onCompletion(ResponseMessage responseMessage) {
                if (!responseMessage.isSuccess()) {
                    future.completeExceptionally(new FeatException(responseMessage.getError()));
                    return;
                }
                String response = responseMessage.getContent() + '\n';
                // 解析响应并决定下一步行动
                AgentAction action = options.actionParse().parse(response);
                if (action == null) {
                    logger.info("无法解析AI模型响应：" + response);
                    future.completeExceptionally(new FeatException("invalid result:" + response));
                    return;
                }
                if (AgentAction.FINAL_ANSWER.equals(action.getAction())) {
                    // 如果是最终答案，则结束
                    future.complete(action.getActionInput());
                    return;
                }

                // 设置状态为工具执行
                setState(AgentState.TOOL_EXECUTION);

                // 执行工具
                executeTool(action.getAction(), action.getActionInput()).whenComplete((observation, throwable) -> {
                    logger.info("执行工具: {}, 输入: {}, 观察结果: {}", action.getAction(), action.getActionInput(), observation);
                    // 将动作和观察结果添加到历史记录中
                    String scratchpadEntry = String.format("Thought: %s\nAction: %s\nAction Input: %s\nObservation: %s\n", action.getThought(), action.getAction(), action.getActionInput(), observation);

                    // 更新scratchpad数据
                    String currentScratchpad = templateData.getOrDefault("agent_scratchpad", "");
                    templateData.put("agent_scratchpad", currentScratchpad + scratchpadEntry);

                    if (cancel) {
                        future.completeExceptionally(new FeatException("canceled"));
                    } else {
                        internalExecute(templateData, iteration + 1, future);
                    }
                });
            }

            @Override
            public void onStreamResponse(String content) {
                System.out.print(content);
                // 通过异常抛出，结束流
                if (cancel) {
                    throw new FeatException("canceled");
                }
            }
        });
    }

    @Override
    public CompletableFuture<String> execute(List<Message> input) {
        StringBuilder sb = new StringBuilder();
        for (Message message : input) {
            sb.append(message.getRole()).append(" : ").append(message.getContent().replace("\n", "\\n")).append("\n");
        }
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        // 准备模板数据
        Map<String, String> templateData = new HashMap<>();
        templateData.put("date", new Date().toString());
        templateData.put("input", sb.toString());
        templateData.put("tool_descriptions", getToolDescriptions());
        templateData.put("tool_names", getToolNames());
        templateData.put("relevant_memories", "无");
        templateData.put("agent_scratchpad", "无");
        templateData.put("system_prompt", options.chatOptions().getSystem());
        internalExecute(templateData, 0, completableFuture);
        return completableFuture;
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
    private CompletableFuture<String> executeTool(String toolName, String input) {
        AgentTool executor = options.getToolExecutor(toolName);
        if (executor == null) {
            return CompletableFuture.completedFuture("Error: Tool '" + toolName + "' not found.");
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

        // 使用工具执行管理器来执行工具
        return executeTool(toolName, JSONObject.parse(input));
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
    private CompletableFuture<String> executeTool(String toolName, JSONObject parameters) {
        AgentTool executor = options.getToolExecutor(toolName);
        if (executor == null) {
            return CompletableFuture.completedFuture("错误：未找到名为 '" + toolName + "' 的工具");
        }
        return executor.execute(parameters);
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
}