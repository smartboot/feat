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

import tech.smartboot.feat.ai.a2a.model.AgentCapability;
import tech.smartboot.feat.ai.a2a.model.AgentCard;
import tech.smartboot.feat.ai.a2a.model.Artifact;
import tech.smartboot.feat.ai.a2a.model.Message;
import tech.smartboot.feat.ai.a2a.model.Part;
import tech.smartboot.feat.ai.a2a.model.Provider;
import tech.smartboot.feat.ai.a2a.model.PushNotificationConfig;
import tech.smartboot.feat.ai.a2a.model.SendTaskRequest;
import tech.smartboot.feat.ai.a2a.model.SetPushNotificationRequest;
import tech.smartboot.feat.ai.a2a.model.Skill;
import tech.smartboot.feat.ai.a2a.model.SubscribeRequest;
import tech.smartboot.feat.ai.a2a.model.Task;
import tech.smartboot.feat.ai.a2a.model.TaskCancelRequest;
import tech.smartboot.feat.ai.a2a.model.TaskQueryRequest;
import tech.smartboot.feat.ai.a2a.model.TaskResponse;
import tech.smartboot.feat.ai.a2a.enums.AgentSkill;
import tech.smartboot.feat.ai.a2a.enums.Role;
import tech.smartboot.feat.ai.a2a.enums.TaskState;
import tech.smartboot.feat.ai.a2a.server.A2AServer;
import tech.smartboot.feat.ai.a2a.server.A2AServerOptions;
import tech.smartboot.feat.ai.a2a.server.TaskHandler;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A2A 服务器演示程序
 *
 * <p>演示如何使用 A2AServer 构建一个完整的 A2A 智能体服务。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>发布 AgentCard 供客户端发现</li>
 *   <li>处理任务创建、查询、取消等操作</li>
 *   <li>支持流式响应和推送通知</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class A2AServerDemo {
    private static final Logger logger = LoggerFactory.getLogger(A2AServerDemo.class);

    public static void main(String[] args) throws Exception {
        // 创建路由器
        Router router = new Router();

        // 创建 A2A 服务器
        A2AServer a2aServer = new A2AServer(options -> {
            // 配置智能体基本信息
            options.setAgentName("SmartBoot A2A Agent");
            options.setAgentDescription("A demo A2A agent built with Feat framework");
            options.setAgentVersion("1.0.0");
            options.setAgentUrl("http://localhost:8080");

            // 配置提供商信息
            Provider provider = new Provider();
            provider.setOrganization("SmartBoot");
            provider.setUrl("https://smartboot.tech");
            options.setProvider(provider);

            // 配置智能体能力
            AgentCapability capability = new AgentCapability();
            capability.setStreaming(true);
            capability.setPushNotifications(true);
            capability.setStateTransitioning(true);
            options.setCapabilities(capability);

            // 添加支持技能
            Skill textGenerationSkill = new Skill();
            textGenerationSkill.setId(AgentSkill.TEXT_GENERATION.getId());
            textGenerationSkill.setName(AgentSkill.TEXT_GENERATION.getName());
            textGenerationSkill.setDescription(AgentSkill.TEXT_GENERATION.getDescription());
            textGenerationSkill.setInputModes(Arrays.asList("text/plain"));
            textGenerationSkill.setOutputModes(Arrays.asList("text/plain"));
            options.addSkill(textGenerationSkill);

            // 添加翻译技能
            Skill translationSkill = new Skill();
            translationSkill.setId(AgentSkill.TRANSLATION.getId());
            translationSkill.setName(AgentSkill.TRANSLATION.getName());
            translationSkill.setDescription(AgentSkill.TRANSLATION.getDescription());
            translationSkill.setInputModes(Arrays.asList("text/plain"));
            translationSkill.setOutputModes(Arrays.asList("text/plain"));
            options.addSkill(translationSkill);

            // 配置输入输出模式
            options.addInputMode("text/plain");
            options.addInputMode("application/json");
            options.addOutputMode("text/plain");
            options.addOutputMode("application/json");
        });

        // 注册任务处理器
        a2aServer.taskHandler(new DemoTaskHandler());

        // 注册到路由
        a2aServer.register(router);

        // 创建 HTTP 服务器
        HttpServer bootstrap = new HttpServer();
        bootstrap.httpHandler(router);
        logger.info("A2A Server started on port 8080");
        logger.info("AgentCard available at: http://localhost:8080/.well-known/agent.json");
        logger.info("JSON-RPC endpoint: http://localhost:8080/");

        // 保持运行
        System.in.read();
    }

    /**
     * 演示任务处理器实现
     */
    private static class DemoTaskHandler implements TaskHandler {
        private final Map<String, Task> taskStore = new ConcurrentHashMap<>();

        @Override
        public TaskResponse sendTask(SendTaskRequest request) {
            logger.info("Received task request");

            // 创建新任务
            Task task = new Task();
            String taskId = request.getId() != null ? request.getId() : UUID.randomUUID().toString();
            task.setId(taskId);
            task.setSessionId(request.getSessionId());
            task.setMetadata(request.getMetadata());
            task.setState(TaskState.WORKING);

            // 添加用户消息到历史
            if (request.getMessage() != null) {
                for (Message message : request.getMessage()) {
                    task.addMessage(message);
                }
            }

            // 处理任务逻辑
            processTask(task);

            // 存储任务
            taskStore.put(taskId, task);

            // 构建响应
            TaskResponse response = new TaskResponse();
            response.setId(taskId);
            response.setStatus(task.getState());

            logger.info("Task {} completed with state: {}", taskId, task.getState());
            return response;
        }

        @Override
        public Task getTask(TaskQueryRequest request) {
            logger.info("Querying task: {}", request.getId());
            Task task = taskStore.get(request.getId());
            if (task == null) {
                logger.warn("Task not found: {}", request.getId());
                return null;
            }
            return task;
        }

        @Override
        public TaskResponse cancelTask(TaskCancelRequest request) {
            logger.info("Cancelling task: {}", request.getId());
            Task task = taskStore.get(request.getId());
            if (task != null) {
                task.markCanceled();
            }

            TaskResponse response = new TaskResponse();
            response.setId(request.getId());
            response.setStatus(TaskState.CANCELED);
            return response;
        }

        @Override
        public TaskResponse subscribe(SubscribeRequest request) {
            logger.info("Subscribing to task: {}", request.getId());
            Task task = taskStore.get(request.getId());
            
            TaskResponse response = new TaskResponse();
            response.setId(request.getId());
            if (task != null) {
                response.setStatus(task.getState());
            }
            return response;
        }

        @Override
        public PushNotificationConfig setPushNotification(SetPushNotificationRequest request) {
            logger.info("Setting push notification for task: {}", request.getId());
            // 这里可以实现推送通知逻辑
            return request.getPushNotification();
        }

        /**
         * 处理任务逻辑
         */
        private void processTask(Task task) {
            // 获取最后一条用户消息
            String userMessage = getLastUserMessage(task);
            if (userMessage == null) {
                userMessage = "";
            }

            // 模拟任务处理
            String responseText = generateResponse(userMessage);

            // 添加智能体响应到历史
            task.addMessage(createAgentMessage(responseText));

            // 完成任务
            task.markCompleted();
            
            // 创建产出物
            Artifact artifact = new Artifact();
            artifact.setName("response");
            artifact.setDescription("Task response");
            artifact.addPart(Part.text(responseText));
            artifact.setComplete(true);
            
            // 注意：TaskResponse 包含 artifact，但 Task 不直接包含
        }

        /**
         * 生成响应文本
         */
        private String generateResponse(String userMessage) {
            userMessage = userMessage.toLowerCase();
            
            if (userMessage.contains("hello") || userMessage.contains("hi")) {
                return "Hello! I'm the SmartBoot A2A Agent. How can I help you today?";
            } else if (userMessage.contains("translate")) {
                return "I can help you with translations. Please provide the text you want to translate.";
            } else if (userMessage.contains("code")) {
                return "I can help you generate code. What kind of code do you need?";
            } else if (userMessage.contains("help")) {
                return "I support text generation, translation, and code assistance. What would you like to do?";
            } else {
                return "I received your message: \"" + userMessage + "\". How can I assist you further?";
            }
        }

        /**
         * 获取最后一条用户消息
         */
        private String getLastUserMessage(Task task) {
            if (task.getHistory() == null || task.getHistory().isEmpty()) {
                return null;
            }
            // 从后往前找用户消息
            for (int i = task.getHistory().size() - 1; i >= 0; i--) {
                Message message = task.getHistory().get(i);
                if (message.getRole() == Role.USER && message.getParts() != null && !message.getParts().isEmpty()) {
                    Part part = message.getParts().get(0);
                    if (part.getText() != null) {
                        return part.getText();
                    }
                }
            }
            return null;
        }

        /**
         * 创建智能体响应消息
         */
        private Message createAgentMessage(String text) {
            Message message = new Message();
            message.setRole(Role.AGENT);
            message.addPart(Part.text(text));
            return message;
        }

        /**
         * 创建任务状态响应消息
         */
        private Message createAgentResponse(Task task) {
            return createAgentMessage("Task is being processed. State: " + task.getState());
        }
    }
}
