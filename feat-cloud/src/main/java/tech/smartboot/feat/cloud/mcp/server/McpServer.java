/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import tech.smartboot.feat.cloud.mcp.model.McpInitializeRequest;
import tech.smartboot.feat.cloud.mcp.model.McpInitializeResponse;
import tech.smartboot.feat.cloud.mcp.model.Request;
import tech.smartboot.feat.cloud.mcp.model.ResourceTemplate;
import tech.smartboot.feat.cloud.mcp.model.Response;
import tech.smartboot.feat.cloud.mcp.model.ServerCapabilities;
import tech.smartboot.feat.cloud.mcp.server.handler.CompletionCompleteHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.ListPromptsHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.LoggingSetLevelHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.PingHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.PromptsGetHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.ResourcesListHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.ResourcesReadHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.ResourcesTemplateListHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.ServerHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.ToolsCallHandler;
import tech.smartboot.feat.cloud.mcp.server.handler.ToolsListHandler;
import tech.smartboot.feat.cloud.mcp.server.model.ServerPrompt;
import tech.smartboot.feat.cloud.mcp.server.model.ServerResource;
import tech.smartboot.feat.cloud.mcp.server.model.ServerTool;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.upgrade.sse.SSEUpgrade;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;
import tech.smartboot.feat.router.RouterHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class McpServer {
    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);
    private final McpOptions options = new McpOptions();
    private final Map<String, ServerHandler> handlers = new HashMap<>();

    {
        handlers.put("tools/list", new ToolsListHandler());
        handlers.put("tools/call", new ToolsCallHandler());
        handlers.put("prompts/list", new ListPromptsHandler());
        handlers.put("prompts/get", new PromptsGetHandler());
        handlers.put("resources/list", new ResourcesListHandler());
        handlers.put("resources/read", new ResourcesReadHandler());
        handlers.put("resources/templates/list", new ResourcesTemplateListHandler());
        handlers.put("completion/complete", new CompletionCompleteHandler());
        handlers.put("logging/setLevel", new LoggingSetLevelHandler());
        handlers.put("ping", new PingHandler());
    }

    private final Map<String, StreamSession> sseEmitters = new ConcurrentHashMap<>();
    private final List<ServerTool> tools = new ArrayList<>();
    private final List<ServerPrompt> prompts = new ArrayList<>();
    private final List<ServerResource> resources = new ArrayList<>();
    private final List<ResourceTemplate> resourceTemplates = new ArrayList<>();

    public List<ServerTool> getTools() {
        return tools;
    }

    public List<ServerPrompt> getPrompts() {
        return prompts;
    }

    public List<ServerResource> getResources() {
        return resources;
    }

    public List<ResourceTemplate> getResourceTemplates() {
        return resourceTemplates;
    }

    public McpServer addPrompt(ServerPrompt prompt) {
        prompts.stream().filter(p -> p.getName().equals(prompt.getName())).findAny().ifPresent(p -> {
            throw new IllegalStateException("prompt already exists");
        });
        prompts.add(prompt);
        notification("notifications/prompts/list_changed");
        return this;
    }

    public McpServer addTool(ServerTool tool) {
        tools.stream().filter(t -> t.getName().equals(tool.getName())).findAny().ifPresent(t -> {
            throw new IllegalStateException("tool already exists");
        });
        tools.add(tool);
        notification("notifications/tools/list_changed");

        return this;
    }

    private void notification(String method) {
        if (sseEmitters.isEmpty()) {
            return;
        }
        Request notify = new Request();
        notify.setMethod(method);
        String data = JSON.toJSONString(notify);
        sseEmitters.forEach((sessionId, session) -> {
            try {
                if (session.getSseEmitter() != null) {
                    session.getSseEmitter().send(data);
                }
            } catch (IOException e) {
                logger.error("notification exception", e);
            }
        });
    }

    public McpServer addResource(ServerResource resource) {
        if (FeatUtils.isBlank(resource.getUri())) {
            throw new IllegalStateException("uri can not be null");
        }
        if (FeatUtils.isBlank(resource.getName())) {
            throw new IllegalStateException("name can not be null");
        }
        if (resources.stream().anyMatch(r -> r.getUri().equals(resource.getUri()))) {
            throw new IllegalStateException("resource already exists");
        }
        resources.add(resource);
        notification("notifications/resources/list_changed");
        return this;
    }

    public McpServer addResourceTemplate(ResourceTemplate resourceTemplate) {
        if (FeatUtils.isBlank(resourceTemplate.getUriTemplate())) {
            throw new IllegalStateException("uriTemplate can not be null");
        }
        if (FeatUtils.isBlank(resourceTemplate.getName())) {
            throw new IllegalStateException("name can not be null");
        }
        if (resourceTemplates.stream().anyMatch(r -> r.getUriTemplate().equals(resourceTemplate.getUriTemplate()))) {
            throw new IllegalStateException("resourceTemplate already exists");
        }
        resourceTemplates.add(resourceTemplate);
        return this;
    }

    public McpOptions getOptions() {
        return options;
    }

    public Map<String, StreamSession> getSseEmitters() {
        return sseEmitters;
    }

    public Response jsonRpcHandle(StreamSession session, HttpRequest request) throws Throwable {
        if (session == null) {
            throw new HttpException(HttpStatus.UNAUTHORIZED);
        }
        switch (session.getState()) {
            case StreamSession.STATE_INITIALIZE: {
                Request<McpInitializeRequest> req = JSON.parseObject(FeatUtils.asString(request.getInputStream()), new TypeReference<Request<McpInitializeRequest>>() {
                });
                if (req == null) {
                    request.getResponse().close();
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                session.setInitializeRequest(req.getParams());
                Response<McpInitializeResponse> rsp = new Response<>();
                rsp.setId(req.getId());
                McpInitializeResponse initializeResponse = new McpInitializeResponse();
                ServerCapabilities capabilities = initializeResponse.getCapabilities();
                if (options.isLoggingEnable()) {
                    capabilities.setLogging(new JSONObject());
                }
                if (options.isPromptsEnable()) {
                    JSONObject capability = new JSONObject();
                    capability.put("listChanged", true);
                    capabilities.setPrompts(capability);
                }
                if (options.isResourceEnable()) {
                    JSONObject capability = new JSONObject();
                    capability.put("listChanged", true);
                    capability.put("subscribe", true);
                    capabilities.setResources(capability);
                }
                if (options.isToolEnable()) {
                    JSONObject capability = new JSONObject();
                    capability.put("listChanged", true);
                    capabilities.setTools(capability);
                }
                initializeResponse.setServerInfo(options.getImplementation());
                rsp.setResult(initializeResponse);
                session.setState(StreamSession.STATE_INITIALIZED);
                return rsp;
            }
            case StreamSession.STATE_INITIALIZED: {
                String json1 = FeatUtils.asString(request.getInputStream());
                System.out.println(json1);
                session.setState(StreamSession.STATE_READY);
                request.getResponse().setHttpStatus(HttpStatus.ACCEPTED);
                return null;
            }

            case StreamSession.STATE_READY: {
                System.out.println("ready: " + request);
                if (HeaderValue.ContentType.APPLICATION_JSON.equals(request.getContentType())) {
                    JSONObject jsonObject = JSON.parseObject(request.getInputStream());
                    String method = jsonObject.getString("method");
                    if (FeatUtils.isBlank(method)) {
                        int id = jsonObject.getIntValue("id", -1);
                        Consumer<JSONObject> response = session.doResponse(id);
                        if (response != null) {
                            response.accept(jsonObject.getJSONObject("result"));
                            return null;
                        } else {
                            throw new FeatException("method is null");
                        }
                    } else if ("notifications/roots/list_changed".equalsIgnoreCase(method)) {
                        session.rootsList();
                        return null;
                    }


                    ServerHandler handler = handlers.get(method);
                    Response<JSONObject> response = new Response<>();

                    try {
                        JSONObject result = handler.apply(this, request, jsonObject);
                        response.setResult(result);
                    } catch (McpServerException e) {
                        JSONObject error = new JSONObject();
                        error.put("code", e.getCode());
                        error.put("message", e.getMessage());
                        error.put("data", e.getData());
                        response.setError(error);
                    } catch (Throwable e) {
                        logger.error("", e);
                        JSONObject error = new JSONObject();
                        error.put("code", -32603);
                        error.put("message", e.toString());
                        response.setError(error);

                    }
                    response.setId(jsonObject.getInteger("id"));
                    return response;
                }
                break;
            }
        }
        throw new RuntimeException("Not Found");
    }

    private void initialized(StreamSession session) throws IOException {
        JSONObject capabilities = session.getInitializeRequest().getCapabilities();
        if (capabilities == null) {
            return;
        }
        JSONObject roots = capabilities.getJSONObject("roots");
        if (roots != null && roots.getBooleanValue("listChanged")) {
            session.rootsList();
        }
    }

    public RouterHandler sseHandler() {
        return ctx -> {
            String sessionId = ctx.Request.getHeader("mcp-session-id");
            StreamSession session;
            if (sessionId == null) {
                session = new StreamSession();
                sseEmitters.put(session.getSessionId(), session);
            } else {
                session = sseEmitters.get(sessionId);
            }
            if (session == null) {
                ctx.Response.setHttpStatus(HttpStatus.UNAUTHORIZED);
                ctx.Response.close();
                return;
            }
            ctx.Request.upgrade(new SSEUpgrade() {
                @Override
                public void onOpen(SseEmitter sseEmitter) throws IOException {
                    System.out.println("onOpen");
                    if (session.getSseEmitter() != null) {
                        session.getSseEmitter().complete();
                    }
                    session.setSseEmitter(sseEmitter);
                    sseEmitter.send(SseEmitter.event().name("endpoint").data(options.getSseMessageEndpoint() + "?session_id=" + session.getSessionId()));
                }
            });
        };
    }

    public RouterHandler sseMessageHandler() {
        return ctx -> {
            StreamSession session = sseEmitters.get(ctx.Request.getParameter("session_id"));
            ctx.Response.setHttpStatus(HttpStatus.ACCEPTED);
            int preStatus = session.getState();
            Response response = jsonRpcHandle(session, ctx.Request);
            if (response != null) {
                session.getSseEmitter().send(JSONObject.toJSONString(response), (Throwable throwable) -> {
                    logger.error("send error", throwable);
                });
            } else if (preStatus == StreamSession.STATE_INITIALIZED && session.getState() == StreamSession.STATE_READY) {
                initialized(session);
            }
        };
    }

    public RouterHandler mcpHandler() {
        return ctx -> {
            HttpRequest request = ctx.Request;
            String sessionId = request.getHeader("mcp-session-id");
            StreamSession session;
            if (sessionId == null) {
                session = new StreamSession();
                sessionId = session.getSessionId();
                sseEmitters.put(session.getSessionId(), session);
            } else {
                session = sseEmitters.get(sessionId);
            }
            if (session == null) {
                request.getResponse().setHttpStatus(HttpStatus.UNAUTHORIZED);
                request.getResponse().close();
                return;
            }
            if (request.getContentType() == null && HeaderValue.ContentType.EVENT_STREAM.equalsIgnoreCase(request.getHeader(HeaderName.ACCEPT))) {
                request.upgrade(new SSEUpgrade() {
                    @Override
                    public void onOpen(SseEmitter sseEmitter) throws IOException {
                        if (session.getSseEmitter() != null) {
                            session.getSseEmitter().complete();
                        }
                        session.setSseEmitter(sseEmitter);
                        initialized(session);
                        System.out.println("onOpen");
                    }
                });
                return;
            }
            Response response = jsonRpcHandle(session, request);
            if (response != null) {
                byte[] bytes = JSON.toJSONBytes(response);
                request.getResponse().setContentLength(bytes.length);
                request.getResponse().setContentType(HeaderValue.ContentType.APPLICATION_JSON);
                if (response.getResult() instanceof McpInitializeResponse) {
                    request.getResponse().setHeader("Mcp-Session-Id", sessionId);
                }
                request.getResponse().write(bytes);
            }
        };
    }
}
