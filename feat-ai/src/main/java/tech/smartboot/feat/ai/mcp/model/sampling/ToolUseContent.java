/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model.sampling;

import com.alibaba.fastjson2.JSONObject;

/**
 * 采样结果中的一次工具调用请求，只在 assistant 消息里出现。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class ToolUseContent {
    private final String type = "tool_use";
    private String id;
    private String name;
    private JSONObject input;

    public static ToolUseContent of(String id, String name, JSONObject input) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("tool use id is required");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("tool name is required");
        }
        ToolUseContent content = new ToolUseContent();
        content.id = id;
        content.name = name;
        content.input = input == null ? new JSONObject() : input;
        return content;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public JSONObject getInput() {
        return input;
    }
}
