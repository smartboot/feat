/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.client.model;

import tech.smartboot.feat.cloud.mcp.server.model.ToolResultContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 7/11/25
 */
public class CallToolResult {
    private List<ToolResultContext> content = new ArrayList<>();
    private boolean isError;

    public List<ToolResultContext> getContent() {
        return content;
    }

    public void addContent(ToolResultContext content) {
        this.content.add(content);
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }
}
