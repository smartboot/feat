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

public class ClientCapabilities {
    private boolean roots;
    private boolean sampling;
    private boolean elicitation;
    private JSONObject experimental;

    public ClientCapabilities rootsEnable() {
        this.roots = true;
        return this;
    }

    public ClientCapabilities samplingEnable() {
        this.sampling = true;
        return this;
    }

    public ClientCapabilities elicitationEnable() {
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