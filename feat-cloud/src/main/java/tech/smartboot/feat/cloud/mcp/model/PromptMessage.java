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

import tech.smartboot.feat.cloud.mcp.server.model.PromptResult;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class PromptMessage {
    private String role;
    private PromptResult.PromptContent content;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public PromptResult.PromptContent getContent() {
        return content;
    }

    public void setContent(PromptResult.PromptContent content) {
        this.content = content;
    }
}

