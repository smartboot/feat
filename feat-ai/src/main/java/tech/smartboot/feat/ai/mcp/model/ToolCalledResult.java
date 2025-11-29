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

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/11/25
 */
public class ToolCalledResult {
    private final List<ToolResult> content = new ArrayList<>();
    private boolean isError;

    public List<ToolResult> getContent() {
        return content;
    }

    public void addContent(ToolResult content) {
        this.content.add(content);
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }


}
