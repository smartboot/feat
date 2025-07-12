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
import tech.smartboot.feat.cloud.mcp.model.ToolListResponse;
import tech.smartboot.feat.cloud.mcp.server.McpServer;
import tech.smartboot.feat.core.server.HttpRequest;

import java.util.ArrayList;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class ToolsListHandler implements ServerHandler {

    @Override
    public JSONObject apply(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        ToolListResponse response = new ToolListResponse();
        response.setTools(new ArrayList<>(mcp.getTools()));
        return JSONObject.from(response);
    }
}
