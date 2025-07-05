/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.server;

/**
 * @author 三刀
 * @version v1.0 7/5/25
 */
public class McpOptions {
    private String mcpEndpoint = "/mcp";
    private String sseEndpoint = "/sse";
    private String sseMessageEndpoint = "/message";

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

    public void setMcpEndpoint(String mcpEndpoint) {
        this.mcpEndpoint = mcpEndpoint;
    }
}
