package tech.smartboot.feat.cloud.mcp;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class McpInitializeRequest {
    private String protocolVersion;
    private ClientCapabilities capabilities;
    private Implementation clientInfo;

    // Getters and Setters

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public ClientCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ClientCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public Implementation getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Implementation clientInfo) {
        this.clientInfo = clientInfo;
    }
}

class ClientCapabilities {
    private Capability roots;
    private Capability sampling;
    private Capability elicitation;
    private Capability experimental;

    public Capability getRoots() {
        return roots;
    }

    public void setRoots(Capability roots) {
        this.roots = roots;
    }

    public Capability getSampling() {
        return sampling;
    }

    public void setSampling(Capability sampling) {
        this.sampling = sampling;
    }

    public Capability getElicitation() {
        return elicitation;
    }

    public void setElicitation(Capability elicitation) {
        this.elicitation = elicitation;
    }

    public Capability getExperimental() {
        return experimental;
    }

    public void setExperimental(Capability experimental) {
        this.experimental = experimental;
    }
}