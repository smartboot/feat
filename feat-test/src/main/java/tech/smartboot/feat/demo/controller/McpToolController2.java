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


import tech.smartboot.feat.ai.mcp.model.ToolCalledResult;
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.mcp.Tool;
import tech.smartboot.feat.cloud.annotation.mcp.ToolParam;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@Controller("mcp")
public class McpToolController2 {

    @Tool(description = "测试工具")
    public String tool1(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return "hello " + param1;
    }

    @Tool(description = "测试工具")
    public boolean tool2(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return true;
    }

    @Tool(description = "测试工具")
    public int tool3(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public float tool4(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public short tool5(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public long tool6(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public double tool7(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }


    @Tool(description = "测试工具")
    public byte tool8(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return 0;
    }

    @Tool(description = "测试工具")
    public ToolResult.ImageContent tool9(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return ToolResult.ofImage("data", "mimeType");
    }

    @Tool(description = "测试工具")
    public ToolResult.TextContent tool10(@ToolParam(required = true, description = "参数1") String param1, @ToolParam(required = false, description = "参数2") int param2, boolean param3) {
        return ToolResult.ofText("aaaaa");
    }

}
