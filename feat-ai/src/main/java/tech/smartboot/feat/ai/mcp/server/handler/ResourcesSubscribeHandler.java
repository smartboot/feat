/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.server.handler;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.McpException;
import tech.smartboot.feat.ai.mcp.model.Request;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.ai.mcp.server.StreamSession;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.HttpRequest;

/**
 * 处理 resources/subscribe。
 * <p>
 * initialize 阶段声明了 resources.subscribe 能力，就必须应答该方法，否则客户端会拿到空响应。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class ResourcesSubscribeHandler implements ServerHandler {

    @Override
    public JSONObject handle(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        JSONObject params = jsonObject.getJSONObject("params");
        String uri = params == null ? null : params.getString("uri");
        if (FeatUtils.isBlank(uri)) {
            throw new McpException(McpException.INVALID_PARAMS, "uri is required");
        }
        if (mcp.getResources().stream().noneMatch(r -> uri.equals(r.getUri()))) {
            throw new McpException(McpException.INVALID_PARAMS, "Resource not found", JSONObject.of("uri", uri));
        }
        StreamSession session = mcp.getSseEmitters().get(request.getHeader(Request.HEADER_SESSION_ID));
        if (session == null) {
            throw new McpException(McpException.INVALID_REQUEST, "session not found");
        }
        session.subscribeResource(uri);
        return new JSONObject();
    }
}
