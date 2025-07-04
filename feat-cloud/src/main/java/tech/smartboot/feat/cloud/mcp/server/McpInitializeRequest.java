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