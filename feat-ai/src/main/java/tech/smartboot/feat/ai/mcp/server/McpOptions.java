/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.server;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.mcp.model.Implementation;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/5/25
 */
public class McpOptions {
    private String mcpEndpoint = "/mcp";
    private String sseEndpoint = "/sse";
    private String sseMessageEndpoint = "/message";
    private final Implementation implementation = Implementation.of("feat-mcp-server", "Feat MCP Server", Feat.VERSION);
    private boolean loggingEnable;
    private boolean promptsEnable;
    private boolean resourceEnable;
    private boolean toolEnable;

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    public McpOptions setSseEndpoint(String sseEndpoint) {
        this.sseEndpoint = sseEndpoint;
        return this;
    }

    public String getSseMessageEndpoint() {
        return sseMessageEndpoint;
    }

    public McpOptions setSseMessageEndpoint(String sseMessageEndpoint) {
        this.sseMessageEndpoint = sseMessageEndpoint;
        return this;
    }

    public String getMcpEndpoint() {
        return mcpEndpoint;
    }

    public McpOptions setMcpEndpoint(String mcpEndpoint) {
        this.mcpEndpoint = mcpEndpoint;
        return this;
    }

    public Implementation getImplementation() {
        return implementation;
    }

    public void implementation(String name, String title, String version) {
        this.implementation.setName(name);
        this.implementation.setTitle(title);
        this.implementation.setVersion(version);
    }

    public McpOptions loggingEnable() {
        this.loggingEnable = true;
        return this;
    }

    public McpOptions promptsEnable() {
        this.promptsEnable = true;
        return this;
    }

    public McpOptions resourceEnable() {
        this.resourceEnable = true;
        return this;
    }

    public McpOptions toolEnable() {
        this.toolEnable = true;
        return this;
    }

    public boolean isLoggingEnable() {
        return loggingEnable;
    }

    public boolean isPromptsEnable() {
        return promptsEnable;
    }

    public boolean isResourceEnable() {
        return resourceEnable;
    }

    public boolean isToolEnable() {
        return toolEnable;
    }
}
