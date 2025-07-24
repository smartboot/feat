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
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.cloud.annotation.mcp.Prompt;
import tech.smartboot.feat.cloud.annotation.mcp.Resource;
import tech.smartboot.feat.cloud.annotation.mcp.Tool;
import tech.smartboot.feat.cloud.annotation.mcp.Param;

@Controller
@McpEndpoint(
    name = "demo-mcp-service",
    title = "Demo MCP Service",
    sseEndpoint = "/mcp/demo/sse",
    sseMessageEndpoint = "/mcp/demo/sse/message",
    streamableEndpoint = "/mcp/demo/stream"
)
public class McpDemoController {

    /**
     * 定义一个工具，用于获取用户信息
     */
    @Tool(name = "getUserInfo", description = "根据用户ID获取用户信息")
    public UserInfo getUserInfo(
        @Param(required = true, description = "用户ID") Long userId,
        @Param(required = false, description = "是否包含详细信息") Boolean detailed
    ) {
        // 实现获取用户信息的逻辑
        UserInfo user = new UserInfo();
        user.setId(userId);
        user.setName("User " + userId);
        user.setEmail(userId + "@example.com");

        if (Boolean.TRUE.equals(detailed)) {
            user.setPhone("13800138000");
            user.setAddress("北京市朝阳区");
        }

        return user;
    }

    /**
     * 定义一个工具，用于计算两个数的和
     */
    @Tool(name = "addNumbers", description = "计算两个数的和")
    public double addNumbers(
        @Param(required = true, description = "第一个数") double a,
        @Param(required = true, description = "第二个数") double b
    ) {
        return a + b;
    }

    /**
     * 定义一个文本提示词
     */
    @Prompt(
        name = "codeReviewPrompt",
        description = "代码审查提示词",
        type = PromptType.TEXT
    )
    public String codeReviewPrompt(
        @Param(required = true, description = "编程语言") String language,
        @Param(required = true, description = "代码片段") String codeSnippet
    ) {
        return String.format("请审查以下%s代码并提供改进建议：%s", language, codeSnippet);
    }

    /**
     * 定义一个文本资源
     */
    @Resource(
        uri = "/resources/coding-standards.md",
        name = "编码规范",
        description = "团队编码规范文档",
        mimeType = "text/markdown",
        isText = true
    )
    public String codingStandards() {
        return "# 编码规范";
    }

    /**
     * 定义一个二进制资源
     */
    @Resource(
        uri = "/resources/architecture.png",
        name = "架构图",
        description = "系统架构图",
        mimeType = "image/png",
        isText = false
    )
    public String architectureDiagram() {
        // 返回图片的Base64编码
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
    }

    // 返回文本内容
    @Tool(description = "获取文本信息")
    public ToolResult.TextContent getTextInfo() {
        return ToolResult.ofText("这是文本内容");
    }

    // 返回图片内容
    @Tool(description = "获取图片信息")
    public ToolResult.ImageContent getImageInfo() {
        return ToolResult.ofImage("base64-encoded-image-data", "image/png");
    }
}