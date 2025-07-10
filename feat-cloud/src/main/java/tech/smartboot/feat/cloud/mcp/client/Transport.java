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
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 三刀
 * @version v1.0 7/9/25
 */
public abstract class Transport {
    protected final McpOptions options;
    protected boolean initialized;
    protected String sessionId;
    private final AtomicInteger requestId = new AtomicInteger(0);

    public Transport(McpOptions options) {
        this.options = options;
    }

    public CompletableFuture<Response<JSONObject>> asyncRequest(String method, JSONObject param) {
        CompletableFuture<Response<JSONObject>> future = new CompletableFuture<>();
        if (!initialized && !"initialize".equals(method)) {
            future.completeExceptionally(new FeatException("not initialized"));
            return future;
        }
        Request<JSONObject> request = new Request<>();
        request.setMethod(method);
        request.setParams(param);
        request.setId(requestId.incrementAndGet());
        return doRequest(future, request);
    }

    protected abstract CompletableFuture<Response<JSONObject>> doRequest(CompletableFuture<Response<JSONObject>> future, Request<JSONObject> request);

    public abstract CompletableFuture<HttpResponse> sendNotification(Request<JSONObject> request);
}
