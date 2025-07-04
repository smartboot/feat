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
import tech.smartboot.feat.cloud.mcp.server.model.Resource;
import tech.smartboot.feat.core.server.HttpRequest;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class ResourcesListHandler implements ServerHandler {

    @Override
    public JSONObject apply(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        JSONObject result = new JSONObject();

        JSONArray resources = new JSONArray();
        for (Resource resource : mcp.getResources()) {
            JSONObject json = new JSONObject();
            json.put("name", resource.getName());
            json.put("title", resource.getTitle());
            json.put("description", resource.getDescription());
            json.put("uri", resource.getUri());
            json.put("mimeType", resource.getMimeType());
            resources.add(json);
        }
        result.put("resources", resources);
        return result;
    }
}
