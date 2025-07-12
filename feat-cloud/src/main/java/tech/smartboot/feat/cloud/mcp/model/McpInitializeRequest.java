/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.model;

import com.alibaba.fastjson2.JSONObject;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class McpInitializeRequest {
    public static final String PROTOCOL_VERSION = "2025-06-18";
    private String protocolVersion;
    private JSONObject capabilities;
    private Implementation clientInfo;

    // Getters and Setters

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public JSONObject getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(JSONObject capabilities) {
        this.capabilities = capabilities;
    }

    public Implementation getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Implementation clientInfo) {
        this.clientInfo = clientInfo;
    }
}

