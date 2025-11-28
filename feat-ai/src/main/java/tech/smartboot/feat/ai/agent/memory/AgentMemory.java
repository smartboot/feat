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

import java.util.List;

/**
 * Agent记忆接口
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface AgentMemory {

    /**
     * 添加记忆
     *
     * @param memory 记忆对象
     */
    void addMemory(Memory memory);

    /**
     * 获取所有记忆
     *
     * @return 记忆列表
     */
    List<Memory> getMemories();

    /**
     * 根据重要性获取记忆
     *
     * @param minImportance 最小重要性阈值
     * @return 符合条件的记忆列表
     */
    List<Memory> getMemoriesByImportance(double minImportance);

    /**
     * 根据时间范围获取记忆
     *
     * @param startTime 开始时间戳
     * @param endTime   结束时间戳
     * @return 时间范围内的记忆列表
     */
    List<Memory> getMemoriesByTimeRange(long startTime, long endTime);

    /**
     * 清空记忆
     */
    void clear();

    /**
     * 获取记忆总数
     *
     * @return 记忆总数
     */
    int size();
    
    /**
     * 根据关键词检索相关记忆
     *
     * @param keywords 关键词列表
     * @return 相关记忆列表
     */
    List<Memory> getMemoriesByKeywords(List<String> keywords);
    
    /**
     * 获取最近添加的记忆
     *
     * @param count 数量
     * @return 最近记忆列表
     */
    List<Memory> getRecentMemories(int count);
    
    /**
     * 根据内容相似度检索记忆
     *
     * @param query 查询内容
     * @param threshold 相似度阈值 (0-1)
     * @return 相似记忆列表
     */
    List<Memory> getMemoriesBySimilarity(String query, double threshold);
}