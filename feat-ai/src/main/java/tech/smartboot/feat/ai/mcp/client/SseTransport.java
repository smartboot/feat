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
import tech.smartboot.feat.core.client.sse.SseClient;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpMethod;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/9/25
 */
final class SseTransport extends Transport {
    private static final Logger LOGGER = LoggerFactory.getLogger(SseTransport.class);
    private final HttpClient httpClient;
    private final SseClient sseClient;
    private String endpoint;
    private CountDownLatch latch = new CountDownLatch(1);
    private final Map<Integer, CompletableFuture<Response<JSONObject>>> responseCallbacks = new ConcurrentHashMap<>();

    public SseTransport(McpOptions options) {
        super(options);
        httpClient = new HttpClient(options.getBaseUrl());
        String sseUrl = options.getBaseUrl().endsWith("/") ? options.getBaseUrl() : options.getBaseUrl() + "/";
        if (options.getSseEndpoint().charAt(0) == '/') {
            sseUrl += options.getSseEndpoint().substring(1);
        } else {
            sseUrl += options.getSseEndpoint();
        }
        sseClient = Feat.sse(sseUrl, opt -> {
            opt.setMethod(HttpMethod.POST).httpOptions().setHeaders(options.getHeaders());
        }).onEvent("endpoint", event -> {
            endpoint = event.getData();
            latch.countDown();
        }).onData(event -> {
            String data = event.getData();
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
        }).onError(throwable -> {
            System.out.println("sse error");
            throwable.printStackTrace();
        });
        sseClient.connect();
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
        latch.countDown();
        httpClient.close();
        sseClient.disconnect();
    }
}
