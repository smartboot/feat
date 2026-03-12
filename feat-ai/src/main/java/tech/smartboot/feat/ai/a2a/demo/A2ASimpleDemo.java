/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.demo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.a2a.model.AgentCard;
import tech.smartboot.feat.ai.a2a.model.JsonRpcRequest;
import tech.smartboot.feat.ai.a2a.model.JsonRpcResponse;
import tech.smartboot.feat.ai.a2a.model.Message;
import tech.smartboot.feat.ai.a2a.model.SendTaskRequest;
import tech.smartboot.feat.ai.a2a.model.Task;
import tech.smartboot.feat.ai.a2a.model.TaskCancelRequest;
import tech.smartboot.feat.ai.a2a.model.TaskQueryRequest;
import tech.smartboot.feat.ai.a2a.model.TaskResponse;
import tech.smartboot.feat.ai.a2a.enums.Role;
import tech.smartboot.feat.ai.a2a.enums.TaskState;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.RouterHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A2A 简化演示程序
 *
 * <p>一个简单的 A2A 服务器演示，用于测试和验证 A2A 协议实现。</p>
 *
 * <p>使用方式：</p>
 * <ol>
 *   <li>运行 main 方法启动服务器</li>
 *   <li>访问 http://localhost:8080/.well-known/agent.json 获取 AgentCard</li>
 *   <li>发送 JSON-RPC 请求到 http://localhost:8080/</li>
 * </ol>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class A2ASimpleDemo {
    private static final Logger logger = LoggerFactory.getLogger(A2ASimpleDemo.class);
    private static final Map<String, Task> taskStore = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        // 创建路由器
        Router router = new Router();

        // 注册 AgentCard 端点
        router.route("/.well-known/agent.json", new RouterHandler() {
            @Override
            public void handle(Context ctx, CompletableFuture<Void> completableFuture) throws Throwable {
                handleAgentCard(ctx);
                completableFuture.complete(null);
            }

            @Override
            public void handle(Context ctx) throws Exception {
                handleAgentCard(ctx);
            }
        });

        // 注册 JSON-RPC 端点
        router.route("/", new RouterHandler() {
            @Override
            public void handle(Context ctx, CompletableFuture<Void> completableFuture) throws Throwable {
                handleJsonRpc(ctx, completableFuture);
            }

            @Override
            public void handle(Context ctx) throws Exception {
                CompletableFuture<Void> future = new CompletableFuture<>();
                handleJsonRpc(ctx, future);
                future.get();
            }
        });

        // 创建 HTTP 服务器
        HttpServer bootstrap = new HttpServer();
        bootstrap.options().debug(true);
        bootstrap.httpHandler(router);
        bootstrap.listen();

        logger.info("========================================");
        logger.info("A2A Server started successfully!");
        logger.info("AgentCard: http://localhost:8080/.well-known/agent.json");
        logger.info("JSON-RPC:  http://localhost:8080/");
        logger.info("========================================");
        logger.info("Press Enter to stop the server...");
        Thread.sleep(600000);
    }

    /**
     * 处理 AgentCard 请求
     */
    private static void handleAgentCard(Context ctx) throws IOException {
        AgentCard agentCard = createAgentCard();
        ctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
        ctx.Response.write(JSON.toJSONBytes(agentCard));
        logger.info("AgentCard requested");
    }

    /**
     * 处理 JSON-RPC 请求
     */
    private static void handleJsonRpc(Context ctx, CompletableFuture<Void> completableFuture) {
        try {
            if (!"POST".equalsIgnoreCase(ctx.Request.getMethod())) {
                ctx.Response.setHttpStatus(HttpStatus.METHOD_NOT_ALLOWED);
                ctx.Response.write("Method not allowed".getBytes());
                completableFuture.complete(null);
                return;
            }

            int contentLength = (int) ctx.Request.getContentLength();
            byte[] bytes = new byte[contentLength];
            ctx.Request.getInputStream().read(bytes);
            String body = new String(bytes);

            logger.info("JSON-RPC Request: {}", body);

            JsonRpcRequest<JSONObject> request = JSON.parseObject(body, 
                new com.alibaba.fastjson2.TypeReference<JsonRpcRequest<JSONObject>>() {});

            if (request.getMethod() == null) {
                sendError(ctx, request.getId(), -32600, "Method is required");
                completableFuture.complete(null);
                return;
            }

            Object result = handleMethod(request);
            sendSuccess(ctx, request.getId(), result);
            completableFuture.complete(null);

        } catch (Exception e) {
            logger.error("Error handling JSON-RPC request", e);
            try {
                sendError(ctx, null, -32603, "Internal error: " + e.getMessage());
            } catch (IOException ex) {
                logger.error("Failed to send error response", ex);
            }
            completableFuture.complete(null);
        }
    }

    /**
     * 处理具体的 JSON-RPC 方法
     */
    private static Object handleMethod(JsonRpcRequest<JSONObject> request) {
        String method = request.getMethod();
        JSONObject params = request.getParams();

        switch (method) {
            case "tasks/send":
                return handleSendTask(params);
            case "tasks/get":
                return handleGetTask(params);
            case "tasks/cancel":
                return handleCancelTask(params);
            default:
                throw new RuntimeException("Method not found: " + method);
        }
    }

    /**
     * 处理发送任务请求
     */
    private static TaskResponse handleSendTask(JSONObject params) {
        SendTaskRequest req = params.toJavaObject(SendTaskRequest.class);
        
        Task task = new Task();
        String taskId = req.getId() != null ? req.getId() : UUID.randomUUID().toString();
        task.setId(taskId);
        task.setSessionId(req.getSessionId());
        task.setState(TaskState.WORKING);

        // 添加用户消息
        if (req.getMessage() != null) {
            for (Message message : req.getMessage()) {
                task.addMessage(message);
            }
        }

        // 生成响应
        String userMessage = getLastUserMessage(task);
        String responseText = generateResponse(userMessage);
        task.addMessage(Message.agentMessage(responseText));
        task.markCompleted();

        taskStore.put(taskId, task);

        TaskResponse response = new TaskResponse();
        response.setId(taskId);
        response.setStatus(TaskState.COMPLETED);

        logger.info("Task {} created and completed", taskId);
        return response;
    }

    /**
     * 处理获取任务请求
     */
    private static Task handleGetTask(JSONObject params) {
        TaskQueryRequest req = params.toJavaObject(TaskQueryRequest.class);
        Task task = taskStore.get(req.getId());
        if (task == null) {
            throw new RuntimeException("Task not found: " + req.getId());
        }
        return task;
    }

    /**
     * 处理取消任务请求
     */
    private static TaskResponse handleCancelTask(JSONObject params) {
        TaskCancelRequest req = params.toJavaObject(TaskCancelRequest.class);
        Task task = taskStore.get(req.getId());
        if (task != null) {
            task.markCanceled();
        }

        TaskResponse response = new TaskResponse();
        response.setId(req.getId());
        response.setStatus(TaskState.CANCELED);
        return response;
    }

    /**
     * 发送成功的 JSON-RPC 响应
     */
    private static void sendSuccess(Context ctx, String id, Object result) throws IOException {
        JsonRpcResponse<Object> response = new JsonRpcResponse<>();
        response.setId(id);
        response.setResult(result);
        ctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
        ctx.Response.write(JSON.toJSONBytes(response));
    }

    /**
     * 发送错误的 JSON-RPC 响应
     */
    private static void sendError(Context ctx, String id, int code, String message) throws IOException {
        JsonRpcResponse<Object> response = JsonRpcResponse.error(id, code, message);
        ctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
        ctx.Response.setHttpStatus(HttpStatus.OK);
        ctx.Response.write(JSON.toJSONBytes(response));
    }

    /**
     * 创建 AgentCard
     */
    private static AgentCard createAgentCard() {
        AgentCard card = new AgentCard();
        card.setName("Simple A2A Agent");
        card.setDescription("A simple A2A agent for testing");
        card.setVersion("1.0.0");
        card.setUrl("http://localhost:8080");
        return card;
    }

    /**
     * 获取最后一条用户消息
     */
    private static String getLastUserMessage(Task task) {
        if (task.getHistory() == null || task.getHistory().isEmpty()) {
            return "";
        }
        for (int i = task.getHistory().size() - 1; i >= 0; i--) {
            Message message = task.getHistory().get(i);
            if (message.getRole() == Role.USER && message.getParts() != null && !message.getParts().isEmpty()) {
                return message.getParts().get(0).getText();
            }
        }
        return "";
    }

    /**
     * 生成响应文本
     */
    private static String generateResponse(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return "Hello! How can I help you?";
        }
        userMessage = userMessage.toLowerCase();
        
        if (userMessage.contains("hello") || userMessage.contains("hi")) {
            return "Hello! I'm the Simple A2A Agent. Nice to meet you!";
        } else if (userMessage.contains("help")) {
            return "I can respond to simple messages. Try saying 'hello' or ask me anything!";
        } else {
            return "I received your message: \"" + userMessage + "\". This is a simple A2A demo.";
        }
    }
}
