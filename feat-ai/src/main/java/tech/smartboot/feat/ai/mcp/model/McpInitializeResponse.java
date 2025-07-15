/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 6/18/25
 */
public class McpInitializeResponse {
    private String protocolVersion = McpInitializeRequest.PROTOCOL_VERSION;
    private final ServerCapabilities capabilities = new ServerCapabilities();
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


    public Implementation getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(Implementation serverInfo) {
        this.serverInfo = serverInfo;
    }
}




