/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.handler;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.cloud.mcp.McpServer;
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

        JSONObject result = new JSONObject();

        JSONArray array = new JSONArray();
        mcp.getTools().stream().filter(tool -> tool.getName().equals(toolName)).findFirst().ifPresent(tool -> {
            JSONObject context = new JSONObject();
            context.put("type", "text");
            context.put("text", tool.getAction().apply(toolParams).toString());
            array.add(context);
        });
        result.put("context", array);
        result.put("isError", false);
        return result;
    }
}
