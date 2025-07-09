/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.client;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.cloud.mcp.Request;
import tech.smartboot.feat.cloud.mcp.Response;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderValue;

/**
 * @author 三刀
 * @version v1.0 7/9/25
 */
public class SseTransport extends Transport {
    private HttpClient httpClient;
    private HttpClient sseClient;

    public SseTransport(McpOptions options) {
        super(options);
        httpClient = new HttpClient(options.getBaseUrl());
        sseClient = new HttpClient(options.getBaseUrl());
    }

    @Override
    Response sendRequest(Request request) {
        byte[] body = JSONObject.toJSONString(request).getBytes();
        HttpRest rest = httpClient.post(options.getSseEndpoint()).header(header -> {
            header.setContentType(HeaderValue.ContentType.APPLICATION_JSON).setContentLength(body.length);
            if (FeatUtils.isNotBlank(sessionId)) {
                header.set("Mcp-Session-Id", sessionId);
            }
        }).body(b -> b.write(body));

        return null;
    }
}
