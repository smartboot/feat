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
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // 匹配工具调用的正则表达式
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile("\\{\\{tool:([\\w_]+)(?:\\(([^}]*)\\))?\\}\\}");

    // 匹配JSON参数的正则表达式
    private static final Pattern PARAM_PATTERN = Pattern.compile("(\\w+)=\"([^\"]*)\"");

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
     * 获取所有已注册的工具执行器
     *
     * @return 工具执行器映射
     */
    public Map<String, ToolExecutor> getToolExecutors() {
        return new HashMap<>(toolExecutors);
    }

    /**
     * 从文本中提取并执行工具调用
     *
     * @param text 包含工具调用的文本
     * @return 工具执行结果映射
     */
    public Map<String, String> extractAndExecuteTools(String text) {
        Map<String, String> results = new HashMap<>();

        if (text == null || text.isEmpty()) {
            logger.info("没有工具调用需要执行");
            return results;
        }

        Matcher matcher = TOOL_CALL_PATTERN.matcher(text);
        int toolCount = 0;

        while (matcher.find()) {
            toolCount++;
            String toolName = matcher.group(1);
            String arguments = matcher.group(2);

            logger.info("检测到工具调用: " + toolName + ", 参数: " + arguments);

            ToolExecutor executor = toolExecutors.get(toolName);
            if (executor == null) {
                String errorMsg = "错误：未找到名为 '" + toolName + "' 的工具";
                logger.warn(errorMsg);
                results.put(toolName, errorMsg);
                continue;
            }

            try {
                JSONObject params = new JSONObject();
                if (arguments != null && !arguments.isEmpty()) {
                    // 解析参数
                    Matcher paramMatcher = PARAM_PATTERN.matcher(arguments);
                    while (paramMatcher.find()) {
                        String paramName = paramMatcher.group(1);
                        String paramValue = paramMatcher.group(2);
                        params.put(paramName, paramValue);
                    }
                }

                String result = executor.execute(params);
                logger.info("工具 " + toolName + " 执行成功");
                results.put(toolName, result);
            } catch (Exception e) {
                String errorMsg = "执行工具 '" + toolName + "' 时出错: " + e.getMessage();
                logger.error(errorMsg);
                results.put(toolName, errorMsg);
            }
        }

        logger.info("总共检测到 " + toolCount + " 个工具调用");

        return results;
    }
    
    /**
     * 执行指定工具
     *
     * @param toolName 工具名称
     * @param parameters 工具参数
     * @return 工具执行结果
     */
    public String executeTool(String toolName, JSONObject parameters) {
        ToolExecutor executor = toolExecutors.get(toolName);
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
     * 异步执行工具调用
     *
     * @param text 包含工具调用的文本
     * @return 异步执行结果
     */
    public CompletableFuture<Map<String, String>> executeToolsAsync(String text) {
        return CompletableFuture.supplyAsync(() -> extractAndExecuteTools(text));
    }
}