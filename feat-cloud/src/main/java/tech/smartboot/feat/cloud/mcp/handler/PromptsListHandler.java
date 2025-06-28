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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.cloud.mcp.Response;
import tech.smartboot.feat.cloud.mcp.response.PromptsListResponse;
import tech.smartboot.feat.core.server.HttpRequest;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class PromptsListHandler implements ServerHandler<PromptsListResponse> {

    @Override
    public Response<PromptsListResponse> apply(HttpRequest request, JSONObject jsonObject) {
        Response<PromptsListResponse> response = new Response<>();
        response.setResult(new PromptsListResponse());
        return response;
    }
}
