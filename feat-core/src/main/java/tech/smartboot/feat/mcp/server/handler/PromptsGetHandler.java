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
import tech.smartboot.feat.mcp.model.PromptMessage;
import tech.smartboot.feat.mcp.server.McpServer;
import tech.smartboot.feat.mcp.McpException;
import tech.smartboot.feat.mcp.server.model.PromptContext;
import tech.smartboot.feat.mcp.server.model.ServerPrompt;
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
        if (promptParams == null) {
            promptParams = new JSONObject();
        }

        PromptContext promptContext = new PromptContext(request, promptParams);

        ServerPrompt prompt = mcp.getPrompts().stream().filter(t -> t.getName().equals(promptName)).findFirst().orElse(null);
        if (prompt == null) {
            throw new McpException(McpException.INTERNAL_ERROR, "Unknown prompt: " + promptName);
        }
        JSONObject result = new JSONObject();
        PromptMessage<?> content = prompt.getAction().apply(promptContext);
        result.put("messages", JSONArray.of(JSONObject.from(content)));
        result.put("description", prompt.getDescription());
        return result;
    }
}
