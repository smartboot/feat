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
import tech.smartboot.feat.mcp.model.Resource;
import tech.smartboot.feat.mcp.server.McpServer;
import tech.smartboot.feat.mcp.McpException;
import tech.smartboot.feat.mcp.server.model.ResourceContext;
import tech.smartboot.feat.mcp.server.model.ServerResource;
import tech.smartboot.feat.core.server.HttpRequest;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 6/28/25
 */
public class ResourcesReadHandler implements ServerHandler {

    @Override
    public JSONObject apply(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        JSONObject params = jsonObject.getJSONObject("params");
        String uri = params.getString("uri");
        ServerResource serverResource = mcp.getResources().stream().filter(r -> r.getUri().equals(uri)).findFirst().orElse(null);

        if (serverResource == null) {
            throw new McpException(McpException.RESOURCE_NOT_FOUND, "Resource not found ", JSONObject.of("uri", uri));
        }
        ResourceContext promptContext = new ResourceContext(request, serverResource);
        String data = serverResource.getAction().apply(promptContext);
        Resource resource = Resource.copy(serverResource);
        if (serverResource.isText()) {
            resource.setText(data);
        } else {
            resource.setBlob(data);
        }
        JSONObject result = new JSONObject();
        result.put("contents", JSONArray.of(JSONObject.from(resource)));
        return result;
    }
}
