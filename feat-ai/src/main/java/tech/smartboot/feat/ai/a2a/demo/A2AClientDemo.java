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

import tech.smartboot.feat.ai.a2a.client.A2AClient;
import tech.smartboot.feat.ai.a2a.model.AgentCard;
import tech.smartboot.feat.ai.a2a.model.Message;
import tech.smartboot.feat.ai.a2a.model.SendTaskRequest;
import tech.smartboot.feat.ai.a2a.model.Skill;
import tech.smartboot.feat.ai.a2a.model.Task;
import tech.smartboot.feat.ai.a2a.model.TaskCancelRequest;
import tech.smartboot.feat.ai.a2a.model.TaskQueryRequest;
import tech.smartboot.feat.ai.a2a.model.TaskResponse;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * A2A 客户端演示程序
 *
 * <p>演示如何使用 A2AClient 与 A2A 服务器进行交互。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>发现智能体（获取 AgentCard）</li>
 *   <li>发送任务</li>
 *   <li>查询任务状态</li>
 *   <li>取消任务</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class A2AClientDemo {
    private static final Logger logger = LoggerFactory.getLogger(A2AClientDemo.class);

    public static void main(String[] args) throws Exception {
        // 创建 A2A 客户端
        A2AClient client = A2AClient.create(options -> {
            options.setEndpoint("http://localhost:8080");
            options.setConnectTimeout(10000);
            options.setDebug(true);
        });

        logger.info("=== A2A Client Demo ===");
        logger.info("Connecting to: http://localhost:8080");

        // 步骤1: 获取 AgentCard
        logger.info("\n[Step 1] Getting AgentCard...");
        AgentCard agentCard = client.getAgentCard();
        printAgentCard(agentCard);

        // 步骤2: 发送任务 - 问候
        logger.info("\n[Step 2] Sending greeting task...");
        SendTaskRequest greetingRequest = createTaskRequest("Hello, A2A Agent!");
        TaskResponse greetingResponse = client.sendTask(greetingRequest);
        printTaskResponse(greetingResponse);

        // 步骤3: 查询任务状态
        logger.info("\n[Step 3] Querying task status...");
        TaskQueryRequest queryRequest = new TaskQueryRequest();
        queryRequest.setId(greetingResponse.getId());
        Task task = client.getTask(queryRequest);
        printTask(task);

        // 步骤4: 发送任务 - 请求帮助
        logger.info("\n[Step 4] Sending help request task...");
        SendTaskRequest helpRequest = createTaskRequest("Can you help me with translation?");
        TaskResponse helpResponse = client.sendTask(helpRequest);
        printTaskResponse(helpResponse);

        // 步骤5: 发送任务 - 请求代码
        logger.info("\n[Step 5] Sending code generation task...");
        SendTaskRequest codeRequest = createTaskRequest("I need help with code generation.");
        TaskResponse codeResponse = client.sendTask(codeRequest);
        printTaskResponse(codeResponse);

        // 步骤6: 取消任务示例
        logger.info("\n[Step 6] Creating and cancelling a task...");
        SendTaskRequest cancelRequest = createTaskRequest("This task will be cancelled");
        TaskResponse cancelResponse = client.sendTask(cancelRequest);
        logger.info("Task created: {}", cancelResponse.getId());
        
        TaskCancelRequest taskCancelRequest = new TaskCancelRequest();
        taskCancelRequest.setId(cancelResponse.getId());
        TaskResponse cancelledResponse = client.cancelTask(taskCancelRequest);
        logger.info("Task cancelled. Status: {}", cancelledResponse.getStatus());

        logger.info("\n=== A2A Client Demo Completed ===");
    }

    /**
     * 打印 AgentCard 信息
     */
    private static void printAgentCard(AgentCard agentCard) {
        logger.info("AgentCard Information:");
        logger.info("  Name: {}", agentCard.getName());
        logger.info("  Description: {}", agentCard.getDescription());
        logger.info("  Version: {}", agentCard.getVersion());
        logger.info("  URL: {}", agentCard.getUrl());
        
        if (agentCard.getProvider() != null) {
            logger.info("  Provider: {}", agentCard.getProvider().getOrganization());
        }
        
        if (agentCard.getCapabilities() != null) {
            logger.info("  Capabilities:");
            logger.info("    - Streaming: {}", agentCard.getCapabilities().isStreaming());
            logger.info("    - Push Notifications: {}", agentCard.getCapabilities().isPushNotifications());
            logger.info("    - State Transitioning: {}", agentCard.getCapabilities().isStateTransitioning());
        }
        
        if (agentCard.getSkills() != null && !agentCard.getSkills().isEmpty()) {
            logger.info("  Skills:");
            for (Skill skill : agentCard.getSkills()) {
                logger.info("    - {} ({}): {}", skill.getName(), skill.getId(), skill.getDescription());
            }
        }
        
        if (agentCard.getInputModes() != null && !agentCard.getInputModes().isEmpty()) {
            logger.info("  Input Modes: {}", agentCard.getInputModes());
        }
        
        if (agentCard.getOutputModes() != null && !agentCard.getOutputModes().isEmpty()) {
            logger.info("  Output Modes: {}", agentCard.getOutputModes());
        }
    }

    /**
     * 打印任务响应
     */
    private static void printTaskResponse(TaskResponse response) {
        logger.info("Task Response:");
        logger.info("  Task ID: {}", response.getId());
        
        if (response.getStatus() != null) {
            logger.info("  State: {}", response.getStatus());
        }

        if (response.getArtifacts() != null && !response.getArtifacts().isEmpty()) {
            logger.info("  Artifacts: {}", response.getArtifacts().size());
        }
    }

    /**
     * 打印任务详情
     */
    private static void printTask(Task task) {
        logger.info("Task Details:");
        logger.info("  Task ID: {}", task.getId());
        logger.info("  Session ID: {}", task.getSessionId());
        logger.info("  State: {}", task.getState());
        logger.info("  Created At: {}", task.getCreatedAt());
        logger.info("  Updated At: {}", task.getUpdatedAt());
        
        if (task.getHistory() != null && !task.getHistory().isEmpty()) {
            logger.info("  Message History ({} messages):", task.getHistory().size());
            for (int i = 0; i < task.getHistory().size(); i++) {
                Message message = task.getHistory().get(i);
                String text = message.getParts() != null && !message.getParts().isEmpty() 
                    ? message.getParts().get(0).getText() 
                    : "";
                logger.info("    [{}] {}: {}", i + 1, message.getRole(), 
                    text.length() > 50 ? text.substring(0, 50) + "..." : text);
            }
        }
    }

    /**
     * 创建任务请求
     */
    private static SendTaskRequest createTaskRequest(String text) {
        SendTaskRequest request = new SendTaskRequest();
        
        Message message = Message.userMessage(text);
        request.setMessage(Arrays.asList(message));
        
        return request;
    }
}
