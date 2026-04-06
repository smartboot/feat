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

import java.util.List;
import java.util.function.Consumer;

/**
 * 记忆接口 - 定义Agent记忆系统的核心操作
 * <p>
 * 记忆系统是AI Agent的重要组成部分，用于存储和检索历史对话和交互信息。
 * 该接口提供了记忆的基本CRUD操作，支持多种存储后端实现。
 * </p>
 * <p>
 * 支持的记忆类型：
 * - 短期记忆：存储当前会话的最近消息
 * - 长期记忆：存储重要的历史对话和知识
 * - 语义记忆：基于向量相似度的记忆检索
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see MemoryMessage 记忆消息实体
 * @see InMemoryMemory 内存存储实现
 * @see VectorMemory 向量存储实现
 */
public interface Memory {

    /**
     * 创建基于内存的记忆存储实现
     * <p>
     * 适用于开发测试场景，数据存储在JVM内存中，
     * 应用重启后数据会丢失。
     * </p>
     *
     * @param consumer 配置选项
     * @return 内存记忆实现实例
     */
    static InMemoryMemory inMemory(Consumer<MemoryOptions> consumer) {
        return new InMemoryMemory(consumer);
    }

    /**
     * 创建基于向量的记忆存储实现
     * <p>
     * 适用于生产环境，支持语义检索和长期存储，
     * 需要配置VectorStore和EmbeddingModel。
     * </p>
     *
     * @param consumer 配置选项
     * @return 向量记忆实现实例
     */
    static VectorMemory vector(Consumer<VectorMemoryOptions> consumer) {
        return new VectorMemory(consumer);
    }

    /**
     * 添加一条记忆消息
     *
     * @param message 记忆消息
     */
    void add(MemoryMessage message);

    /**
     * 批量添加记忆消息
     *
     * @param messages 记忆消息列表
     */
    void add(List<MemoryMessage> messages);

    /**
     * 检索与查询相关的记忆消息
     * <p>
     * 根据查询内容返回最相关的历史记忆，
     * 返回的记忆数量由配置决定。
     * </p>
     *
     * @param query 查询内容
     * @return 相关的记忆消息列表
     */
    List<MemoryMessage> search(String query);

    /**
     * 检索与查询相关的记忆消息，指定返回数量
     *
     * @param query 查询内容
     * @param topK  返回的最大记忆数量
     * @return 相关的记忆消息列表
     */
    List<MemoryMessage> search(String query, int topK);

    /**
     * 获取最近的记忆消息
     *
     * @param limit 返回的最大数量
     * @return 最近的记忆消息列表
     */
    List<MemoryMessage> getRecent(int limit);

    /**
     * 清除所有记忆
     */
    void clear();

    /**
     * 删除指定会话的记忆
     *
     * @param sessionId 会话ID
     */
    void clear(String sessionId);

    /**
     * 获取记忆数量
     *
     * @return 当前存储的记忆总数
     */
    long size();
}
