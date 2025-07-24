/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.controller;


import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.cloud.annotation.mcp.Param;
import tech.smartboot.feat.cloud.annotation.mcp.Tool;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@Controller
@McpEndpoint(sseEndpoint = "/mcp/sse"
        , sseMessageEndpoint = "/mcp/sse/message"
        , streamableEndpoint = "/mcp/message")
public class McpToolController2 {
    @Autowired
    private McpServer mcpServer;

    @RequestMapping("/afa/asdf/adsf/afd")
    public String hello() {
        return "hello ";
    }

    @Tool(description = "测试工具")
    public String tool1(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return "hello " + param1;
    }

    @Tool(description = "测试工具")
    public boolean tool2(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return true;
    }

    @Tool(description = "测试工具")
    public int tool3(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public float tool4(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public short tool5(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public long tool6(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public double tool7(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }


    @Tool(description = "测试工具")
    public byte tool8(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public ToolResult.ImageContent tool9(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return ToolResult.ofImage("data", "mimeType");
    }

    @Tool(description = "测试工具")
    public ToolResult.TextContent tool10(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2, boolean param3) {
        return ToolResult.ofText("aaaaa");
    }

    public void setMcpServer(McpServer mcpServer) {
        this.mcpServer = mcpServer;
    }
}
