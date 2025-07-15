/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.enums;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/4/25
 */
public enum ToolResultType {
    TEXT("text"), IMAGE("image"), AUDIO("audio"), RESOURCE_LINK("resource_link"), EMBEDDED_RESOURCE("resource"), STRUCTURED_CONTENT("structured_content");
    private final String type;

    ToolResultType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
