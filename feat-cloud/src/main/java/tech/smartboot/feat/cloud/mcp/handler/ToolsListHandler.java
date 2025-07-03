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
import tech.smartboot.feat.cloud.mcp.model.Property;
import tech.smartboot.feat.cloud.mcp.model.PropertyType;
import tech.smartboot.feat.cloud.mcp.model.Tool;
import tech.smartboot.feat.core.server.HttpRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class ToolsListHandler implements ServerHandler {

    @Override
    public JSONObject apply(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        JSONObject result = new JSONObject();

        JSONArray tools = new JSONArray();
        for (Tool tool : mcp.getTools()) {
            JSONObject toolObject = new JSONObject();
            toolObject.put("name", tool.getName());
            toolObject.put("title", tool.getTitle());
            toolObject.put("description", tool.getDescription());

            List<String> requiredInputs = new ArrayList<>();

            JSONObject inputSchema = new JSONObject();
            inputSchema.put("type", PropertyType.Object.getType());
            JSONObject properties = new JSONObject();
            for (Property property : tool.getInputs()) {
                JSONObject propertyObject = new JSONObject();
                propertyObject.put("type", property.getType().getType());
                propertyObject.put("description", property.getDescription());
                properties.put(property.getName(), propertyObject);

                if (property.isRequired()) {
                    requiredInputs.add(property.getName());
                }
            }
            inputSchema.put("properties", properties);
            inputSchema.put("required", requiredInputs);
            toolObject.put("inputSchema", inputSchema);
            tools.add(toolObject);
        }
        result.put("tools", tools);
        return result;
    }
}
