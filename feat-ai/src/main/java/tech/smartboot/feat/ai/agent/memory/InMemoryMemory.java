/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.memory;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 基于内存的记忆实现 - 适用于开发和测试场景
 * <p>
 * 该实现将所有记忆消息存储在JVM内存中，具有以下特点：
 * 1. 访问速度快，适合高频读写场景
 * 2. 支持基于关键词的简单检索
 * 3. 支持基于时间窗口的记忆淘汰
 * 4. 应用重启后数据丢失
 * </p>
 * <p>
 * 实现细节：
 * - 使用CopyOnWriteArrayList保证线程安全
 * - 支持会话隔离，不同会话的记忆分开存储
 * - 使用LRU策略进行消息淘汰
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see Memory 记忆接口
 * @see MemoryOptions 配置选项
 */
public class InMemoryMemory implements Memory {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryMemory.class.getName());

    /**
     * 全局记忆存储 - 按会话ID分组
     */
    private final Map<String, List<MemoryMessage>> sessionMemories;

    /**
     * 配置选项
     */
    private final MemoryOptions options;

    /**
     * 消息计数器
     */
    private final AtomicLong messageCounter;

    /**
     * 构造方法
     *
     * @param consumer 配置选项消费者
     */
    InMemoryMemory(Consumer<MemoryOptions> consumer) {
        this.options = new MemoryOptions();
        consumer.accept(this.options);
        this.sessionMemories = new ConcurrentHashMap<>();
        this.messageCounter = new AtomicLong(0);
        logger.info("初始化InMemoryMemory，配置: maxMessages={}, topK={}",
                options.getMaxMessages(), options.getDefaultTopK());
    }

    @Override
    public void add(MemoryMessage message) {
        add(Collections.singletonList(message));
    }

    @Override
    public void add(List<MemoryMessage> messages) {
        for (MemoryMessage message : messages) {
            String sessionId = message.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = "default";
                message.setSessionId(sessionId);
            }

            // 获取或创建会话的记忆列表
            List<MemoryMessage> sessionMemory = sessionMemories.computeIfAbsent(
                    sessionId, k -> new CopyOnWriteArrayList<>());

            // 应用角色默认重要性
            if (message.getImportance() <= 0 && message.getRole() != null) {
                message.setImportance(message.getRole().getDefaultImportance());
            }

            synchronized (sessionMemory) {
                // 检查是否需要去重
                if (options.isEnableDeduplication()) {
                    boolean isDuplicate = sessionMemory.stream()
                            .anyMatch(m -> isSimilar(m, message, options.getDeduplicationThreshold()));
                    if (isDuplicate) {
                        logger.debug("发现重复消息，跳过添加: {}", message.getContent().substring(0,
                                Math.min(50, message.getContent().length())));
                        continue;
                    }
                }

                // 添加消息
                sessionMemory.add(message);
                messageCounter.incrementAndGet();

                // 执行消息淘汰
                evictIfNecessary(sessionMemory);
            }
        }

        logger.debug("添加 {} 条记忆消息，当前总消息数: {}", messages.size(), messageCounter.get());
    }

    @Override
    public List<MemoryMessage> search(String query) {
        return search(query, options.getDefaultTopK());
    }

    @Override
    public List<MemoryMessage> search(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return getRecent(topK);
        }

        List<MemoryMessage> allMessages = new ArrayList<>();

        // 收集所有相关消息
        for (Map.Entry<String, List<MemoryMessage>> entry : sessionMemories.entrySet()) {
            String sessionId = entry.getKey();
            List<MemoryMessage> sessionMemory = entry.getValue();

            for (MemoryMessage message : sessionMemory) {
                // 应用过滤器
                if (!shouldIncludeMessage(message)) {
                    continue;
                }

                // 计算相似度分数
                double score = calculateRelevanceScore(message, query);
                if (score >= options.getSimilarityThreshold()) {
                    allMessages.add(message);
                }
            }
        }

        // 按相关性和重要性排序
        allMessages.sort((m1, m2) -> {
            double score1 = calculateRelevanceScore(m1, query) * m1.getImportance();
            double score2 = calculateRelevanceScore(m2, query) * m2.getImportance();
            return Double.compare(score2, score1);
        });

        // 返回前topK个
        return allMessages.stream()
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemoryMessage> getRecent(int limit) {
        List<MemoryMessage> allMessages = new ArrayList<>();

        for (List<MemoryMessage> sessionMemory : sessionMemories.values()) {
            for (MemoryMessage message : sessionMemory) {
                if (shouldIncludeMessage(message)) {
                    allMessages.add(message);
                }
            }
        }

        // 按时间戳倒序排序
        allMessages.sort(Comparator.comparingLong(MemoryMessage::getTimestamp).reversed());

        return allMessages.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        sessionMemories.clear();
        messageCounter.set(0);
        logger.info("清除所有记忆消息");
    }

    @Override
    public void clear(String sessionId) {
        List<MemoryMessage> removed = sessionMemories.remove(sessionId);
        if (removed != null) {
            messageCounter.addAndGet(-removed.size());
            logger.info("清除会话 '{}' 的记忆消息，共 {} 条", sessionId, removed.size());
        }
    }

    @Override
    public long size() {
        return messageCounter.get();
    }

    /**
     * 获取指定会话的记忆数量
     *
     * @param sessionId 会话ID
     * @return 记忆数量
     */
    public long size(String sessionId) {
        List<MemoryMessage> sessionMemory = sessionMemories.get(sessionId);
        return sessionMemory != null ? sessionMemory.size() : 0;
    }

    /**
     * 获取指定会话的所有记忆
     *
     * @param sessionId 会话ID
     * @return 记忆列表
     */
    public List<MemoryMessage> getSessionMemories(String sessionId) {
        List<MemoryMessage> sessionMemory = sessionMemories.get(sessionId);
        return sessionMemory != null ? new ArrayList<>(sessionMemory) : Collections.emptyList();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 判断是否需要淘汰消息
     *
     * @param sessionMemory 会话记忆列表
     */
    private void evictIfNecessary(List<MemoryMessage> sessionMemory) {
        int maxMessages = options.getMaxMessages();
        if (maxMessages <= 0) {
            return; // 无限制
        }

        // 按时间戳排序，移除最旧的消息
        while (sessionMemory.size() > maxMessages) {
            MemoryMessage removed = sessionMemory.remove(0);
            messageCounter.decrementAndGet();
            logger.debug("淘汰旧消息: {}", removed.getId());
        }
    }

    /**
     * 判断消息是否应该被包含在检索结果中
     *
     * @param message 消息
     * @return true表示应该包含
     */
    private boolean shouldIncludeMessage(MemoryMessage message) {
        // 重要性过滤
        if (message.getImportance() < options.getImportanceThreshold()) {
            return false;
        }

        // 角色类型过滤
        if (!options.isIncludeSystemMessages() && message.getRole() == MemoryRole.SYSTEM) {
            return false;
        }

        if (!options.isIncludeThoughts() && message.getRole() == MemoryRole.THOUGHT) {
            return false;
        }

        return true;
    }

    /**
     * 计算消息与查询的相关性分数
     * <p>
     * 使用简单的关键词匹配算法，可以替换为更复杂的算法
     * </p>
     *
     * @param message 消息
     * @param query   查询
     * @return 相关性分数 (0.0-1.0)
     */
    private double calculateRelevanceScore(MemoryMessage message, String query) {
        String content = message.getContent().toLowerCase();
        String queryLower = query.toLowerCase();

        // 提取查询关键词
        String[] queryWords = queryLower.split("\\s+");
        if (queryWords.length == 0) {
            return 0.0;
        }

        // 计算匹配的关键词数量
        int matchCount = 0;
        for (String word : queryWords) {
            if (word.length() > 2 && content.contains(word)) {
                matchCount++;
            }
        }

        // 计算分数
        double wordMatchScore = (double) matchCount / queryWords.length;

        // 完全匹配加分
        double exactMatchBonus = content.contains(queryLower) ? 0.3 : 0;

        // 时间衰减因子 - 越新的消息分数越高
        long age = System.currentTimeMillis() - message.getTimestamp();
        double timeDecay = Math.exp(-age / (24 * 60 * 60 * 1000.0)); // 按天衰减

        return Math.min(1.0, wordMatchScore + exactMatchBonus) * (0.5 + 0.5 * timeDecay);
    }

    /**
     * 判断两条消息是否相似（用于去重）
     *
     * @param m1        消息1
     * @param m2        消息2
     * @param threshold 相似度阈值
     * @return true表示相似
     */
    private boolean isSimilar(MemoryMessage m1, MemoryMessage m2, double threshold) {
        // 角色不同则不相似
        if (m1.getRole() != m2.getRole()) {
            return false;
        }

        // 计算内容相似度（简单实现）
        String content1 = m1.getContent().toLowerCase();
        String content2 = m2.getContent().toLowerCase();

        if (content1.equals(content2)) {
            return true;
        }

        // 计算Jaccard相似度
        String[] words1 = content1.split("\\s+");
        String[] words2 = content2.split("\\s+");

        int intersection = 0;
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.equals(w2) && w1.length() > 2) {
                    intersection++;
                    break;
                }
            }
        }

        int union = words1.length + words2.length - intersection;
        double similarity = union > 0 ? (double) intersection / union : 0;

        return similarity >= threshold;
    }
}
