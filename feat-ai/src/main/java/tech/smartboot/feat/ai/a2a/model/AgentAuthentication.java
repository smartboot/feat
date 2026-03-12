/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.model;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.a2a.enums.AuthenticationType;

/**
 * A2A 智能体认证配置类
 *
 * <p>描述智能体的认证方式和配置。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class AgentAuthentication {
    /**
     * 认证类型
     */
    private AuthenticationType type;

    /**
     * 认证端点（用于OAuth2等）
     */
    private String endpoint;

    /**
     * 认证作用域
     */
    private String[] scopes;

    /**
     * 额外的认证配置
     */
    private JSONObject credentials;

    public AuthenticationType getType() {
        return type;
    }

    public void setType(AuthenticationType type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String[] getScopes() {
        return scopes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public JSONObject getCredentials() {
        return credentials;
    }

    public void setCredentials(JSONObject credentials) {
        this.credentials = credentials;
    }
}
