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

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.cloud.mcp.McpInitializeRequest;
import tech.smartboot.feat.cloud.mcp.McpInitializeResponse;
import tech.smartboot.feat.cloud.mcp.PromptMessage;
import tech.smartboot.feat.cloud.mcp.Request;
import tech.smartboot.feat.cloud.mcp.Resource;
import tech.smartboot.feat.cloud.mcp.Response;
import tech.smartboot.feat.cloud.mcp.client.model.GetPromptResult;
import tech.smartboot.feat.cloud.mcp.client.model.PromptListResponse;
import tech.smartboot.feat.cloud.mcp.client.model.ResourceListResponse;
import tech.smartboot.feat.cloud.mcp.client.model.ToolListResponse;
import tech.smartboot.feat.cloud.mcp.server.model.PromptResult;
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

    public PromptListResponse ListPrompts() {
        return ListPrompts(null);
    }


    public CompletableFuture<JSONObject> asyncCallTool(String toolName, JSONObject arguments) {
        JSONObject param = new JSONObject();
        param.put("name", toolName);
        param.put("arguments", arguments);
        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("tools/call", param);
        f.thenAccept(response -> {
            future.complete(response.getResult());
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        return future;
    }

    public JSONObject callTool(String toolName, JSONObject arguments) {
        try {
            return asyncCallTool(toolName, arguments).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<PromptListResponse> AsyncListPrompts(String nextCursor) {
        CompletableFuture<PromptListResponse> future = new CompletableFuture<>();
        JSONObject param = new JSONObject();
        if (FeatUtils.isNotBlank(nextCursor)) {
            param.put("cursor", nextCursor);
        }
        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("prompts/list", param);
        f.thenAccept(response -> {
            PromptListResponse toolListResponse = response.getResult().to(PromptListResponse.class);
            future.complete(toolListResponse);
        });
        return future;
    }

    public PromptListResponse ListPrompts(String nextCursor) {
        try {
            return AsyncListPrompts(nextCursor).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<GetPromptResult> asyncGetPrompt(String name, JSONObject arguments) {
        JSONObject param = new JSONObject();
        param.put("name", name);
        if (arguments != null) {
            param.put("arguments", arguments);
        }
        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("prompts/get", param);
        CompletableFuture<GetPromptResult> future = new CompletableFuture<>();
        f.thenAccept(response -> {
            JSONObject result = response.getResult();
            GetPromptResult promptMessage = new GetPromptResult();
            promptMessage.setDescription(result.getString("description"));
            JSONArray messages = result.getJSONArray("messages");
            for (int i = 0; i < messages.size(); i++) {
                JSONObject message = messages.getJSONObject(i);
                JSONObject content = message.getJSONObject("content");
                String type = content.getString("type");
                PromptMessage promptResult = new PromptMessage();
                promptResult.setRole(message.getString("role"));
                switch (type) {
                    case "text":
                        promptResult.setContent(new PromptResult.TextPromptContent(content.getString("text")));
                        break;
                    case "image":
                        promptResult.setContent(new PromptResult.ImagePromptContent(content.getString("data"), content.getString("mimeType")));
                        break;
                    case "audio":
                        promptResult.setContent(new PromptResult.AudioPromptContent(content.getString("data"), content.getString("mimeType")));
                        break;
                    case "resource":
                        JSONObject resource = content.getJSONObject("resource");
                        promptResult.setContent(new PromptResult.EmbeddedResourcePromptContent(resource.to(Resource.class)));
                        break;
                    default:
                        throw new FeatException("unknown prompt content type: " + type);
                }
                promptMessage.getMessages().add(promptResult);
                future.complete(promptMessage);
            }
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        return future;
    }

    public GetPromptResult getPrompt(String name) {
        return getPrompt(name, null);
    }

    public GetPromptResult getPrompt(String name, JSONObject arguments) {
        try {
            return asyncGetPrompt(name, arguments).get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public ResourceListResponse ListResources() {
        return ListResources(null);
    }

    public CompletableFuture<ResourceListResponse> AsyncListResources(String nextCursor) {
        CompletableFuture<ResourceListResponse> future = new CompletableFuture<>();
        JSONObject param = new JSONObject();
        if (FeatUtils.isNotBlank(nextCursor)) {
            param.put("cursor", nextCursor);
        }
        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("resources/list", param);
        f.thenAccept(response -> {
            ResourceListResponse toolListResponse = response.getResult().to(ResourceListResponse.class);
            future.complete(toolListResponse);
        });
        return future;
    }

    public ResourceListResponse ListResources(String nextCursor) {
        try {
            return AsyncListResources(nextCursor).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
