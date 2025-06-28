package tech.smartboot.feat.cloud.mcp;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class McpInitializeResponse extends Response {
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
        builder.mcpInitializeResponse.serverInfo = new Implementation();
        builder.mcpInitializeResponse.protocolVersion = "2025-03-26";
        return builder;
    }

    public static class Builder {
        private final McpInitializeResponse mcpInitializeResponse = new McpInitializeResponse();

        public Builder loggingEnable() {
            mcpInitializeResponse.getCapabilities().setLogging(new Capability());
            return this;
        }

        public Builder promptsEnable() {
            Capability capability = new Capability();
            capability.setListChanged(true);
            mcpInitializeResponse.getCapabilities().setPrompts(capability);
            return this;
        }

        public Builder resourceEnable() {
            Capability capability = new Capability();
            capability.setListChanged(true);
            mcpInitializeResponse.getCapabilities().setResources(capability);
            return this;
        }

        public Builder toolEnable() {
            Capability capability = new Capability();
            capability.setListChanged(true);
            mcpInitializeResponse.getCapabilities().setTools(capability);
            return this;
        }

        public McpInitializeResponse build() {
            return mcpInitializeResponse;
        }
    }
}

class ServerCapabilities {
    private Capability prompts;
    private Capability resources;
    private Capability tools;
    private Capability logging;
    private Capability completions;
    private Capability experimental;

    public Capability getPrompts() {
        return prompts;
    }

    public void setPrompts(Capability prompts) {
        this.prompts = prompts;
    }

    public Capability getResources() {
        return resources;
    }

    public void setResources(Capability resources) {
        this.resources = resources;
    }

    public Capability getTools() {
        return tools;
    }

    public void setTools(Capability tools) {
        this.tools = tools;
    }

    public Capability getLogging() {
        return logging;
    }

    public void setLogging(Capability logging) {
        this.logging = logging;
    }

    public Capability getCompletions() {
        return completions;
    }

    public void setCompletions(Capability completions) {
        this.completions = completions;
    }

    public Capability getExperimental() {
        return experimental;
    }

    public void setExperimental(Capability experimental) {
        this.experimental = experimental;
    }
}


