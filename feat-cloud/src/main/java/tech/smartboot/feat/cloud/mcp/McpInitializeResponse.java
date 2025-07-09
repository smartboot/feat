/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.mcp.server.Implementation;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class McpInitializeResponse {
    private String protocolVersion;
    private ServerCapabilities capabilities = new ServerCapabilities();
    private Implementation serverInfo;

    // Getters and Setters

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public ServerCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ServerCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public Implementation getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(Implementation serverInfo) {
        this.serverInfo = serverInfo;
    }


    public static Builder builder() {
        Builder builder = new Builder();
        builder.mcpInitializeResponse.serverInfo = Implementation.of("feat-mcp-server", "feat-mcp-server", Feat.VERSION);
        builder.mcpInitializeResponse.protocolVersion = "2025-03-26";
        return builder;
    }

    public static class Builder {
        private final McpInitializeResponse mcpInitializeResponse = new McpInitializeResponse();

        public Builder loggingEnable() {
            mcpInitializeResponse.getCapabilities().setLogging(new JSONObject());
            return this;
        }

        public Builder promptsEnable() {
            JSONObject capability = new JSONObject();
            capability.put("listChanged", true);
            mcpInitializeResponse.getCapabilities().setPrompts(capability);
            return this;
        }

        public Builder resourceEnable() {
            JSONObject capability = new JSONObject();
            capability.put("listChanged", true);
            capability.put("subscribe", true);
            mcpInitializeResponse.getCapabilities().setResources(capability);
            return this;
        }

        public Builder toolEnable() {
            JSONObject capability = new JSONObject();
            capability.put("listChanged", true);
            mcpInitializeResponse.getCapabilities().setTools(capability);
            return this;
        }

        public McpInitializeResponse build() {
            return mcpInitializeResponse;
        }
    }
}

class ServerCapabilities {
    private JSONObject prompts;
    private JSONObject resources;
    private JSONObject tools;
    private JSONObject logging;
    private JSONObject completions;
    private JSONObject experimental;

    public JSONObject getPrompts() {
        return prompts;
    }

    public void setPrompts(JSONObject prompts) {
        this.prompts = prompts;
    }

    public JSONObject getResources() {
        return resources;
    }

    public void setResources(JSONObject resources) {
        this.resources = resources;
    }

    public JSONObject getTools() {
        return tools;
    }

    public void setTools(JSONObject tools) {
        this.tools = tools;
    }

    public JSONObject getLogging() {
        return logging;
    }

    public void setLogging(JSONObject logging) {
        this.logging = logging;
    }

    public JSONObject getCompletions() {
        return completions;
    }

    public void setCompletions(JSONObject completions) {
        this.completions = completions;
    }

    public JSONObject getExperimental() {
        return experimental;
    }

    public void setExperimental(JSONObject experimental) {
        this.experimental = experimental;
    }
}


