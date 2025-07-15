/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.mcp.server.handler;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.mcp.model.ToolCalledResult;
import tech.smartboot.feat.mcp.enums.ToolResultType;
import tech.smartboot.feat.mcp.server.McpServer;
import tech.smartboot.feat.mcp.McpException;
import tech.smartboot.feat.mcp.server.model.ServerTool;
import tech.smartboot.feat.mcp.server.model.ToolContext;
import tech.smartboot.feat.core.server.HttpRequest;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class ToolsCallHandler implements ServerHandler {

    @Override
    public JSONObject apply(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        JSONObject params = jsonObject.getJSONObject("params");
        String toolName = params.getString("name");
        JSONObject toolParams = params.getJSONObject("arguments");

        ToolContext toolContext = new ToolContext(request, toolParams);

        ServerTool tool = mcp.getTools().stream().filter(t -> t.getName().equals(toolName)).findFirst().orElse(null);
        if (tool == null) {
            throw new McpException(McpException.INTERNAL_ERROR, "Unknown tool: " + toolName);
        }
        JSONObject result = new JSONObject();
        try {
            ToolCalledResult.Content content = tool.getAction().apply(toolContext);
            if (ToolResultType.STRUCTURED_CONTENT.getType().equals(content.getType())) {
                ToolCalledResult.StructuredContent structuredContent = (ToolCalledResult.StructuredContent) content;
                result.put("content", JSONArray.of(JSONObject.from(ToolCalledResult.ofText(structuredContent.getContent().toString()))));
                result.put("structuredContent", structuredContent.getContent());
            } else {
                result.put("content", JSONArray.of(JSONObject.from(content)));
            }
        } catch (Throwable e) {
            result.put("content", JSONArray.of(JSONObject.from(ToolCalledResult.ofText(e.getMessage()))));
            result.put("isError", true);
        }
        return result;
    }
}
