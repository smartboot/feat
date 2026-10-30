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

import java.util.ArrayList;
import java.util.List;

/**
 * 工具执行结果，作为 user 消息回传给模型。
 * <p>
 * 规范要求：含 tool_result 的 user 消息只能包含 tool_result，不能与其他内容类型混排；
 * 且每个 tool_use 都必须在紧随其后的 user 消息中被对应解析。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class ToolResultContent {
    private final String type = "tool_result";
    private String toolUseId;
    private List<Object> content;

    public static ToolResultContent of(String toolUseId, List<Object> content) {
        if (toolUseId == null || toolUseId.isEmpty()) {
            throw new IllegalArgumentException("toolUseId is required");
        }
        ToolResultContent result = new ToolResultContent();
        result.toolUseId = toolUseId;
        result.content = content == null ? new ArrayList<Object>() : content;
        return result;
    }

    public static ToolResultContent ofText(String toolUseId, String text) {
        List<Object> blocks = new ArrayList<>();
        blocks.add(JSONObject.of("type", "text", "text", text));
        return of(toolUseId, blocks);
    }

    public String getType() {
        return type;
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public List<Object> getContent() {
        return content;
    }
}
