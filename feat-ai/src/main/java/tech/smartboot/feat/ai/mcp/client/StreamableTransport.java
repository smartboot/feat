/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.client;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.mcp.McpException;
import tech.smartboot.feat.ai.mcp.model.Request;
import tech.smartboot.feat.ai.mcp.model.Response;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/9/25
 */
final class StreamableTransport extends Transport {
    private static final Logger logger = LoggerFactory.getLogger(StreamableTransport.class);
    private HttpClient httpClient;
    private HttpClient sseClient;


    public StreamableTransport(McpOptions options) {
        super(options);
        httpClient = new HttpClient(options.getUrl());
        sseClient = Feat.httpClient(options.getUrl());
        httpClient.options().debug(true);
    }

    @Override
    void initialized() {
        sseClient.post()
                .header(header -> {
                    options.getHeaders().forEach(header::set);
                    header.set(Request.HEADER_SESSION_ID, sessionId);
                    //The client MUST include an Accept header, listing both application/json and text/event-stream as supported content types.
                    header.set(HeaderName.ACCEPT, "application/json,text/event-stream");
                })
                .onSSE(sse -> sse.onData(event -> {
                    JSONObject jsonObject = JSONObject.parseObject(event.getData());
                    Response<JSONObject> response = handleServerRequest(jsonObject);
                    if (response != null) {
                        doRequest(httpClient.post(), response);
                        return;
                    }
                    options.getNotificationHandler().accept(jsonObject.getString("method"));
                    logger.warn("unexpected event: " + event.getType() + " data: " + event.getData());

                }))
                .onFailure(throwable -> {
                    System.out.println("sse error");
                    throwable.printStackTrace();
                }).submit();
    }

    @Override
    protected CompletableFuture<Response<JSONObject>> doRequest(CompletableFuture<Response<JSONObject>> future, Request<JSONObject> request) {
        if (FeatUtils.isBlank(options.getUrl())) {
            future.completeExceptionally(new FeatException("endpoint not found"));
            return future;
        }
        HttpRest httpRest = httpClient.post();
        doRequest(httpRest, request);
        httpRest.onSuccess(response -> {
            if (response.statusCode() == HttpStatus.ACCEPTED.value()) {
                future.complete(null);
                return;
            }
            if (FeatUtils.isBlank(sessionId)) {
                sessionId = response.getHeader(Request.HEADER_SESSION_ID);
            }
            Response<JSONObject> rsp = JSONObject.parseObject(response.body(), new TypeReference<Response<JSONObject>>() {
            });
            if (rsp.getError() != null) {
                future.completeExceptionally(new McpException(rsp.getError().getInteger("code"), rsp.getError().getString("message")));
            } else {
                future.complete(rsp);
            }
        }).onFailure(future::completeExceptionally);
        return future;
    }

    @Override
    public CompletableFuture<HttpResponse> sendNotification(String method) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        Request<JSONObject> request = new Request<>();
        request.setMethod(method);
        byte[] body = JSONObject.toJSONString(request).getBytes();
        httpClient.post().header(header -> {
            options.getHeaders().forEach(header::set);
            header.setContentType(HeaderValue.ContentType.APPLICATION_JSON).setContentLength(body.length);
            if (FeatUtils.isNotBlank(sessionId)) {
                header.set(Request.HEADER_SESSION_ID, sessionId);
            }
        }).body(b -> b.write(body)).onSuccess(future::complete).onFailure(future::completeExceptionally).submit();
        return future;
    }

    @Override
    public void close() {
        httpClient.close();
        sseClient.close();
    }
}
