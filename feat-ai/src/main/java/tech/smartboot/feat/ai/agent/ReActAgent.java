/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.memory.MemoryMessage;
import tech.smartboot.feat.ai.agent.memory.MemoryRole;
import tech.smartboot.feat.ai.agent.tools.FileOperationTool;
import tech.smartboot.feat.ai.agent.tools.SearchTool;
import tech.smartboot.feat.ai.agent.tools.WebPageReaderTool;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.ai.chat.prompt.PromptTemplate;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
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
        this(opts -> opts.tool(new FileOperationTool()).tool(new SearchTool()).tool(new WebPageReaderTool()).chatOptions().model(ChatModelVendor.GiteeAI.DeepSeek_V32));

    }

    public ReActAgent(Consumer<AgentOptions> opts) {
        options.prompt(new Prompt(PromptTemplate.loadPrompt("feat_react_agent.tpl"))).chatOptions().model(ChatModelVendor.GiteeAI.DeepSeek_V32);
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
                ToolCaller action = options.actionParse().parse(response);
                if (action == null) {
                    logger.info("无法解析AI模型响应：" + response);
                    future.completeExceptionally(new FeatException("invalid result:" + response));
                    return;
                }
                if (ToolCaller.FINAL_ANSWER.equals(action.getAction())) {
                    // 如果是最终答案，则结束
                    future.complete(action.getActionInput());
                    return;
                }

                // 设置状态为工具执行
                setState(AgentState.TOOL_EXECUTION);
                options.hook().preTool(action);
                // 执行工具
                executeTool(action.getAction(), action.getActionInput()).whenComplete((observation, throwable) -> {
                    action.setObservation(observation);
                    action.setThrowable(throwable);
                    logger.info("执行工具: {}, 输入: {}, 观察结果: {}", action.getAction(), action.getActionInput(), observation);
                    options.hook().postTool(action);
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


            // 解析流式响应，区分推理部分和结果部分
            final StringBuilder stream = new StringBuilder();
            final int PHASE_WAIT_REASONING = 0;
            final int PHASE_REASONING = 1;
            final int PHASE_WAIT_RESULT = 2;
            final int PHASE_RESULT = 3;

            // 记录当前阶段：0=等待Thought, 1=推理中, 2=结果中
            int phase = PHASE_WAIT_REASONING;
            int index = 0;

            @Override
            public void onStreamResponse(String content) {
                System.out.print(content);
                if (cancel) {
                    throw new FeatException("canceled");
                }
                stream.append(content);
                // 处理推理部分
                if (phase == PHASE_WAIT_REASONING) {
                    int thoughtPos = stream.indexOf("Thought:");
                    if (thoughtPos == -1) {
                        return;
                    }
                    index = thoughtPos + 8;
                    phase = PHASE_REASONING;
                }
                // 处理推理结果
                if (phase == PHASE_REASONING) {
                    int endPos = stream.indexOf("\n", index);
                    if (endPos == -1) {
                        options.hook().onAgentReasoning(stream.substring(index));
                        index = stream.length();
                        return;
                    }
                    options.hook().onModelReasoning(stream.substring(index, endPos));
                    index = endPos + 1;
                    phase = PHASE_WAIT_RESULT;
                }
                if (phase == PHASE_WAIT_RESULT) {
                    int actionPos = stream.indexOf("\nAI:");
                    if (actionPos == -1) {
                        return;
                    }
                    phase = PHASE_RESULT;
                    index = actionPos + 4;
                }
                if (phase == PHASE_RESULT) {
                    options.hook().onFinalAnswer(stream.substring(index));
                    index = stream.length();
                }
            }

            @Override
            public void onReasoning(String content) {
                // 通过异常抛出，结束流
                if (cancel) {
                    throw new FeatException("canceled");
                }
                options.hook().onModelReasoning(content);
            }
        });
    }

    @Override
    public CompletableFuture<String> execute(List<Message> input) {
        options.hook().preCall(input);
        StringBuilder sb = new StringBuilder();
        for (Message message : input) {
            sb.append(message.getRole()).append(" : ").append(message.getContent().replace("\n", "\\n")).append("\n");
        }
        
        // 将输入消息存入记忆系统
        storeMessagesToMemory(input);
        
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        // 准备模板数据
        Map<String, String> templateData = new HashMap<>();
        templateData.put("date", new Date().toString());
        templateData.put("input", sb.toString());
        templateData.put("tool_descriptions", getToolDescriptions());
        templateData.put("tool_names", getToolNames());
        
        // 检索相关记忆
        String relevantMemories = retrieveRelevantMemories(input);
        templateData.put("relevant_memories", relevantMemories);
        
        templateData.put("agent_scratchpad", "无");
        templateData.put("system_prompt", options.chatOptions().getSystem());
        
        internalExecute(templateData, 0, completableFuture);
        completableFuture.thenApply(result -> {
            // 将结果存入记忆系统
            storeResultToMemory(result);
            
            Message output = Message.ofAssistant(result);
            options.hook().postCall(output);
            return output.getContent();
        });
        return completableFuture;
    }

    /**
     * 将输入消息存入记忆系统
     *
     * @param messages 输入消息列表
     */
    private void storeMessagesToMemory(List<Message> messages) {
        if (!options.isMemoryEnabled() || options.getMemory() == null) {
            return;
        }
        
        List<MemoryMessage> memoryMessages = new ArrayList<>();
        String sessionId = options.getSessionId();
        
        for (Message message : messages) {
            MemoryRole role = convertToMemoryRole(message.getRole());
            MemoryMessage memoryMessage = new MemoryMessage();
            memoryMessage.setContent(message.getContent());
            memoryMessage.setRole(role);
            memoryMessage.setSessionId(sessionId);
            memoryMessage.setTimestamp(System.currentTimeMillis());
            memoryMessages.add(memoryMessage);
        }
        
        if (!memoryMessages.isEmpty()) {
            options.getMemory().add(memoryMessages);
            logger.debug("存储 {} 条用户输入消息到记忆系统", memoryMessages.size());
        }
    }

    /**
     * 将执行结果存入记忆系统
     *
     * @param result 执行结果
     */
    private void storeResultToMemory(String result) {
        if (!options.isMemoryEnabled() || options.getMemory() == null) {
            return;
        }
        
        MemoryMessage memoryMessage = MemoryMessage.ofAssistant(result);
        memoryMessage.setSessionId(options.getSessionId());
        memoryMessage.setTimestamp(System.currentTimeMillis());
        memoryMessage.setImportance(1.2); // AI回复通常比较重要
        
        options.getMemory().add(memoryMessage);
        logger.debug("存储AI回复到记忆系统");
    }

    /**
     * 检索相关记忆
     *
     * @param input 当前输入消息
     * @return 相关记忆字符串，用于添加到提示词中
     */
    private String retrieveRelevantMemories(List<Message> input) {
        if (!options.isMemoryEnabled() || options.getMemory() == null) {
            return "无";
        }
        
        // 提取查询内容
        StringBuilder queryBuilder = new StringBuilder();
        for (Message message : input) {
            if ("user".equalsIgnoreCase(message.getRole())) {
                queryBuilder.append(message.getContent()).append(" ");
            }
        }
        String query = queryBuilder.toString().trim();
        
        if (query.isEmpty()) {
            return "无";
        }
        
        try {
            List<MemoryMessage> relevantMemories = options.getMemory().search(query, options.getMemoryTopK());
            
            if (relevantMemories.isEmpty()) {
                return "无";
            }
            
            // 格式化记忆为字符串
            StringBuilder memoriesBuilder = new StringBuilder();
            memoriesBuilder.append("以下是相关的历史对话记忆，请参考：\n\n");
            
            for (MemoryMessage memory : relevantMemories) {
                memoriesBuilder.append("[").append(memory.getRole().getDisplayName()).append("]: ");
                // 限制单条记忆长度
                String content = memory.getContent();
                if (content.length() > 500) {
                    content = content.substring(0, 500) + "...";
                }
                memoriesBuilder.append(content).append("\n\n");
            }
            
            logger.debug("检索到 {} 条相关记忆", relevantMemories.size());
            return memoriesBuilder.toString();
            
        } catch (Exception e) {
            logger.warn("检索记忆时发生错误: {}", e.getMessage());
            return "无";
        }
    }

    /**
     * 将Message的role转换为MemoryRole
     *
     * @param role Message的role字符串
     * @return 对应的MemoryRole
     */
    private MemoryRole convertToMemoryRole(String role) {
        if (role == null) {
            return MemoryRole.USER;
        }
        switch (role.toLowerCase()) {
            case "user":
                return MemoryRole.USER;
            case "assistant":
            case "ai":
                return MemoryRole.ASSISTANT;
            case "system":
                return MemoryRole.SYSTEM;
            default:
                return MemoryRole.USER;
        }
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
        try {
            return executeTool(toolName, JSONObject.parse(input));
        } catch (Throwable e) {
            logger.error("exception tool:{} input:{} ", toolName, input, e);
            return CompletableFuture.completedFuture("Error: Invalid input format.");
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