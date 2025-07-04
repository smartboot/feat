/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.server.handler;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.cloud.mcp.server.McpServer;
import tech.smartboot.feat.cloud.mcp.server.McpServerException;
import tech.smartboot.feat.cloud.mcp.server.model.Prompt;
import tech.smartboot.feat.cloud.mcp.server.model.PromptContext;
import tech.smartboot.feat.cloud.mcp.server.model.PromptResult;
import tech.smartboot.feat.core.server.HttpRequest;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class PromptsGetHandler implements ServerHandler {
    @Override
    public JSONObject apply(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        JSONObject params = jsonObject.getJSONObject("params");
        String promptName = params.getString("name");
        JSONObject promptParams = params.getJSONObject("arguments");

        PromptContext promptContext = new PromptContext(request, promptParams);

        Prompt prompt = mcp.getPrompts().stream().filter(t -> t.getName().equals(promptName)).findFirst().orElse(null);
        if (prompt == null) {
            throw new McpServerException(McpServerException.INTERNAL_ERROR, "Unknown prompt: " + promptName);
        }
        JSONObject result = new JSONObject();
        PromptResult content = prompt.getAction().apply(promptContext);
        result.put("messages", JSONArray.of(JSONObject.from(content)));
        result.put("description", prompt.getDescription());
        return result;
    }
}
