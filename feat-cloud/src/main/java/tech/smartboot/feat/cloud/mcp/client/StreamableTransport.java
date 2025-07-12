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
import com.alibaba.fastjson2.TypeReference;
import tech.smartboot.feat.cloud.mcp.model.Request;
import tech.smartboot.feat.cloud.mcp.model.Response;
import tech.smartboot.feat.cloud.mcp.server.McpServerException;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀
 * @version v1.0 7/9/25
 */
public class StreamableTransport extends Transport {
    private HttpClient httpClient;
    private HttpClient sseClient;

    public StreamableTransport(McpOptions options) {
        super(options);
        httpClient = new HttpClient(options.getBaseUrl());
        sseClient = new HttpClient(options.getBaseUrl());
    }

    @Override
    protected CompletableFuture<Response<JSONObject>> doRequest(CompletableFuture<Response<JSONObject>> future, Request<JSONObject> request) {
        if (FeatUtils.isBlank(options.getMcpEndpoint())) {
            future.completeExceptionally(new FeatException("endpoint not found"));
            return future;
        }
        byte[] body = JSONObject.toJSONString(request).getBytes();
        httpClient.post(options.getMcpEndpoint()).header(header -> {
                    header.setContentType(HeaderValue.ContentType.APPLICATION_JSON).setContentLength(body.length);
                    if (FeatUtils.isNotBlank(sessionId)) {
                        header.set("Mcp-Session-Id", sessionId);
                    }
                }).body(b -> b.write(body)).onSuccess(response -> {
                    if (response.statusCode() == HttpStatus.ACCEPTED.value()) {
                        future.complete(null);
                        return;
                    }
                    if (FeatUtils.isBlank(sessionId)) {
                        sessionId = response.getHeader("Mcp-Session-Id");
                    }
                    Response<JSONObject> rsp = JSONObject.parseObject(response.body(), new TypeReference<Response<JSONObject>>() {
                    });
                    if (rsp.getError() != null) {
                        future.completeExceptionally(new McpServerException(rsp.getError().getInteger("code"), rsp.getError().getString("message")));
                    } else {
                        future.complete(rsp);
                    }
                })
                .onFailure(throwable -> future.completeExceptionally(throwable)).submit();
        return future;
    }

    @Override
    public CompletableFuture<HttpResponse> sendNotification(Request<JSONObject> request) {
        byte[] body = JSONObject.toJSONString(request).getBytes();
        return httpClient.post(options.getMcpEndpoint()).header(header -> {
            header.setContentType(HeaderValue.ContentType.APPLICATION_JSON).setContentLength(body.length);
            if (FeatUtils.isNotBlank(sessionId)) {
                header.set("Mcp-Session-Id", sessionId);
            }
        }).body(b -> b.write(body)).submit();
    }
}
