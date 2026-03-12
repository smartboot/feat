/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import tech.smartboot.feat.ai.a2a.A2AException;
import tech.smartboot.feat.ai.a2a.model.AgentCard;
import tech.smartboot.feat.ai.a2a.model.JsonRpcError;
import tech.smartboot.feat.ai.a2a.model.JsonRpcRequest;
import tech.smartboot.feat.ai.a2a.model.JsonRpcResponse;
import tech.smartboot.feat.ai.a2a.model.PushNotificationConfig;
import tech.smartboot.feat.ai.a2a.model.SendTaskRequest;
import tech.smartboot.feat.ai.a2a.model.SubscribeRequest;
import tech.smartboot.feat.ai.a2a.model.Task;
import tech.smartboot.feat.ai.a2a.model.TaskCancelRequest;
import tech.smartboot.feat.ai.a2a.model.TaskQueryRequest;
import tech.smartboot.feat.ai.a2a.model.TaskResponse;
import tech.smartboot.feat.ai.a2a.model.SetPushNotificationRequest;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.RouterHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A2A (Agent-to-Agent) 服务器实现类
 *
 * <p>提供完整的A2A协议服务器端实现，支持智能体发现、任务管理、推送通知等功能。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>智能体卡片自动发布（/.well-known/agent.json）</li>
 *   <li>JSON-RPC协议支持</li>
 *   <li>任务生命周期管理</li>
 *   <li>推送通知支持</li>
 *   <li>可扩展的任务处理器</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class A2AServer {
    private static final Logger logger = LoggerFactory.getLogger(A2AServer.class);

    private final A2AServerOptions options;
    private TaskHandler taskHandler;

    /**
     * 构造A2A服务器
     *
     * @param options 服务器配置选项
     */
    public A2AServer(A2AServerOptions options) {
        this.options = options;
    }

    /**
     * 构造A2A服务器
     *
     * @param consumer 配置选项消费者
     */
    public A2AServer(Consumer<A2AServerOptions> consumer) {
        this.options = new A2AServerOptions();
        consumer.accept(options);
    }

    /**
     * 设置任务处理器
     *
     * @param taskHandler 任务处理器
     * @return 当前A2AServer实例（链式调用）
     */
    public A2AServer taskHandler(TaskHandler taskHandler) {
        this.taskHandler = taskHandler;
        return this;
    }

    /**
     * 注册路由处理器
     *
     * <p>将A2A端点注册到Feat路由中。</p>
     *
     * @param router Feat路由实例
     * @return 当前A2AServer实例（链式调用）
     */
    public A2AServer register(Router router) {
        // 注册Agent Card端点
        router.route("/.well-known/agent.json", new RouterHandler() {
            @Override
            public void handle(Context ctx, CompletableFuture<Void> completableFuture) throws Throwable {
                handleAgentCard(ctx);
                completableFuture.complete(null);
            }

            @Override
            public void handle(Context ctx) throws Exception {
                throw new IllegalStateException();
            }
        });

        // 注册JSON-RPC端点
        router.route("/", new RouterHandler() {
            @Override
            public void handle(Context ctx, CompletableFuture<Void> completableFuture) throws Throwable {
                handleJsonRpc(ctx, completableFuture);
            }

            @Override
            public void handle(Context ctx) throws Exception {
                throw new IllegalStateException();
            }
        });

        return this;
    }

    /**
     * 处理Agent Card请求
     *
     * @param ctx 路由上下文
     */
    private void handleAgentCard(Context ctx) {
        try {
            AgentCard agentCard = options.buildAgentCard();
            ctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            ctx.Response.write(JSON.toJSONBytes(agentCard));
        } catch (Exception e) {
            logger.error("Failed to generate agent card", e);
            ctx.Response.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            try {
                ctx.Response.write(("Failed to generate agent card: " + e.getMessage()).getBytes());
            } catch (IOException ex) {
                logger.error("Failed to write error response", ex);
            }
        }
    }

    /**
     * 处理JSON-RPC请求
     *
     * @param ctx 路由上下文
     * @param completableFuture 完成Future
     */
    private void handleJsonRpc(Context ctx, CompletableFuture<Void> completableFuture) {
        if (!"POST".equalsIgnoreCase(ctx.Request.getMethod())) {
            ctx.Response.setHttpStatus(HttpStatus.METHOD_NOT_ALLOWED);
            try {
                ctx.Response.write("Method not allowed".getBytes());
            } catch (IOException e) {
                logger.error("Failed to write response", e);
            }
            completableFuture.complete(null);
            return;
        }

        String body = null;
        try {
            int contentLength = (int) ctx.Request.getContentLength();
            byte[] bytes = new byte[contentLength];
            ctx.Request.getInputStream().read(bytes);
            body = new String(bytes);
        } catch (IOException e) {
            sendError(ctx, null, JsonRpcError.INVALID_REQUEST, "Failed to read request body");
            completableFuture.complete(null);
            return;
        }

        if (body == null || body.isEmpty()) {
            sendError(ctx, null, JsonRpcError.INVALID_REQUEST, "Request body is empty");
            completableFuture.complete(null);
            return;
        }

        try {
            JsonRpcRequest<JSONObject> rpcRequest = JSON.parseObject(body, new TypeReference<JsonRpcRequest<JSONObject>>() {
            });

            if (rpcRequest.getMethod() == null) {
                sendError(ctx, rpcRequest.getId(), JsonRpcError.INVALID_REQUEST, "Method is required");
                completableFuture.complete(null);
                return;
            }

            handleMethod(ctx, rpcRequest, completableFuture);
        } catch (Exception e) {
            logger.error("Failed to parse JSON-RPC request", e);
            sendError(ctx, null, JsonRpcError.PARSE_ERROR, "Failed to parse request: " + e.getMessage());
            completableFuture.complete(null);
        }
    }

    /**
     * 处理具体的JSON-RPC方法
     *
     * @param ctx 路由上下文
     * @param request JSON-RPC请求
     * @param completableFuture 完成Future
     */
    private void handleMethod(Context ctx, JsonRpcRequest<JSONObject> request, CompletableFuture<Void> completableFuture) {
        if (taskHandler == null) {
            sendError(ctx, request.getId(), JsonRpcError.INTERNAL_ERROR, "Task handler not configured");
            completableFuture.complete(null);
            return;
        }

        String method = request.getMethod();
        JSONObject params = request.getParams();

        try {
            Object result;
            switch (method) {
                case "tasks/send":
                    SendTaskRequest sendRequest = params.toJavaObject(SendTaskRequest.class);
                    result = taskHandler.sendTask(sendRequest);
                    break;
                case "tasks/get":
                    TaskQueryRequest queryRequest = params.toJavaObject(TaskQueryRequest.class);
                    result = taskHandler.getTask(queryRequest);
                    break;
                case "tasks/cancel":
                    TaskCancelRequest cancelRequest = params.toJavaObject(TaskCancelRequest.class);
                    result = taskHandler.cancelTask(cancelRequest);
                    break;
                case "tasks/subscribe":
                    SubscribeRequest subscribeRequest = params.toJavaObject(SubscribeRequest.class);
                    result = taskHandler.subscribe(subscribeRequest);
                    break;
                case "tasks/pushNotification/set":
                    SetPushNotificationRequest pushRequest = params.toJavaObject(SetPushNotificationRequest.class);
                    result = taskHandler.setPushNotification(pushRequest);
                    break;
                default:
                    sendError(ctx, request.getId(), JsonRpcError.METHOD_NOT_FOUND, "Method not found: " + method);
                    completableFuture.complete(null);
                    return;
            }

            sendSuccess(ctx, request.getId(), result);
            completableFuture.complete(null);
        } catch (A2AException e) {
            logger.error("A2A error: {}", e.getMessage(), e);
            sendError(ctx, request.getId(), JsonRpcError.INTERNAL_ERROR, e.getMessage());
            completableFuture.complete(null);
        } catch (Exception e) {
            logger.error("Internal error", e);
            sendError(ctx, request.getId(), JsonRpcError.INTERNAL_ERROR, "Internal error: " + e.getMessage());
            completableFuture.complete(null);
        }
    }

    /**
     * 发送成功的JSON-RPC响应
     *
     * @param ctx 路由上下文
     * @param id 请求ID
     * @param result 响应结果
     */
    private void sendSuccess(Context ctx, String id, Object result) {
        try {
            JsonRpcResponse<Object> rpcResponse = new JsonRpcResponse<>();
            rpcResponse.setId(id);
            rpcResponse.setResult(result);
            ctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            ctx.Response.write(JSON.toJSONBytes(rpcResponse));
        } catch (Exception e) {
            logger.error("Failed to send success response", e);
        }
    }

    /**
     * 发送错误的JSON-RPC响应
     *
     * @param ctx 路由上下文
     * @param id 请求ID
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     */
    private void sendError(Context ctx, String id, int errorCode, String errorMessage) {
        try {
            JsonRpcResponse<Object> rpcResponse = JsonRpcResponse.error(id, errorCode, errorMessage);
            ctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            ctx.Response.setHttpStatus(HttpStatus.OK);
            ctx.Response.write(JSON.toJSONBytes(rpcResponse));
        } catch (Exception e) {
            logger.error("Failed to send error response", e);
        }
    }

    /**
     * 获取AgentCard
     *
     * @return AgentCard实例
     */
    public AgentCard getAgentCard() {
        return options.buildAgentCard();
    }

    /**
     * 获取服务器配置选项
     *
     * @return 配置选项
     */
    public A2AServerOptions getOptions() {
        return options;
    }
}
