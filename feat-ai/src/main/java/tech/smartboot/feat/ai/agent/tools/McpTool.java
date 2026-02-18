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
import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.mcp.client.McpClient;
import tech.smartboot.feat.ai.mcp.model.McpInitializeResponse;
import tech.smartboot.feat.ai.mcp.model.Tool;
import tech.smartboot.feat.ai.mcp.model.ToolCalledResult;
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.core.common.FeatUtils;

import java.util.concurrent.CompletableFuture;

/**
 * MCP工具，将MCP服务器的单个工具封装为Agent可用的工具
 * <p>
 * 该工具类用于将MCP服务器中的单个工具映射为AI Agent可识别的工具。
 * 每个McpTool实例对应MCP服务器中的一个具体工具。
 * </p>
 * <p>
 * 使用示例：
 * </p>
 * <pre>
 * // 方式1：在AgentOptions中直接配置
 * ReActAgent agent = new ReActAgent(opt -> opt
 *     .mcp(mcp -> mcp.sse("http://localhost:8080/sse"))
 * );
 *
 * // 方式2：手动创建并注册
 * McpClient client = McpClient.streamable(opt -> opt.host("http://localhost:8080"));
 * McpTool.register(agent, opt -> opt.host("http://localhost:8080"));
 * </pre>
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


    public static McpClient register(FeatAgent agent, McpClient mcpClient) {
        McpInitializeResponse initialize = mcpClient.initialize();
        mcpClient.listTools().getTools().forEach(tool -> {
            agent.options().tool(new McpTool(mcpClient, tool) {
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
        return "mcpTool_" + tool.getName();
    }

    @Override
    public String getDescription() {
        return tool.getDescription() + (tool.getAnnotations() == null ? "" : (" annotations:" + tool.getAnnotations()));
    }

    @Override
    public String getParametersSchema() {
        return JSONObject.from(tool.getInputSchema()).toJSONString();
    }

    /**
     * 获取底层MCP客户端
     *
     * @return MCP客户端实例
     */
    public McpClient client() {
        return mcpClient;
    }

    /**
     * 获取工具定义
     *
     * @return 工具定义对象
     */
    public Tool tool() {
        return tool;
    }

    /**
     * 关闭MCP连接
     */
    public void close() {
        mcpClient.close();
    }

}
