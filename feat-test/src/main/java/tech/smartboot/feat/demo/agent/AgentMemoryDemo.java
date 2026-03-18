/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.agent;

import tech.smartboot.feat.ai.agent.ReActAgent;
import tech.smartboot.feat.ai.agent.memory.MemoryMessage;
import tech.smartboot.feat.ai.agent.memory.MemoryRole;
import tech.smartboot.feat.ai.chat.entity.Message;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * FeatAgent Memory功能对比演示
 * 
 * 【文档类型】教程
 * 【目的】演示有Memory和无Memory的Agent在多轮对话中的区别
 * 【前置条件】已配置AI模型（如GiteeAI）
 * 【验证方式】运行main方法，观察两种模式下Agent对上下文问题的回答差异
 * 
 * @author Feat Team
 * @version v1.0.0
 */
public class AgentMemoryDemo {

    /**
     * 模拟用户对话历史
     */
    private static final List<String> CONVERSATION_HISTORY = Arrays.asList(
        "你好，我叫张三，是一名Java开发工程师",
        "我喜欢使用Spring Boot开发微服务应用",
        "请问根据我刚才说的，我擅长什么技术栈？",
        "你能记住我的名字吗？"
    );

    public static void main(String[] args) throws Exception {
        System.out.println("======================================================");
        System.out.println("  FeatAgent Memory功能对比演示");
        System.out.println("======================================================\n");

        // 演示1: 无Memory的Agent
        demonstrateWithoutMemory();
        
        System.out.println("\n=====================================================\n");
        
        // 演示2: 有Memory的Agent
        demonstrateWithMemory();
        
        System.out.println("\n======================================================");
        System.out.println("  演示完成");
        System.out.println("======================================================");
    }

    /**
     * 演示无Memory的Agent
     * 
     * 每次对话都是独立的，Agent无法记住之前的上下文
     */
    private static void demonstrateWithoutMemory() throws Exception {
        System.out.println("【模式一】无Memory的Agent");
        System.out.println("-------------------------------------------------------");
        System.out.println("特点：每次execute都是独立的，Agent没有记忆能力\n");
        
        // 创建无Memory的Agent
        ReActAgent agentWithoutMemory = new ReActAgent(opts -> {
            opts.disableMemory();  // 显式禁用记忆
        });
        
        // 模拟多轮对话
        for (int i = 0; i < CONVERSATION_HISTORY.size(); i++) {
            String userInput = CONVERSATION_HISTORY.get(i);
            System.out.println("用户: " + userInput);
            
            // 注意：无Memory时，每次只传递当前消息
            List<Message> messages = Arrays.asList(Message.ofUser(userInput));
            
            try {
                CompletableFuture<String> future = agentWithoutMemory.execute(messages);
                String response = future.get();
                System.out.println("Agent: " + truncate(response, 200));
            } catch (Exception e) {
                System.out.println("Agent: [调用失败 - " + e.getMessage() + "]");
            }
            System.out.println();
        }
        
        System.out.println("观察：Agent无法记住用户之前的自我介绍");
    }

    /**
     * 演示有Memory的Agent
     * 
     * Agent会自动存储对话历史，并在后续查询中检索相关记忆
     */
    private static void demonstrateWithMemory() throws Exception {
        System.out.println("【模式二】有Memory的Agent");
        System.out.println("-------------------------------------------------------");
        System.out.println("特点：Agent自动存储和检索历史对话\n");
        
        // 创建有Memory的Agent
        final String sessionId = "demo_session_001";
        ReActAgent agentWithMemory = new ReActAgent(opts -> {
            opts.memory(memOpts -> {
                memOpts.maxMessages(100)        // 最大存储100条消息
                       .defaultTopK(5);        // 检索时返回最多5条相关记忆
            });
            opts.sessionId(sessionId);          // 设置会话ID
        });
        
        // 手动添加一些历史记忆（模拟之前的对话）
        // 在实际应用中，这些会由Agent自动存储
        System.out.println("[系统] 预加载历史记忆到Agent...\n");
        
        // 模拟多轮对话
        for (int i = 0; i < CONVERSATION_HISTORY.size(); i++) {
            String userInput = CONVERSATION_HISTORY.get(i);
            System.out.println("用户: " + userInput);
            
            // 构建消息列表
            List<Message> messages = Arrays.asList(Message.ofUser(userInput));
            
            try {
                CompletableFuture<String> future = agentWithMemory.execute(messages);
                String response = future.get();
                System.out.println("Agent: " + truncate(response, 200));
                
                // 模拟存储到Memory（实际由Agent自动完成）
                storeToMemory(agentWithMemory, userInput, response);
                
            } catch (Exception e) {
                System.out.println("Agent: [调用失败 - " + e.getMessage() + "]");
            }
            System.out.println();
        }
        
        // 展示记忆内容
        demonstrateMemoryContent(agentWithMemory);
    }

    /**
     * 模拟将对话存储到Memory
     */
    private static void storeToMemory(ReActAgent agent, String userInput, String response) {
        if (agent.options().getMemory() != null) {
            // 存储用户消息
            MemoryMessage userMessage = MemoryMessage.ofUser(userInput);
            userMessage.setSessionId(agent.options().getSessionId());
            agent.options().getMemory().add(userMessage);
            
            // 存储AI回复
            MemoryMessage assistantMessage = MemoryMessage.ofAssistant(response);
            assistantMessage.setSessionId(agent.options().getSessionId());
            agent.options().getMemory().add(assistantMessage);
        }
    }

    /**
     * 演示Memory的内容检索
     */
    private static void demonstrateMemoryContent(ReActAgent agent) {
        System.out.println("\n【Memory检索演示】");
        System.out.println("-------------------------------------------------------");
        
        if (agent.options().getMemory() == null) {
            System.out.println("Memory未配置");
            return;
        }
        
        // 搜索与"技术栈"相关的记忆
        System.out.println("搜索关键词: \"技术栈\"");
        List<MemoryMessage> results = agent.options().getMemory().search("技术栈", 3);
        System.out.println("找到 " + results.size() + " 条相关记忆:");
        for (MemoryMessage msg : results) {
            System.out.println("  [" + msg.getRole().getDisplayName() + "]: " + 
                             truncate(msg.getContent(), 80));
        }
        
        System.out.println("\n搜索关键词: \"名字\"");
        results = agent.options().getMemory().search("名字", 3);
        System.out.println("找到 " + results.size() + " 条相关记忆:");
        for (MemoryMessage msg : results) {
            System.out.println("  [" + msg.getRole().getDisplayName() + "]: " + 
                             truncate(msg.getContent(), 80));
        }
        
        // 显示最近的消息
        System.out.println("\n最近添加的 3 条记忆:");
        List<MemoryMessage> recent = agent.options().getMemory().getRecent(3);
        for (MemoryMessage msg : recent) {
            System.out.println("  [" + msg.getRole().getDisplayName() + "]: " + 
                             truncate(msg.getContent(), 80));
        }
        
        System.out.println("\n观察：Agent可以通过Memory检索到之前的对话内容");
    }

    /**
     * 截断长文本
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
