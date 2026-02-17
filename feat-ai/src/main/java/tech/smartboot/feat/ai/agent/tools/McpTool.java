/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.ai.mcp.client.McpClient;
import tech.smartboot.feat.ai.mcp.client.McpOptions;
import tech.smartboot.feat.ai.mcp.model.Tool;
import tech.smartboot.feat.ai.mcp.model.ToolCalledResult;
import tech.smartboot.feat.ai.mcp.model.ToolListResponse;
import tech.smartboot.feat.ai.mcp.model.ToolResult;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * MCP工具，将MCP服务器的所有工具封装为Agent可用的工具
 * <p>
 * 该工具自动从MCP服务器获取工具列表，并为每个MCP工具创建对应的Agent工具。
 * 使用示例：
 * </p>
 * <pre>
 * // 方式1：在AgentOptions中直接配置
 * ReActAgent agent = new ReActAgent(opt -> opt
 *     .mcp(mcp -> mcp.sse("http://localhost:8080/sse"))
 * );
 *
 * // 方式2：手动创建并注册
 * McpClient client = McpClient.newSseClient(opt -> opt.host("http://localhost:8080"));
 * McpTool.register(agent.options(), client);
 * </pre>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class McpTool implements AgentTool {

    private final McpClient mcpClient;
    private final String name;
    private final String description;
    private final Map<String, Tool> toolCache = new ConcurrentHashMap<>();

    public McpTool(String name, String description, Consumer<McpOptions> opt) {
        this.name = name;
        this.description = description;
        this.mcpClient = McpClient.streamable(opt);
        initialize();
    }


    private void initialize() {
        mcpClient.initialize();
        ToolListResponse response = mcpClient.listTools();
        if (response != null && response.getTools() != null) {
            for (Tool tool : response.getTools()) {
                toolCache.put(tool.getName(), tool);
            }
        }
    }

    @Override
    public CompletableFuture<String> execute(JSONObject parameters) {
        String toolName = parameters.getString("tool_name");
        JSONObject arguments = parameters.getJSONObject("arguments");

        if (toolName == null || toolName.isEmpty()) {
            return CompletableFuture.completedFuture(buildAvailableToolsInfo());
        }

        Tool tool = toolCache.get(toolName);
        if (tool == null) {
            return CompletableFuture.completedFuture("Error: Tool '" + toolName + "' not found. Available: " + String.join(", ", toolCache.keySet()));
        }

        return mcpClient.asyncCallTool(toolName, arguments).thenApply(this::formatResult);
    }

    private String buildAvailableToolsInfo() {
        StringBuilder sb = new StringBuilder("Available MCP tools:\n");
        for (Tool tool : toolCache.values()) {
            sb.append("- ").append(tool.getName());
            if (tool.getDescription() != null) {
                sb.append(": ").append(tool.getDescription());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatResult(ToolCalledResult result) {
        if (result == null) {
            return "";
        }
        if (result.isError()) {
            return "Error: " + extractContent(result);
        }
        return extractContent(result);
    }

    private String extractContent(ToolCalledResult result) {
        if (result.getContent() == null || result.getContent().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ToolResult content : result.getContent()) {
            if (content instanceof ToolResult.TextContent) {
                sb.append(((ToolResult.TextContent) content).getText());
            } else if (content instanceof ToolResult.ImageContent) {
                sb.append("[Image]");
            } else if (content instanceof ToolResult.AudioContent) {
                sb.append("[Audio]");
            } else if (content instanceof ToolResult.ResourceLinks) {
                sb.append("[Resource]");
            } else if (content instanceof ToolResult.StructuredContent) {
                sb.append(((ToolResult.StructuredContent) content).getContent().toJSONString());
            }
        }
        return sb.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description + ". Available tools: " + String.join(", ", toolCache.keySet());
    }

    @Override
    public String getParametersSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");

        JSONObject properties = new JSONObject();

        JSONObject toolNameProp = new JSONObject();
        toolNameProp.put("type", "string");
        toolNameProp.put("description", "要调用的MCP工具名称");
        toolNameProp.put("enum", new ArrayList<>(toolCache.keySet()));
        properties.put("tool_name", toolNameProp);

        JSONObject argumentsProp = new JSONObject();
        argumentsProp.put("type", "object");
        argumentsProp.put("description", "传递给MCP工具的参数，根据选择的工具不同而有不同的结构");

        // 为每个MCP工具添加参数描述
        JSONArray anyOf = new JSONArray();
        anyOf.addAll(toolCache.values());


        if (!anyOf.isEmpty()) {
            argumentsProp.put("anyOf", anyOf);
        }
        properties.put("arguments", argumentsProp);

        schema.put("properties", properties);

        JSONArray required = new JSONArray();
        required.add("tool_name");
        schema.put("required", required);

        return schema.toJSONString();
    }

    /**
     * 获取底层MCP客户端
     */
    public McpClient client() {
        return mcpClient;
    }

    /**
     * 关闭MCP连接
     */
    public void close() {
        mcpClient.close();
    }

}
