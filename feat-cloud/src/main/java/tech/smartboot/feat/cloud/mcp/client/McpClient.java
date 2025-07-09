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
import tech.smartboot.feat.cloud.mcp.enums.TransportTypeEnum;
import tech.smartboot.feat.cloud.mcp.server.Request;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 7/7/25
 */
public class McpClient {
    private final McpOptions options;
    private HttpClient httpClient;
    private String sessionId;
    private TransportTypeEnum transportType;

    private McpClient(McpOptions options, TransportTypeEnum transportType) {
        this.options = options;
        httpClient = new HttpClient(options.getBaseUrl());
        this.transportType = transportType;
    }

    public static McpClient newSseClient(Consumer<McpOptions> opt) {
        McpOptions options = new McpOptions();
        opt.accept(options);
        return new McpClient(options, TransportTypeEnum.Sse);
    }

    public static McpClient newStreamableClient(Consumer<McpOptions> opt) {
        McpOptions options = new McpOptions();
        opt.accept(options);
        return new McpClient(options, TransportTypeEnum.Streamable);
    }

    public CompletableFuture<McpClient> AsyncInitialize(ClientCapabilities capabilities) {
        CompletableFuture<McpClient> future = new CompletableFuture<>();
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

        Request<McpInitializeRequest> jsonrpcRequest = new Request<>();
        jsonrpcRequest.setMethod("initialize");
        jsonrpcRequest.setParams(request);

        sendRequest(jsonrpcRequest).onSuccess(response -> {
            sessionId = response.getHeader("Mcp-Session-Id");
            McpInitializeResponse r = JSONObject.parseObject(response.body(), McpInitializeResponse.class);
            System.out.println("response:" + response.body());
            Request initialized = new Request();
            initialized.setMethod("notifications/initialized");
            sendRequest(initialized)
                    .onSuccess(response1 -> {
                        future.complete(this);
                    }).onFailure(future::completeExceptionally)
                    .submit();
        }).onFailure(future::completeExceptionally).submit();
        return future;
    }

    public McpClient Initialize(ClientCapabilities capabilities) {
        try {
            return AsyncInitialize(capabilities).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    private HttpRest sendRequest(Request request) {
        byte[] body = JSONObject.toJSONString(request).getBytes();
        return httpClient.post(options.getMcpEndpoint()).header(header -> {
            header.setContentType(HeaderValue.ContentType.APPLICATION_JSON).setContentLength(body.length);
            if (FeatUtils.isNotBlank(sessionId)) {
                header.set("Mcp-Session-Id", sessionId);
            }
        }).body(b -> b.write(body));
    }

}
