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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentOptions;
import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.ai.mcp.client.McpClient;
import tech.smartboot.feat.ai.mcp.model.McpInitializeResponse;
import tech.smartboot.feat.ai.mcp.model.Tool;
import tech.smartboot.feat.ai.mcp.model.ToolCalledResult;
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.core.common.FeatUtils;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class McpTool implements AgentTool {

    private final McpClient mcpClient;
    private final Tool tool;

    /**
     * 创建MCP工具实例
     *
     * @param client MCP客户端
     * @param tool   MCP工具定义
     */
    public McpTool(McpClient client, Tool tool) {
        this.mcpClient = client;
        this.tool = tool;
    }


    public static McpClient register(AgentOptions options, McpClient mcpClient) {
        McpInitializeResponse initialize = mcpClient.initialize();
        mcpClient.listTools().getTools().forEach(tool -> {
            options.tool(new McpTool(mcpClient, tool) {
                @Override
                public String getName() {
                    if (FeatUtils.isNotBlank(initialize.getServerInfo().getName())) {
                        return initialize.getServerInfo().getName() + "_" + tool.getName();
                    } else {
                        return super.getName();
                    }
                }
            });
        });
        return mcpClient;
    }

    @Override
    public CompletableFuture<String> execute(JSONObject parameters) {
        return mcpClient.asyncCallTool(tool.getName(), parameters).thenApply(this::formatResult);
    }

    /**
     * 格式化工具执行结果
     *
     * @param result 工具调用结果
     * @return 格式化后的字符串
     */
    private String formatResult(ToolCalledResult result) {
        if (result == null) {
            return "";
        }
        if (result.isError()) {
            return "Error: " + extractContent(result);
        }
        return extractContent(result);
    }

    /**
     * 从结果中提取内容
     *
     * @param result 工具调用结果
     * @return 提取的内容字符串
     */
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
        return "mcpTool" + this.hashCode() + "_" + tool.getName();
    }

    @Override
    public String getDescription() {
        return tool.getDescription() + (tool.getAnnotations() == null ? "" : (" annotations:" + tool.getAnnotations()));
    }

    @Override
    public String getParametersSchema() {
        return JSONObject.from(tool.getInputSchema()).toJSONString();
    }

}
