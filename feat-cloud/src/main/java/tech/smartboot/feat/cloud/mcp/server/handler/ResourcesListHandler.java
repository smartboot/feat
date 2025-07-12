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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.cloud.mcp.model.Resource;
import tech.smartboot.feat.cloud.mcp.server.McpServer;
import tech.smartboot.feat.cloud.mcp.server.model.ServerResource;
import tech.smartboot.feat.core.server.HttpRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class ResourcesListHandler implements ServerHandler {

    @Override
    public JSONObject apply(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        JSONObject result = new JSONObject();
        List<Resource> resources = new ArrayList<>();
        for (ServerResource resource : mcp.getResources()) {
            resources.add(resource.getResource());
        }
        result.put("resources", resources);
        return result;
    }
}
