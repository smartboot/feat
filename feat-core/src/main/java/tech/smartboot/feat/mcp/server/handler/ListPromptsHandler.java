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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.mcp.model.PromptListResponse;
import tech.smartboot.feat.mcp.server.McpServer;
import tech.smartboot.feat.core.server.HttpRequest;

import java.util.ArrayList;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class ListPromptsHandler implements ServerHandler {

    @Override
    public JSONObject apply(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        PromptListResponse listResponse = new PromptListResponse();
        listResponse.setPrompts(new ArrayList<>(mcp.getPrompts()));
        return JSONObject.from(listResponse);
    }
}
