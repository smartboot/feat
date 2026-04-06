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

import java.util.function.Consumer;

/**
 * 内存记忆配置选项 - 配置InMemoryMemory的行为参数
 * <p>
 * 提供内存存储的记忆系统配置，包括存储容量限制、
 * 检索策略、消息过滤等参数。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see InMemoryMemory 内存记忆实现
 */
public class MemoryOptions {

    private static final Logger logger = LoggerFactory.getLogger(MemoryOptions.class.getName());

    /**
     * 最大存储消息数量
     * <p>
     * 当消息数量超过此限制时，会按照淘汰策略移除旧消息。
     * 设置为0表示无限制。
     * </p>
     */
    private int maxMessages = 100;

    /**
     * 默认检索返回数量
     */
    private int defaultTopK = 5;

    /**
     * 检索相似度阈值
     * <p>
     * 范围0.0-1.0，低于此阈值的记忆将被过滤。
     * 值越高表示要求越严格。
     * </p>
     */
    private double similarityThreshold = 0.5;

    /**
     * 是否启用消息去重
     * <p>
     * 启用后会自动合并内容相似的消息。
     * </p>
     */
    private boolean enableDeduplication = false;

    /**
     * 去重相似度阈值
     */
    private double deduplicationThreshold = 0.95;

    /**
     * 会话隔离模式
     * <p>
     * true表示只检索同一会话的记忆，
     * false表示可以跨会话检索。
     * </p>
     */
    private boolean sessionIsolation = true;

    /**
     * 是否包含系统消息
     */
    private boolean includeSystemMessages = false;

    /**
     * 是否包含思考过程
     */
    private boolean includeThoughts = false;

    /**
     * 消息重要性过滤阈值
     * <p>
     * 低于此阈值的消息在检索时会被忽略。
     * </p>
     */
    private double importanceThreshold = 0.0;

    /**
     * 设置最大存储消息数量
     *
     * @param maxMessages 最大消息数
     * @return 当前实例
     */
    public MemoryOptions maxMessages(int maxMessages) {
        this.maxMessages = Math.max(0, maxMessages);
        logger.info("设置最大存储消息数量: {}", maxMessages);
        return this;
    }

    /**
     * 设置默认检索返回数量
     *
     * @param topK 返回数量
     * @return 当前实例
     */
    public MemoryOptions defaultTopK(int topK) {
        this.defaultTopK = Math.max(1, topK);
        return this;
    }

    /**
     * 设置检索相似度阈值
     *
     * @param threshold 阈值（0.0-1.0）
     * @return 当前实例
     */
    public MemoryOptions similarityThreshold(double threshold) {
        this.similarityThreshold = Math.max(0.0, Math.min(1.0, threshold));
        return this;
    }

    /**
     * 启用消息去重
     *
     * @return 当前实例
     */
    public MemoryOptions enableDeduplication() {
        this.enableDeduplication = true;
        return this;
    }

    /**
     * 设置去重阈值
     *
     * @param threshold 阈值
     * @return 当前实例
     */
    public MemoryOptions deduplicationThreshold(double threshold) {
        this.deduplicationThreshold = threshold;
        return this;
    }

    /**
     * 禁用会话隔离，允许跨会话检索
     *
     * @return 当前实例
     */
    public MemoryOptions disableSessionIsolation() {
        this.sessionIsolation = false;
        return this;
    }

    /**
     * 包含系统消息
     *
     * @return 当前实例
     */
    public MemoryOptions includeSystemMessages() {
        this.includeSystemMessages = true;
        return this;
    }

    /**
     * 包含思考过程
     *
     * @return 当前实例
     */
    public MemoryOptions includeThoughts() {
        this.includeThoughts = true;
        return this;
    }

    /**
     * 设置重要性过滤阈值
     *
     * @param threshold 阈值
     * @return 当前实例
     */
    public MemoryOptions importanceThreshold(double threshold) {
        this.importanceThreshold = threshold;
        return this;
    }

    // ==================== Getter方法 ====================

    public int getMaxMessages() {
        return maxMessages;
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public boolean isEnableDeduplication() {
        return enableDeduplication;
    }

    public double getDeduplicationThreshold() {
        return deduplicationThreshold;
    }

    public boolean isSessionIsolation() {
        return sessionIsolation;
    }

    public boolean isIncludeSystemMessages() {
        return includeSystemMessages;
    }

    public boolean isIncludeThoughts() {
        return includeThoughts;
    }

    public double getImportanceThreshold() {
        return importanceThreshold;
    }
}
