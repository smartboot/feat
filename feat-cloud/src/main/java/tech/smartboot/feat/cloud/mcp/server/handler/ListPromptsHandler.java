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
import tech.smartboot.feat.cloud.mcp.model.Prompt;
import tech.smartboot.feat.core.server.HttpRequest;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class ListPromptsHandler implements ServerHandler {

    @Override
    public JSONObject apply(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        JSONObject result = new JSONObject();

        JSONArray prompts = new JSONArray();
        for (Prompt prompt : mcp.getPrompts()) {
            JSONObject toolObject = new JSONObject();
            toolObject.put("name", prompt.getName());
            toolObject.put("title", prompt.getTitle());
            toolObject.put("description", prompt.getDescription());


            JSONArray arguments = new JSONArray();
            for (Prompt.Argument argument : prompt.getArguments()) {
                JSONObject propertyObject = new JSONObject();
                propertyObject.put("name", argument.getName());
                propertyObject.put("description", argument.getDescription());
                propertyObject.put("required", argument.isRequired());
                arguments.add(propertyObject);
            }
            toolObject.put("arguments", arguments);
            prompts.add(toolObject);
        }
        result.put("prompts", prompts);
        return result;
    }
}
