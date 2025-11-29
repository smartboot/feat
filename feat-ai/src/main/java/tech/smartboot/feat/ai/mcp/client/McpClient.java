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

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.enums.LoggerLevel;
import tech.smartboot.feat.ai.mcp.enums.RoleEnum;
import tech.smartboot.feat.ai.mcp.model.McpInitializeRequest;
import tech.smartboot.feat.ai.mcp.model.McpInitializeResponse;
import tech.smartboot.feat.ai.mcp.model.PromptGetResult;
import tech.smartboot.feat.ai.mcp.model.PromptListResponse;
import tech.smartboot.feat.ai.mcp.model.PromptMessage;
import tech.smartboot.feat.ai.mcp.model.Resource;
import tech.smartboot.feat.ai.mcp.model.ResourceListResponse;
import tech.smartboot.feat.ai.mcp.model.Response;
import tech.smartboot.feat.ai.mcp.model.Roots;
import tech.smartboot.feat.ai.mcp.model.ToolCalledResult;
import tech.smartboot.feat.ai.mcp.model.ToolListResponse;
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/7/25
 */
public class McpClient {
    private final McpOptions options;
    private final Transport transport;
    private boolean initialized = false;

    private McpClient(McpOptions options, Transport transport) {
        this.options = options;
        this.transport = transport;
        options.setNotificationHandler(method -> {
            if ("notifications/tools/list_changed".equals(method)) {
                listTools();
            }
        });
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

    public CompletableFuture<McpInitializeResponse> asyncInitialize() {
        CompletableFuture<McpInitializeResponse> future = new CompletableFuture<>();
        McpInitializeRequest request = new McpInitializeRequest();
        request.setProtocolVersion(McpInitializeRequest.PROTOCOL_VERSION);
        JSONObject capabilitiesJson = new JSONObject();
        if (options.isRoots()) {
            capabilitiesJson.put("roots", JSONObject.of("listChanged", true));
        }
        if (options.isSampling()) {
            capabilitiesJson.put("sampling", new JSONObject());
        }
        if (options.isElicitation()) {
            capabilitiesJson.put("elicitation", new JSONObject());
        }
        if (options.getExperimental() != null) {
            capabilitiesJson.put("experimental", options.getExperimental());
        }
        request.setCapabilities(capabilitiesJson);
        request.setClientInfo(options.getImplementation());

        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("initialize", JSONObject.from(request));
        f.thenAccept(response -> {
            McpInitializeResponse initializeResponse = response.getResult().to(McpInitializeResponse.class);
            //After successful initialization, the client MUST send an initialized notification to indicate it is ready to begin normal operations
            CompletableFuture<HttpResponse> notification = transport.sendNotification("notifications/initialized");
            notification.whenComplete((r, e) -> {
                if (e != null) {
                    future.completeExceptionally(e);
                } else if (r.statusCode() == HttpStatus.ACCEPTED.value()) {
                    initialized = true;
                    transport.initialized();
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

    public McpInitializeResponse initialize() {
        try {
            return asyncInitialize().get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    public ToolListResponse listTools() {
        return listTools(null);
    }

    public CompletableFuture<ToolListResponse> asyncListTools(String nextCursor) {
        stateCheck();
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

    public ToolListResponse listTools(String nextCursor) {
        try {
            return asyncListTools(nextCursor).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    public PromptListResponse listPrompts() {
        return listPrompts(null);
    }


    public CompletableFuture<ToolCalledResult> asyncCallTool(String toolName, JSONObject arguments) {
        stateCheck();
        JSONObject param = new JSONObject();
        param.put("name", toolName);
        if (arguments != null) {
            param.put("arguments", arguments);
        }
        CompletableFuture<ToolCalledResult> future = new CompletableFuture<>();
        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("tools/call", param);
        f.thenAccept(response -> {
            JSONArray content = response.getResult().getJSONArray("content");
            ToolCalledResult callToolResult = new ToolCalledResult();
            callToolResult.setError(response.getResult().getBooleanValue("isError"));
            for (int i = 0; i < content.size(); i++) {
                JSONObject contentItem = content.getJSONObject(i);
                String type = contentItem.getString("type");
                switch (type) {
                    case "text":
                        callToolResult.addContent(contentItem.to(ToolResult.TextContent.class));
                        break;
                    case "image":
                        callToolResult.addContent(contentItem.to(ToolResult.ImageContent.class));
                        break;
                    case "audio":
                        callToolResult.addContent(contentItem.to(ToolResult.AudioContent.class));
                        break;
                    case "resource_link":
                        callToolResult.addContent(contentItem.to(ToolResult.ResourceLinks.class));
                        break;
                    default:
                        callToolResult.addContent(ToolResult.ofStructuredContent(content.getJSONObject(i)));
                }
            }
            future.complete(callToolResult);
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        return future;
    }

    public ToolCalledResult callTool(String toolName) {
        return callTool(toolName, null);
    }


    public ToolCalledResult callTool(String toolName, JSONObject arguments) {
        try {
            return asyncCallTool(toolName, arguments).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    public CompletableFuture<PromptListResponse> asyncListPrompts(String nextCursor) {
        stateCheck();
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

    public PromptListResponse listPrompts(String nextCursor) {
        try {
            return asyncListPrompts(nextCursor).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    public CompletableFuture<PromptGetResult> asyncGetPrompt(String name, JSONObject arguments) {
        stateCheck();
        JSONObject param = new JSONObject();
        param.put("name", name);
        if (arguments != null) {
            param.put("arguments", arguments);
        }
        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("prompts/get", param);
        CompletableFuture<PromptGetResult> future = new CompletableFuture<>();
        f.thenAccept(response -> {
            JSONObject result = response.getResult();
            PromptGetResult promptMessage = new PromptGetResult();
            promptMessage.setDescription(result.getString("description"));
            JSONArray messages = result.getJSONArray("messages");
            for (int i = 0; i < messages.size(); i++) {
                JSONObject message = messages.getJSONObject(i);
                JSONObject content = message.getJSONObject("content");
                String type = content.getString("type");
                RoleEnum roleEnum = RoleEnum.of(message.getString("role"));
                PromptMessage<?> promptResult = null;
                switch (type) {
                    case "text":
                        promptResult = PromptMessage.ofText(roleEnum, content.getString("text"));
                        break;
                    case "image":
                        promptResult = PromptMessage.ofImage(roleEnum, content.getString("data"), content.getString("mimeType"));
                        break;
                    case "audio":
                        promptResult = PromptMessage.ofAudio(roleEnum, content.getString("data"), content.getString("mimeType"));
                        break;
                    case "resource":
                        JSONObject resource = content.getJSONObject("resource");
                        promptResult = PromptMessage.ofEmbeddedResource(roleEnum, resource.to(Resource.class));
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

    public PromptGetResult getPrompt(String name) {
        return getPrompt(name, null);
    }

    public PromptGetResult getPrompt(String name, JSONObject arguments) {
        try {
            return asyncGetPrompt(name, arguments).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    public void setLoggingLevel(LoggerLevel level) {
//transport.sendNotification()
    }

    public ResourceListResponse listResources() {
        return listResources(null);
    }

    public CompletableFuture<ResourceListResponse> asyncListResources(String nextCursor) {
        stateCheck();
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

    public ResourceListResponse listResources(String nextCursor) {
        try {
            return asyncListResources(nextCursor).get();
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    public void addRoot(String uri, String name) {
        if (!options.isRoots()) {
            throw new FeatException("roots is not enabled");
        }
        options.getRootsList().add(new Roots(uri, name));
        transport.sendNotification("notifications/roots/list_changed");
    }

    public void removeRoot(String uri) {
        stateCheck();
        if (!options.isRoots()) {
            throw new FeatException("roots is not enabled");
        }
        options.getRootsList().removeIf(roots -> roots.getUri().equals(uri));
        transport.sendNotification("notifications/roots/list_changed");
    }
//    public void subscribeResource(String uri) {
//
//        CompletableFuture<Response<JSONObject>> f = transport.asyncRequest("resources/subscribe", JSONObject.of("uri",uri));
//    }

    private void stateCheck() {
        if (!initialized) {
            throw new IllegalStateException("McpClient not initialized");
        }
    }

    public void close() {
        transport.close();
    }
}
