/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.client;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.mcp.model.Implementation;

/**
 * @author 三刀
 * @version v1.0 7/8/25
 */
public class McpOptions {
    private String baseUrl;
    private String mcpEndpoint = "/mcp";
    private String sseEndpoint = "/sse";
    private final Implementation implementation = Implementation.of("feat-mcp-client", "Feat MCP", Feat.VERSION);
    private boolean roots;
    private boolean sampling;
    private boolean elicitation;
    private JSONObject experimental;

    McpOptions() {
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public McpOptions baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String getMcpEndpoint() {
        return mcpEndpoint;
    }

    public McpOptions setMcpEndpoint(String mcpEndpoint) {
        this.mcpEndpoint = mcpEndpoint;
        return this;
    }

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    public void setSseEndpoint(String sseEndpoint) {
        this.sseEndpoint = sseEndpoint;
    }


    public Implementation getImplementation() {
        return implementation;
    }

    public void implementation(String name, String title, String version) {
        this.implementation.setName(name);
        this.implementation.setTitle(title);
        this.implementation.setVersion(version);
    }

    public McpOptions rootsEnable() {
        this.roots = true;
        return this;
    }

    public McpOptions samplingEnable() {
        this.sampling = true;
        return this;
    }

    public McpOptions elicitationEnable() {
        this.elicitation = true;
        return this;
    }

    public boolean isRoots() {
        return roots;
    }

    public boolean isSampling() {
        return sampling;
    }

    public boolean isElicitation() {
        return elicitation;
    }

    public JSONObject getExperimental() {
        return experimental;
    }
}
