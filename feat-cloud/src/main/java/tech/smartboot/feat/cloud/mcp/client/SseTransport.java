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
import tech.smartboot.feat.cloud.mcp.McpException;
import tech.smartboot.feat.cloud.mcp.model.Request;
import tech.smartboot.feat.cloud.mcp.model.Response;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.stream.ServerSentEventStream;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author 三刀
 * @version v1.0 7/9/25
 */
final class SseTransport extends Transport {
    private static final Logger LOGGER = LoggerFactory.getLogger(SseTransport.class);
    private final HttpClient httpClient;
    private final HttpClient sseClient;
    private String endpoint;
    private CountDownLatch latch = new CountDownLatch(1);
    private final Map<Integer, CompletableFuture<Response<JSONObject>>> responseCallbacks = new ConcurrentHashMap<>();

    public SseTransport(McpOptions options) {
        super(options);
        httpClient = new HttpClient(options.getBaseUrl());
        sseClient = new HttpClient(options.getBaseUrl());
        sseClient.post(options.getSseEndpoint()).header(header -> header.set(HeaderName.ACCEPT, HeaderValue.ContentType.EVENT_STREAM).set(HeaderName.CACHE_CONTROL.getName(), HeaderValue.NO_CACHE).set(HeaderName.CONNECTION.getName(), HeaderValue.Connection.KEEPALIVE)).onResponseBody(new ServerSentEventStream() {
            @Override
            public void onEvent(HttpResponse httpResponse, Map<String, String> event) {
                String e = event.get(ServerSentEventStream.EVENT);
                String data = event.get(ServerSentEventStream.DATA);
                if ("endpoint".equals(e)) {
                    endpoint = data;
                    latch.countDown();
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(data);

                    Response<JSONObject> response = handleServerRequest(jsonObject);
                    if (response != null) {
                        try {
                            doRequest(httpClient.post(endpoint), response);
                        } catch (Throwable throwable) {
                            LOGGER.error("send error", throwable);
                        }
                        return;
                    }
                    response = jsonObject.to(new TypeReference<Response<JSONObject>>() {
                    });
                    if (response.getId() == null) {
                        System.out.println("no id");
                        return;
                    }
                    CompletableFuture<Response<JSONObject>> future = responseCallbacks.remove(response.getId());
                    if (future != null) {
                        if (response.getError() != null) {
                            future.completeExceptionally(new McpException(response.getError().getInteger("code"), response.getError().getString("message")));
                        } else {
                            future.complete(response);
                        }
                    }
                }
            }
        }).onFailure(throwable -> {
            System.out.println("sse error");
            throwable.printStackTrace();
        }).submit();
    }

    @Override
    void initialized() {

    }

    @Override
    protected CompletableFuture<Response<JSONObject>> doRequest(CompletableFuture<Response<JSONObject>> future, Request<JSONObject> request) {
        if ("initialize".equals(request.getMethod())) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (FeatUtils.isBlank(endpoint)) {
            future.completeExceptionally(new FeatException("endpoint not found"));
            return future;
        }
        responseCallbacks.put(request.getId(), future);
        try {
            HttpResponse response = doRequest(httpClient.post(endpoint), request).get();
            System.out.println("status: " + response.statusCode());
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<HttpResponse> sendNotification(String method) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        Request<JSONObject> request = new Request<>();
        request.setMethod(method);
        byte[] body = JSONObject.toJSONString(request).getBytes();
        httpClient.post(endpoint).header(header -> {
            header.setContentType(HeaderValue.ContentType.APPLICATION_JSON).setContentLength(body.length);
            if (FeatUtils.isNotBlank(sessionId)) {
                header.set(Request.HEADER_SESSION_ID, sessionId);
            }
        }).body(b -> b.write(body)).onSuccess(future::complete).onFailure(future::completeExceptionally).submit();
        return future;
    }

    @Override
    public void close() {
        latch.countDown();
        httpClient.close();
        sseClient.close();
    }
}
