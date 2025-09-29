/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tool;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 工具执行管理器
 * 负责执行Agent的工具调用并处理结果
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.1
 */
public class ToolExecutionManager {

    private final Map<String, ToolExecutor> toolExecutors = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ToolExecutionManager.class.getName());

    public ToolExecutionManager() {
    }

    /**
     * 添加工具执行器
     *
     * @param name     工具名称
     * @param executor 工具执行器
     */
    public void addToolExecutor(String name, ToolExecutor executor) {
        toolExecutors.put(name, executor);
    }

    /**
     * 移除工具执行器
     *
     * @param name 工具名称
     */
    public void removeToolExecutor(String name) {
        toolExecutors.remove(name);
    }

    /**
     * 执行工具调用
     *
     * @param toolCalls 工具调用列表
     * @return 工具执行结果映射
     */
    public Map<String, String> executeTools(List<ToolCall> toolCalls) {
        Map<String, String> results = new HashMap<>();

        if (toolCalls == null || toolCalls.isEmpty()) {
            logger.info("没有工具调用需要执行");
            return results;
        }

        logger.info("开始执行 " + toolCalls.size() + " 个工具调用");

        for (ToolCall toolCall : toolCalls) {
            String toolName = toolCall.getFunction().get("name");
            String arguments = toolCall.getFunction().get("arguments");

            logger.info("执行工具: " + toolName + ", 参数: " + arguments);

            ToolExecutor executor = toolExecutors.get(toolName);
            if (executor == null) {
                String errorMsg = "错误：未找到名为 '" + toolName + "' 的工具";
                logger.warn(errorMsg);
                results.put(toolName, errorMsg);
                continue;
            }

            try {
                JSONObject params = JSONObject.parseObject(arguments);
                String result = executor.execute(params);
                logger.info("工具 " + toolName + " 执行成功");
                results.put(toolName, result);
            } catch (Exception e) {
                String errorMsg = "执行工具 '" + toolName + "' 时出错: " + e.getMessage();
                logger.error(errorMsg);
                results.put(toolName, errorMsg);
            }
        }

        return results;
    }

    /**
     * 异步执行工具调用
     *
     * @param toolCalls 工具调用列表
     * @return 异步执行结果
     */
    public CompletableFuture<Map<String, String>> executeToolsAsync(List<ToolCall> toolCalls) {
        return CompletableFuture.supplyAsync(() -> executeTools(toolCalls));
    }

    /**
     * 处理带有工具调用的响应
     *
     * @param chatModel     ChatModel实例
     * @param response      响应消息
     * @param finalCallback 最终回调
     */
    public void handleToolResponse(ChatModel chatModel, ResponseMessage response, Consumer<ResponseMessage> finalCallback) {
        if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
            logger.info("检测到工具调用，开始处理");

            // 执行工具调用
            Map<String, String> toolResults = executeTools(response.getToolCalls());

            // 构建工具执行结果摘要
            StringBuilder toolResultsSummary = new StringBuilder();
            toolResultsSummary.append("工具执行结果:\n");

            for (Map.Entry<String, String> entry : toolResults.entrySet()) {
                String toolName = entry.getKey();
                String result = entry.getValue();

                // 添加工具调用结果到历史记录
                Message toolResultMessage = new Message();
                toolResultMessage.setRole("tool");
                toolResultMessage.setContent(result);
                chatModel.getHistory().add(toolResultMessage);

                // 构建摘要
                toolResultsSummary.append("- ").append(toolName).append(": ");
                if (result.length() > 100) {
                    toolResultsSummary.append(result.substring(0, 100)).append("...");
                } else {
                    toolResultsSummary.append(result);
                }
                toolResultsSummary.append("\n");
            }

            logger.info("工具执行完成，继续对话获取最终响应");

            // 继续对话以获取最终响应，提供更明确的指令
            String followUpPrompt = "请基于以下工具执行结果，生成一个完整、清晰的最终回答。请整合所有工具返回的信息，给出专业的分析和建议：\n\n" +
                    toolResultsSummary.toString() +
                    "\n请确保回答内容完整、准确，并直接回答用户最初的问题。";

            chatModel.chat(followUpPrompt, finalResponse -> {
                // 标记原始响应为丢弃
                response.discard();
                logger.info("工具调用处理完成，返回最终响应");
                finalCallback.accept(finalResponse);
            });
        } else {
            // 没有工具调用，直接返回响应
            logger.info("没有检测到工具调用，直接返回响应");
            finalCallback.accept(response);
        }
    }

    /**
     * 处理流式工具调用响应
     *
     * @param chatModel    ChatModel实例
     * @param response     响应消息
     * @param toolCallback 工具执行回调
     */
    public void handleStreamToolResponse(ChatModel chatModel, ResponseMessage response, Consumer<Map<String, String>> toolCallback) {
        if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
            logger.info("检测到流式工具调用");
            Map<String, String> toolResults = executeTools(response.getToolCalls());
            toolCallback.accept(toolResults);
        } else {
            toolCallback.accept(new HashMap<>());
        }
    }
}