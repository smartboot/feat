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
import tech.smartboot.feat.cloud.mcp.McpInitializeRequest;
import tech.smartboot.feat.cloud.mcp.McpInitializeResponse;
import tech.smartboot.feat.cloud.mcp.Request;
import tech.smartboot.feat.cloud.mcp.Response;
import tech.smartboot.feat.cloud.mcp.client.model.ToolListResponse;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 7/7/25
 */
public class McpClient {
    private final McpOptions options;
    private HttpClient httpClient;
    private String sessionId;
    private Transport transport;
    private boolean initialized = false;

    private McpClient(McpOptions options, Transport transport) {
        this.options = options;
        httpClient = new HttpClient(options.getBaseUrl());
        this.transport = transport;
    }

    public static McpClient newSseClient(Consumer<McpOptions> opt) {
        McpOptions options = new McpOptions();
        opt.accept(options);
        return new McpClient(options, new SseTransport(options));
    }

    public static McpClient newStreamableClient(Consumer<McpOptions> opt) {
        McpOptions options = new McpOptions();
        opt.accept(options);
        return new McpClient(options, new StreamableTransport(options));
    }

    public CompletableFuture<McpInitializeResponse> AsyncInitialize(ClientCapabilities capabilities) {
        CompletableFuture<McpInitializeResponse> future = new CompletableFuture<>();
        McpInitializeRequest request = new McpInitializeRequest();
        request.setProtocolVersion(McpInitializeRequest.PROTOCOL_VERSION);
        JSONObject capabilitiesJson = new JSONObject();
        if (capabilities.isRoots()) {
            capabilitiesJson.put("roots", JSONObject.of("listChanged", true));
        }
        if (capabilities.isSampling()) {
            capabilitiesJson.put("sampling", new JSONObject());
        }
        if (capabilities.isElicitation()) {
            capabilitiesJson.put("elicitation", new JSONObject());
        }
        if (capabilities.getExperimental() != null) {
            capabilitiesJson.put("experimental", capabilities.getExperimental());
        }
        request.setCapabilities(capabilitiesJson);
        request.setClientInfo(options.getImplementation());

        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("initialize", JSONObject.from(request));
        f.thenAccept(response -> {
            McpInitializeResponse initializeResponse = response.getResult().to(McpInitializeResponse.class);
            //After successful initialization, the client MUST send an initialized notification to indicate it is ready to begin normal operations
            Request<JSONObject> initializedNotify = new Request<>();
            initializedNotify.setMethod("notifications/initialized");
            initializedNotify.setParams(new JSONObject());
            CompletableFuture<HttpResponse> notification = transport.sendNotification(initializedNotify);
            notification.whenComplete((r, e) -> {
                if (e != null) {
                    future.completeExceptionally(e);
                } else if (r.statusCode() == HttpStatus.ACCEPTED.value()) {
                    initialized = true;
                    future.complete(initializeResponse);
                } else {
                    future.completeExceptionally(new FeatException("notification failed"));
                }
            });

        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        return future;
    }

    public McpInitializeResponse Initialize(ClientCapabilities capabilities) {
        try {
            return AsyncInitialize(capabilities).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    public ToolListResponse ListTools() {
        return ListTools(null);
    }

    public CompletableFuture<ToolListResponse> AsyncListTools(String nextCursor) {
        CompletableFuture<ToolListResponse> future = new CompletableFuture<>();
        JSONObject param = new JSONObject();
        if (FeatUtils.isNotBlank(nextCursor)) {
            param.put("cursor", nextCursor);
        }
        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("tools/list", param);
        f.thenAccept(response -> {
            ToolListResponse toolListResponse = response.getResult().to(ToolListResponse.class);
            future.complete(toolListResponse);
        });
        return future;
    }

    public ToolListResponse ListTools(String nextCursor) {
        try {
            return AsyncListTools(nextCursor).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
