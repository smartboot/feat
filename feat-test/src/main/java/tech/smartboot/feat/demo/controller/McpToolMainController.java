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


import tech.smartboot.feat.ai.mcp.enums.PromptType;
import tech.smartboot.feat.ai.mcp.enums.RoleEnum;
import tech.smartboot.feat.ai.mcp.model.PromptMessage;
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.cloud.annotation.mcp.Param;
import tech.smartboot.feat.cloud.annotation.mcp.Prompt;
import tech.smartboot.feat.cloud.annotation.mcp.Resource;
import tech.smartboot.feat.cloud.annotation.mcp.Tool;

import java.util.Base64;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@McpEndpoint(mcpSseEndpoint = "/main/sse"
        , mcpSseMessageEndpoint = "/main/sse/message"
        , mcpStreamableEndpoint = "/main/mcp")
public class McpToolMainController {

    @Autowired
    private McpServer mcpServer;

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

    @Prompt(name = "prompt1", description = "测试提示", type = PromptType.TEXT)
    public String prompt1(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2) {
        return "prompt1";
    }

    @Prompt(name = "prompt2", description = "测试提示", type = PromptType.TEXT)
    public int prompt2(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2) {
        return 1;
    }

    @Prompt(name = "prompt3", description = "测试提示", type = PromptType.IMAGE, mineType = "bbb")
    public String prompt3(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2) {
        return Base64.getEncoder().encodeToString("aasdfadsfa".getBytes());
    }

    @Prompt(name = "prompt4", description = "测试提示", type = PromptType.AUDIO, mineType = "aaa")
    public String prompt4(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2) {
        return Base64.getEncoder().encodeToString("aasdfadsfa".getBytes());
    }

    @Prompt(name = "prompt5", description = "测试提示", type = PromptType.EMBEDDED_RESOURCE)
    public PromptMessage prompt5(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2) {
        return PromptMessage.ofEmbeddedResource(RoleEnum.User, tech.smartboot.feat.ai.mcp.model.Resource.of("file:///aa.txt", "test.txt", "text/plain"));
    }

    @Prompt(name = "prompt6", description = "测试提示", type = PromptType.EMBEDDED_RESOURCE)
    public PromptMessage<PromptMessage.EmbeddedResourcePromptContent> prompt6(@Param(required = true, description = "参数1") String param1, @Param(required = false, description = "参数2") int param2) {
        return PromptMessage.ofEmbeddedResource(RoleEnum.User, tech.smartboot.feat.ai.mcp.model.Resource.of("file:///aa.txt", "test.txt", "text/plain"));
    }

    @Resource(uri = "/resource/text", name = "text", description = "text", mimeType = "text/x-rust", isText = true)
    public String resourceText() {
        return "text";
    }

    @Resource(uri = "/resource/text1", name = "text", description = "text", isText = false)
    public String resourceBin() {
        return "text";
    }

//    @Resource(uri = "/resource/text1", name = "text", description = "text", isText = false)
//    public ServerResource resourceBin1() {
//        return ServerResource.ofBinary("/resource/text1", "text");
//    }
}
