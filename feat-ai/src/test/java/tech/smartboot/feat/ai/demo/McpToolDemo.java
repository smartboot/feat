/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.demo;

import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.agent.ReActAgent;
import tech.smartboot.feat.ai.agent.tools.McpTool;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.ai.mcp.client.McpClient;
import tech.smartboot.feat.ai.mcp.model.McpInitializeResponse;
import tech.smartboot.feat.ai.mcp.model.Tool;
import tech.smartboot.feat.core.common.FeatUtils;

import java.util.List;

/**
 * McpTool 使用示例
 * <p>
 * 本Demo展示了如何使用 McpTool 将 MCP 服务器的工具集成到 Feat AI Agent 中。
 * <p>
 * 使用场景：
 * 1. 当你有一个提供各种工具能力的 MCP 服务器时
 * 2. 希望 AI Agent 能够自动发现并调用这些工具
 * 3. 需要统一管理和调用外部工具能力
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class McpToolDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("\n========== 示例3: ReActAgent 集成 MCP 工具 ==========");

        // 创建 MCP 客户端
        McpClient mcpClient = McpClient.streamable(opt -> {
            opt.debug(true).url("http://remote.mcpservers.org/fetch/mcp");
        });
        McpInitializeResponse initialize = mcpClient.initialize();
        List<Tool> toolList = mcpClient.listTools(null).getTools();
        try {

            // 创建 ReActAgent 并添加 MCP 工具
            FeatAgent agent = new ReActAgent(opt -> {
                // 添加 MCP 工具到 Agent
//                opt.tool(mcpTool);
                toolList.forEach(tool -> {
                    opt.tool(new McpTool(mcpClient, tool) {
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
                // 配置模型（示例使用 GiteeAI）
                opt.chatOptions().model(ChatModelVendor.GiteeAI.Kimi_K25_Instruct);
            });
           

            // 使用 Agent 执行任务
            String task = "smartboot组织下的开源项目feat的作者是谁";
            System.out.println("任务: " + task);

            // Agent 会智能决策何时调用 MCP 工具
            String result = agent.execute(task).get();
            System.out.println("执行结果: " + result);

            System.out.println("Agent 创建成功，包含 MCP 工具: " + mcpClient);

        } finally {
//            mcpClient.close();
        }
    }
}
