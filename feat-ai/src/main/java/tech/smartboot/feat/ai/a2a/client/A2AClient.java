/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import tech.smartboot.feat.ai.a2a.A2AException;
import tech.smartboot.feat.ai.a2a.model.AgentCard;
import tech.smartboot.feat.ai.a2a.model.JsonRpcRequest;
import tech.smartboot.feat.ai.a2a.model.JsonRpcResponse;
import tech.smartboot.feat.ai.a2a.model.PushNotificationConfig;
import tech.smartboot.feat.ai.a2a.model.SendTaskRequest;
import tech.smartboot.feat.ai.a2a.model.SetPushNotificationRequest;
import tech.smartboot.feat.ai.a2a.model.SubscribeRequest;
import tech.smartboot.feat.ai.a2a.model.Task;
import tech.smartboot.feat.ai.a2a.model.TaskCancelRequest;
import tech.smartboot.feat.ai.a2a.model.TaskQueryRequest;
import tech.smartboot.feat.ai.a2a.model.TaskResponse;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * A2A (Agent-to-Agent) 客户端实现类
 *
 * <p>该类提供了与A2A服务器进行通信的完整客户端功能，支持任务管理、推送通知、
 * 流式响应等核心A2A协议功能。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>智能体发现（AgentCard获取）</li>
 *   <li>任务创建和管理</li>
 *   <li>任务状态查询</li>
 *   <li>任务取消</li>
 *   <li>推送通知配置</li>
 *   <li>流式响应支持</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class A2AClient {
    private final A2AOptions options;
    private final HttpClient httpClient;

    private A2AClient(A2AOptions options) {
        this.options = options;
        this.httpClient = new HttpClient(options.getEndpoint());
        this.httpClient.options()
                .connectTimeout(options.getConnectTimeout())
                .debug(options.isDebug());
    }

    /**
     * 创建A2A客户端实例
     *
     * <p>这是创建A2A客户端的标准方法。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * A2AClient client = A2AClient.create(options -> {
     *     options.setEndpoint("http://localhost:8080/a2a");
     *     options.setTimeout(30000);
     * });
     * }</pre>
     *
     * @param opt A2A配置选项消费者函数，用于配置客户端参数
     * @return 配置好的A2A客户端实例
     */
    public static A2AClient create(Consumer<A2AOptions> opt) {
        A2AOptions options = new A2AOptions();
        opt.accept(options);
        return new A2AClient(options);
    }

    /**
     * 获取智能体卡片（AgentCard）
     *
     * <p>从A2A服务器获取智能体的元数据和能力描述。</p>
     *
     * @return 智能体卡片
     * @throws A2AException 当获取失败时抛出
     */
    public AgentCard getAgentCard() {
        CompletableFuture<AgentCard> future = asyncGetAgentCard();
        try {
            return future.get();
        } catch (Exception e) {
            throw new A2AException("Failed to get agent card", e);
        }
    }

    /**
     * 异步获取智能体卡片（AgentCard）
     *
     * @return 包含AgentCard的CompletableFuture
     */
    public CompletableFuture<AgentCard> asyncGetAgentCard() {
        CompletableFuture<AgentCard> future = new CompletableFuture<>();
        HttpRest get = httpClient.get("/.well-known/agent.json");
        configureRequest(get);
        get.onSuccess(response -> {
            if (response.statusCode() == HttpStatus.OK.value()) {
                try {
                    AgentCard agentCard = JSON.parseObject(response.body(), AgentCard.class);
                    future.complete(agentCard);
                } catch (Exception e) {
                    future.completeExceptionally(new A2AException("Failed to parse agent card", e));
                }
            } else {
                future.completeExceptionally(new A2AException("Failed to get agent card: " + response.statusCode() + " - " + response.body()));
            }
        }).onFailure(throwable -> {
            future.completeExceptionally(new A2AException("Failed to get agent card", throwable));
        }).submit();
        return future;
    }

    /**
     * 发送任务
     *
     * <p>向A2A服务器提交一个新任务。</p>
     *
     * @param request 任务请求参数
     * @return 任务响应
     * @throws A2AException 当任务提交失败时抛出
     */
    public TaskResponse sendTask(SendTaskRequest request) {
        JsonRpcRequest<SendTaskRequest> rpcRequest = createJsonRpcRequest("tasks/send", request);
        JsonRpcResponse<TaskResponse> response = executeRpcRequest(rpcRequest, TaskResponse.class);
        if (response.isSuccess()) {
            return response.getResult();
        } else {
            throw new A2AException("Send task failed: " + response.getError().getMessage());
        }
    }

    /**
     * 异步发送任务
     *
     * @param request 任务请求参数
     * @return 包含TaskResponse的CompletableFuture
     */
    public CompletableFuture<TaskResponse> asyncSendTask(SendTaskRequest request) {
        return CompletableFuture.supplyAsync(() -> sendTask(request));
    }

    /**
     * 获取任务状态
     *
     * <p>查询指定任务的当前状态和消息历史。</p>
     *
     * @param request 任务查询请求
     * @return 任务对象
     * @throws A2AException 当查询失败时抛出
     */
    public Task getTask(TaskQueryRequest request) {
        JsonRpcRequest<TaskQueryRequest> rpcRequest = createJsonRpcRequest("tasks/get", request);
        JsonRpcResponse<Task> response = executeRpcRequest(rpcRequest, Task.class);
        if (response.isSuccess()) {
            return response.getResult();
        } else {
            throw new A2AException("Get task failed: " + response.getError().getMessage());
        }
    }

    /**
     * 异步获取任务状态
     *
     * @param request 任务查询请求
     * @return 包含Task的CompletableFuture
     */
    public CompletableFuture<Task> asyncGetTask(TaskQueryRequest request) {
        return CompletableFuture.supplyAsync(() -> getTask(request));
    }

    /**
     * 取消任务
     *
     * <p>取消指定ID的任务执行。</p>
     *
     * @param request 任务取消请求
     * @return 任务响应
     * @throws A2AException 当取消失败时抛出
     */
    public TaskResponse cancelTask(TaskCancelRequest request) {
        JsonRpcRequest<TaskCancelRequest> rpcRequest = createJsonRpcRequest("tasks/cancel", request);
        JsonRpcResponse<TaskResponse> response = executeRpcRequest(rpcRequest, TaskResponse.class);
        if (response.isSuccess()) {
            return response.getResult();
        } else {
            throw new A2AException("Cancel task failed: " + response.getError().getMessage());
        }
    }

    /**
     * 异步取消任务
     *
     * @param request 任务取消请求
     * @return 包含TaskResponse的CompletableFuture
     */
    public CompletableFuture<TaskResponse> asyncCancelTask(TaskCancelRequest request) {
        return CompletableFuture.supplyAsync(() -> cancelTask(request));
    }

    /**
     * 设置推送通知
     *
     * <p>为指定任务配置推送通知。</p>
     *
     * @param request 推送通知设置请求
     * @return 推送通知配置
     * @throws A2AException 当设置失败时抛出
     */
    public PushNotificationConfig setPushNotification(SetPushNotificationRequest request) {
        JsonRpcRequest<SetPushNotificationRequest> rpcRequest = createJsonRpcRequest("tasks/pushNotification/set", request);
        JsonRpcResponse<PushNotificationConfig> response = executeRpcRequest(rpcRequest, PushNotificationConfig.class);
        if (response.isSuccess()) {
            return response.getResult();
        } else {
            throw new A2AException("Set push notification failed: " + response.getError().getMessage());
        }
    }

    /**
     * 异步设置推送通知
     *
     * @param request 推送通知设置请求
     * @return 包含PushNotificationConfig的CompletableFuture
     */
    public CompletableFuture<PushNotificationConfig> asyncSetPushNotification(SetPushNotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> setPushNotification(request));
    }

    /**
     * 订阅任务更新
     *
     * <p>订阅指定任务的状态更新。</p>
     *
     * @param request 订阅请求
     * @return 任务响应
     * @throws A2AException 当订阅失败时抛出
     */
    public TaskResponse subscribe(SubscribeRequest request) {
        JsonRpcRequest<SubscribeRequest> rpcRequest = createJsonRpcRequest("tasks/subscribe", request);
        JsonRpcResponse<TaskResponse> response = executeRpcRequest(rpcRequest, TaskResponse.class);
        if (response.isSuccess()) {
            return response.getResult();
        } else {
            throw new A2AException("Subscribe failed: " + response.getError().getMessage());
        }
    }

    /**
     * 异步订阅任务更新
     *
     * @param request 订阅请求
     * @return 包含TaskResponse的CompletableFuture
     */
    public CompletableFuture<TaskResponse> asyncSubscribe(SubscribeRequest request) {
        return CompletableFuture.supplyAsync(() -> subscribe(request));
    }

    /**
     * 执行JSON-RPC请求
     *
     * @param request     请求对象
     * @param resultClass 结果类型
     * @param <T>         结果类型参数
     * @param <R>         请求参数类型
     * @return JSON-RPC响应
     */
    private <T, R> JsonRpcResponse<T> executeRpcRequest(JsonRpcRequest<R> request, Class<T> resultClass) {
        CompletableFuture<JsonRpcResponse<T>> future = new CompletableFuture<>();
        try {
            HttpRest post = httpClient.post("/");
            configureRequest(post);
            byte[] body = JSON.toJSONBytes(request);
            post.body(b -> b.write(body)).onSuccess(response -> {
                if (response.statusCode() == HttpStatus.OK.value()) {
                    try {
                        JsonRpcResponse<T> rpcResponse = JSON.parseObject(response.body(), new TypeReference<JsonRpcResponse<T>>() {});
                        future.complete(rpcResponse);
                    } catch (Exception e) {
                        future.completeExceptionally(new A2AException("Failed to parse response", e));
                    }
                } else {
                    future.completeExceptionally(new A2AException("RPC request failed: " + response.statusCode() + " - " + response.body()));
                }
            }).onFailure(throwable -> {
                future.completeExceptionally(new A2AException("RPC request failed", throwable));
            }).submit();
        } catch (Exception e) {
            throw new A2AException("RPC request failed", e);
        }
        try {
            return future.get();
        } catch (Exception e) {
            throw new A2AException("RPC request failed", e);
        }
    }

    /**
     * 创建JSON-RPC请求
     *
     * @param method 方法名
     * @param params 参数
     * @param <T>    参数类型
     * @return JSON-RPC请求对象
     */
    private <T> JsonRpcRequest<T> createJsonRpcRequest(String method, T params) {
        JsonRpcRequest<T> request = new JsonRpcRequest<>();
        request.setId(UUID.randomUUID().toString());
        request.setMethod(method);
        request.setParams(params);
        return request;
    }

    /**
     * 配置HTTP请求（添加认证头等）
     *
     * @param request HTTP请求对象
     */
    private void configureRequest(HttpRest request) {
        request.header(header -> {
            // 添加认证头
            if (options.getApiKey() != null) {
                header.set(HeaderName.AUTHORIZATION, "ApiKey " + options.getApiKey());
            } else if (options.getBearerToken() != null) {
                header.set(HeaderName.AUTHORIZATION, "Bearer " + options.getBearerToken());
            }

            // 添加Content-Type
            header.setContentType(HeaderValue.ContentType.APPLICATION_JSON);

            // 添加自定义请求头
            if (options.getHeaders() != null) {
                options.getHeaders().forEach((key, value) -> header.set(key, value.toString()));
            }
        });
    }
}
