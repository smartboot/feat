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

import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.vector.Document;
import tech.smartboot.feat.ai.vector.SearchRequest;
import tech.smartboot.feat.ai.vector.VectorStore;
import tech.smartboot.feat.ai.vector.expression.Expression;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 基于向量的记忆实现 - 支持语义检索和长期存储
 * <p>
 * 该实现利用EmbeddingModel和VectorStore提供高级记忆功能：
 * 1. 语义检索：基于向量相似度找到语义相关的记忆
 * 2. 持久化存储：支持将记忆持久化到外部向量数据库
 * 3. 可扩展性：支持大规模记忆存储
 * </p>
 * <p>
 * 实现细节：
 * - 使用EmbeddingModel将文本转换为向量
 * - 使用VectorStore进行向量存储和相似度搜索
 * - 支持会话隔离和元数据过滤
 * - 提供缓存机制优化性能
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see Memory 记忆接口
 * @see VectorMemoryOptions 配置选项
 * @see EmbeddingModel 嵌入模型
 * @see VectorStore 向量存储
 */
public class VectorMemory implements Memory {

    private static final Logger logger = LoggerFactory.getLogger(VectorMemory.class.getName());

    /**
     * 配置选项
     */
    private final VectorMemoryOptions options;

    /**
     * 内存缓存 - 用于快速访问
     */
    private final Map<String, MemoryMessage> memoryCache;

    /**
     * 消息计数器
     */
    private final AtomicLong messageCounter;

    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;

    /**
     * 构造方法
     *
     * @param consumer 配置选项消费者
     */
    VectorMemory(Consumer<VectorMemoryOptions> consumer) {
        this.options = new VectorMemoryOptions();
        consumer.accept(this.options);
        this.memoryCache = new ConcurrentHashMap<>();
        this.messageCounter = new AtomicLong(0);

        // 验证必要配置
        validateConfiguration();

        logger.info("初始化VectorMemory，配置: collection={}, vectorDimension={}",
                options.getCollectionName(), options.getVectorDimension());
    }

    /**
     * 验证配置
     */
    private void validateConfiguration() {
        if (options.getVectorStore() == null) {
            throw new IllegalStateException("VectorStore必须配置");
        }
        if (options.getEmbeddingModel() == null) {
            throw new IllegalStateException("EmbeddingModel必须配置");
        }
    }

    /**
     * 懒初始化
     */
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    // 可以从VectorStore加载已有记忆
                    loadFromVectorStore();
                    initialized = true;
                }
            }
        }
    }

    /**
     * 从VectorStore加载记忆
     */
    private void loadFromVectorStore() {
        // 这里可以实现从VectorStore加载历史记忆的逻辑
        logger.info("从VectorStore加载记忆数据");
    }

    @Override
    public void add(MemoryMessage message) {
        add(Collections.singletonList(message));
    }

    @Override
    public void add(List<MemoryMessage> messages) {
        ensureInitialized();

        List<Document> documents = new ArrayList<>();

        for (MemoryMessage message : messages) {
            // 设置会话ID：优先使用消息中的sessionId，否则使用options中的sessionId
            String sessionId = message.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = options.getSessionId() != null ? options.getSessionId() : "default";
                message.setSessionId(sessionId);
            }

            // 应用角色默认重要性
            if (message.getImportance() <= 0 && message.getRole() != null) {
                message.setImportance(message.getRole().getDefaultImportance());
            }

            // 生成向量嵌入
            float[] embedding = generateEmbedding(message);
            message.setEmbedding(embedding);

            // 创建Document
            Document doc = createDocument(message);
            documents.add(doc);

            // 添加到缓存
            memoryCache.put(message.getId(), message);
            messageCounter.incrementAndGet();

            logger.debug("添加记忆消息: id={}, role={}, session={}",
                    message.getId(), message.getRole(), sessionId);
        }

        // 批量存储到VectorStore
        if (!documents.isEmpty()) {
            options.getVectorStore().add(documents);
            logger.info("批量添加 {} 条记忆到VectorStore", documents.size());
        }
    }

    @Override
    public List<MemoryMessage> search(String query) {
        return search(query, options.getDefaultTopK());
    }

    @Override
    public List<MemoryMessage> search(String query, int topK) {
        ensureInitialized();

        if (query == null || query.trim().isEmpty()) {
            return getRecent(topK);
        }

        try {
            // 构建搜索请求
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setQuery(query);
            
            // 应用会话隔离过滤
            if (options.isSessionIsolation() && options.getSessionId() != null) {
                searchRequest.setExpression(Expression.of("sessionId").eq(options.getSessionId()));
            }

            // 执行向量搜索
            List<Document> results = options.getVectorStore().similaritySearch(searchRequest);

            // 转换为MemoryMessage并过滤
            List<MemoryMessage> messages = results.stream()
                    .map(this::convertToMemoryMessage)
                    .filter(this::shouldIncludeMessage)
                    .limit(topK)
                    .collect(Collectors.toList());

            logger.debug("向量搜索查询: '{}', 返回 {} 条结果", query, messages.size());
            return messages;

        } catch (Exception e) {
            logger.error("向量搜索失败，回退到基于缓存的搜索", e);
            return searchFromCache(query, topK);
        }
    }

    @Override
    public List<MemoryMessage> getRecent(int limit) {
        ensureInitialized();

        // 从缓存中获取最近的记忆
        return memoryCache.values().stream()
                .filter(this::shouldIncludeMessage)
                .sorted((m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        // 清空缓存
        memoryCache.clear();
        messageCounter.set(0);

        // 清空VectorStore
        // 这里需要根据具体的VectorStore实现来删除数据
        logger.info("清除所有向量记忆");
    }

    @Override
    public void clear(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }
        
        // 移除缓存中指定会话的记忆
        List<String> toRemove = memoryCache.values().stream()
                .filter(m -> sessionId.equals(m.getSessionId()))
                .map(MemoryMessage::getId)
                .collect(Collectors.toList());

        toRemove.forEach(memoryCache::remove);
        messageCounter.addAndGet(-toRemove.size());

        // 从VectorStore中删除指定会话的数据
        try {
            options.getVectorStore().delete(Expression.of("sessionId").eq(sessionId));
            logger.info("清除会话 '{}' 的向量记忆，共 {} 条", sessionId, toRemove.size());
        } catch (Exception e) {
            logger.warn("从VectorStore清除会话 '{}' 记忆失败: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public long size() {
        return messageCounter.get();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成向量嵌入
     *
     * @param message 记忆消息
     * @return 向量表示
     */
    private float[] generateEmbedding(MemoryMessage message) {
        try {
            String text = message.getContent();
            if (message.getRole() != null) {
                // 将角色信息也包含在嵌入中
                text = message.getRole().getDisplayName() + ": " + text;
            }
            return options.getEmbeddingModel().embed(text);
        } catch (Exception e) {
            logger.error("生成嵌入向量失败", e);
            return new float[options.getVectorDimension()];
        }
    }

    /**
     * 创建Document对象
     *
     * @param message 记忆消息
     * @return Document
     */
    private Document createDocument(MemoryMessage message) {
        Document doc = new Document();
        doc.setId(message.getId());
        doc.setDocument(message.getContent());

        // 设置元数据
        Map<String, String> metadata = new HashMap<>();
        if (message.getSessionId() != null) {
            metadata.put("sessionId", message.getSessionId());
        }
        if (message.getRole() != null) {
            metadata.put("role", message.getRole().name());
        }
        metadata.put("timestamp", String.valueOf(message.getTimestamp()));
        metadata.put("importance", String.valueOf(message.getImportance()));

        // 合并原有元数据
        if (message.getMetadata() != null) {
            metadata.putAll(message.getMetadata());
        }
        doc.setMetadata(metadata);

        return doc;
    }

    /**
     * 将Document转换为MemoryMessage
     *
     * @param doc Document
     * @return MemoryMessage
     */
    private MemoryMessage convertToMemoryMessage(Document doc) {
        // 先尝试从缓存获取
        MemoryMessage cached = memoryCache.get(doc.getId());
        if (cached != null) {
            return cached;
        }

        // 从Document重建
        MemoryMessage message = new MemoryMessage();
        message.setId(doc.getId());
        message.setContent(doc.getDocument());

        Map<String, String> metadata = doc.getMetadata();
        if (metadata != null) {
            message.setSessionId(metadata.get("sessionId"));

            String roleStr = metadata.get("role");
            if (roleStr != null) {
                try {
                    message.setRole(MemoryRole.valueOf(roleStr));
                } catch (IllegalArgumentException e) {
                    logger.warn("未知的角色类型: {}", roleStr);
                }
            }

            String timestampStr = metadata.get("timestamp");
            if (timestampStr != null) {
                try {
                    message.setTimestamp(Long.parseLong(timestampStr));
                } catch (NumberFormatException e) {
                    logger.warn("无效的时间戳: {}", timestampStr);
                }
            }

            String importanceStr = metadata.get("importance");
            if (importanceStr != null) {
                try {
                    message.setImportance(Double.parseDouble(importanceStr));
                } catch (NumberFormatException e) {
                    logger.warn("无效的重要性值: {}", importanceStr);
                }
            }

            message.setMetadata(new HashMap<>(metadata));
        }

        return message;
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

        // 会话隔离已在search方法中通过Expression实现
        // 这里保留二次验证逻辑用于缓存检索场景
        if (options.isSessionIsolation() && options.getSessionId() != null) {
            if (!options.getSessionId().equals(message.getSessionId())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 从缓存中搜索（向量搜索失败时的回退方案）
     *
     * @param query 查询
     * @param topK  返回数量
     * @return 记忆列表
     */
    private List<MemoryMessage> searchFromCache(String query, int topK) {
        String queryLower = query.toLowerCase();

        return memoryCache.values().stream()
                .filter(m -> m.getContent().toLowerCase().contains(queryLower))
                .sorted((m1, m2) -> {
                    // 按内容匹配度和时间排序
                    double score1 = calculateRelevanceScore(m1, query);
                    double score2 = calculateRelevanceScore(m2, query);
                    return Double.compare(score2, score1);
                })
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 计算相关性分数
     *
     * @param message 消息
     * @param query   查询
     * @return 分数
     */
    private double calculateRelevanceScore(MemoryMessage message, String query) {
        String content = message.getContent().toLowerCase();
        String queryLower = query.toLowerCase();

        // 完全匹配
        if (content.equals(queryLower)) {
            return 1.0;
        }

        // 包含匹配
        if (content.contains(queryLower)) {
            return 0.8;
        }

        // 关键词匹配
        String[] words = queryLower.split("\\s+");
        int matchCount = 0;
        for (String word : words) {
            if (word.length() > 2 && content.contains(word)) {
                matchCount++;
            }
        }

        return (double) matchCount / words.length * 0.5;
    }


    /**
     * 获取配置选项
     *
     * @return VectorMemoryOptions
     */
    public VectorMemoryOptions options() {
        return options;
    }

    /**
     * 设置会话ID
     * <p>
     * 用于动态更新会话隔离标识。应在配置阶段设置，
     * 不建议在已有记忆数据后更改。
     * </p>
     *
     * @param sessionId 会话ID
     */
    public void setSessionId(String sessionId) {
        this.options.sessionId(sessionId);
        logger.info("更新VectorMemory会话ID: {}", sessionId);
    }
}
