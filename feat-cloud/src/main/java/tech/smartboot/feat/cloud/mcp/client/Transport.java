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
import tech.smartboot.feat.cloud.mcp.model.JsonRpc;
import tech.smartboot.feat.cloud.mcp.model.Request;
import tech.smartboot.feat.cloud.mcp.model.Response;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderValue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀
 * @version v1.0 7/9/25
 */
abstract class Transport {
    protected final McpOptions options;
    protected String sessionId;
    private final AtomicInteger requestId = new AtomicInteger(0);

    public Transport(McpOptions options) {
        this.options = options;
    }

    public CompletableFuture<Response<JSONObject>> asyncRequest(String method, JSONObject param) {
        CompletableFuture<Response<JSONObject>> future = new CompletableFuture<>();
        Request<JSONObject> request = new Request<>();
        request.setMethod(method);
        request.setParams(param);
        request.setId(requestId.incrementAndGet());
        return doRequest(future, request);
    }

    abstract void initialized();

    protected abstract CompletableFuture<Response<JSONObject>> doRequest(CompletableFuture<Response<JSONObject>> future, Request<JSONObject> request);

    protected final CompletableFuture<HttpResponse> doRequest(HttpRest httpRest, JsonRpc request) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        byte[] body = JSONObject.toJSONString(request).getBytes();
        httpRest.header(header -> {
            header.setContentType(HeaderValue.ContentType.APPLICATION_JSON).setContentLength(body.length);
            if (FeatUtils.isNotBlank(sessionId)) {
                header.set(Request.HEADER_SESSION_ID, sessionId);
            }
        }).body(b -> b.write(body)).onSuccess(future::complete).onFailure(future::completeExceptionally).submit();
        return future;
    }

    public abstract CompletableFuture<HttpResponse> sendNotification(String method);

    protected final Response<JSONObject> handleServerRequest(JSONObject request) {
        String method = request.getString("method");
        if ("roots/list".equals(method)) {
            Response<JSONObject> response = new Response<>();
            response.setId(request.getInteger("id"));
            JSONObject json = new JSONObject();
            json.put("roots", options.getRootsList());
            response.setResult(json);
            return response;
        }
        return null;
    }
}
