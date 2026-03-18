/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.memory;

import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.vector.VectorStore;

/**
 * 向量记忆配置选项 - 配置VectorMemory的行为参数
 * <p>
 * 提供基于向量存储的记忆系统配置，需要配合EmbeddingModel和VectorStore使用。
 * 支持语义检索、持久化存储等高级功能。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see VectorMemory 向量记忆实现
 * @see EmbeddingModel 嵌入模型
 * @see VectorStore 向量存储
 */
public class VectorMemoryOptions extends MemoryOptions {

    /**
     * 向量存储实例
     */
    private VectorStore vectorStore;

    /**
     * 嵌入模型实例
     */
    private EmbeddingModel embeddingModel;

    /**
     * 记忆集合名称
     */
    private String collectionName = "agent_memory";

    /**
     * 向量维度
     */
    private int vectorDimension = 1024;

    /**
     * 当前会话ID，用于会话隔离
     */
    private String sessionId;

    /**
     * 设置向量存储
     *
     * @param vectorStore 向量存储实例
     * @return 当前实例
     */
    public VectorMemoryOptions vectorStore(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        return this;
    }

    /**
     * 设置嵌入模型
     *
     * @param embeddingModel 嵌入模型实例
     * @return 当前实例
     */
    public VectorMemoryOptions embeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        return this;
    }

    /**
     * 设置集合名称
     *
     * @param collectionName 集合名称
     * @return 当前实例
     */
    public VectorMemoryOptions collectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }

    /**
     * 设置向量维度
     *
     * @param dimension 维度
     * @return 当前实例
     */
    public VectorMemoryOptions vectorDimension(int dimension) {
        this.vectorDimension = dimension;
        return this;
    }

    /**
     * 设置会话ID，用于会话隔离
     *
     * @param sessionId 会话ID
     * @return 当前实例
     */
    public VectorMemoryOptions sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    // ==================== Getter方法 ====================

    public VectorStore getVectorStore() {
        return vectorStore;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public int getVectorDimension() {
        return vectorDimension;
    }

    public String getSessionId() {
        return sessionId;
    }
}
