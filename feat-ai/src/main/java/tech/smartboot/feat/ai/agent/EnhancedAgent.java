/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent;

import tech.smartboot.feat.ai.agent.memory.AgentMemory;
import tech.smartboot.feat.ai.agent.memory.Memory;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * 增强版Agent实现
 * 提供更智能的记忆管理和工具调用能力
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class EnhancedAgent extends AbstractAgent {
    
    private static final Logger logger = Logger.getLogger(EnhancedAgent.class.getName());
    
    public EnhancedAgent(String name, ChatModel chatModel, AgentMemory memory) {
        super(name, chatModel, memory);
    }
    
    public EnhancedAgent(AgentOptions options) {
        super(options.getName(), 
              tech.smartboot.feat.ai.FeatAI.chatModel(chatOptions -> chatOptions.model(options.getVendor())), 
              options.getMemory());
    }
    
    @Override
    public CompletableFuture<ResponseMessage> chat(String content, ChatOptions options) {
        // 使用默认选项或合并选项
        ChatOptions finalOptions = options != null ? options : ChatOptions.create().enableMemory();
        return super.chat(content, finalOptions);
    }
    
    @Override
    public void chatStream(String content, ChatOptions options, StreamResponseCallback callback) {
        // 使用默认选项或合并选项
        ChatOptions finalOptions = options != null ? options : ChatOptions.create().enableMemory();
        super.chatStream(content, finalOptions, callback);
    }
    
    /**
     * 智能对话 - 自动检索相关记忆并整合到对话中
     *
     * @param content 用户输入
     * @return 异步响应
     */
    public CompletableFuture<ResponseMessage> smartChat(String content) {
        return chat(content, ChatOptions.create().enableMemory().enableTools());
    }
    
    /**
     * 智能对话 - 自动检索相关记忆并整合到对话中
     *
     * @param content  用户输入
     * @param callback 回调函数
     */
    public void smartChat(String content, Consumer<ResponseMessage> callback) {
        chat(content, ChatOptions.create().enableMemory().enableTools(), response -> {
            callback.accept(response);
        });
    }
    
    /**
     * 智能流式对话
     *
     * @param content  用户输入
     * @param callback 流式回调
     */
    public void smartChatStream(String content, StreamResponseCallback callback) {
        chatStream(content, ChatOptions.create().enableMemory().enableTools(), callback);
    }
    
    /**
     * 从内容中提取关键词（简单实现）
     *
     * @param content 内容
     * @return 关键词列表
     */
    private List<String> extractKeywords(String content) {
        // 简单的关键词提取逻辑
        // 实际应用中可以使用更复杂的NLP技术
        String[] words = content.split("\\s+");
        List<String> keywords = new ArrayList<>();
        
        for (String word : words) {
            if (word.length() > 2 && !isCommonWord(word.toLowerCase())) {
                keywords.add(word.toLowerCase());
            }
        }
        
        // 限制关键词数量
        return keywords.subList(0, Math.min(5, keywords.size()));
    }
    
    /**
     * 判断是否为常见词
     *
     * @param word 单词
     * @return 是否为常见词
     */
    private boolean isCommonWord(String word) {
        String[] commonWords = {"the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"};
        for (String common : commonWords) {
            if (common.equals(word)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 添加结构化记忆
     *
     * @param content 记忆内容
     * @param importance 重要性评分
     */
    public void addStructuredMemory(String content, double importance) {
        Memory memory = new SimpleMemory(content, System.currentTimeMillis(), importance);
        addMemory(memory);
        logger.info("添加结构化记忆，重要性: " + importance);
    }
    
    /**
     * 清空所有记忆
     */
    public void clearAllMemories() {
        getMemory().clear();
        logger.info("清空所有Agent记忆");
    }
    
    /**
     * 获取记忆统计信息
     *
     * @return 统计信息字符串
     */
    public String getMemoryStats() {
        return "记忆总数: " + getMemory().size();
    }
    
    /**
     * 简单记忆实现
     */
    private static class SimpleMemory implements Memory {
        private final String content;
        private final long timestamp;
        private final double importance;
        
        public SimpleMemory(String content, long timestamp, double importance) {
            this.content = content;
            this.timestamp = timestamp;
            this.importance = importance;
        }
        
        @Override
        public String getContent() {
            return content;
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public double getImportance() {
            return importance;
        }
    }
}