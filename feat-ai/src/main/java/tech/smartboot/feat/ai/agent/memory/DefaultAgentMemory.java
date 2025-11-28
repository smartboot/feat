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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 默认Agent记忆实现
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.1
 */
public class DefaultAgentMemory implements AgentMemory {

    private final List<Memory> memories = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(DefaultAgentMemory.class.getName());

    // 记忆容量限制
    private static final int MAX_MEMORY_SIZE = 1000;

    @Override
    public void addMemory(Memory memory) {
        // 检查记忆容量，如果超过限制则移除最旧的记忆
        if (memories.size() >= MAX_MEMORY_SIZE) {
            Memory oldestMemory = memories.stream()
                    .min(Comparator.comparingLong(Memory::getTimestamp))
                    .orElse(null);
            if (oldestMemory != null) {
                memories.remove(oldestMemory);
                logger.info("记忆容量达到上限，移除最旧的记忆: " + oldestMemory.getContent().substring(0, Math.min(50, oldestMemory.getContent().length())));
            }
        }

        memories.add(memory);
        logger.info("添加新记忆，当前记忆数量: " + memories.size());
    }

    @Override
    public List<Memory> getMemories() {
        return new ArrayList<>(memories);
    }

    @Override
    public List<Memory> getMemoriesByImportance(double minImportance) {
        List<Memory> importantMemories = memories.stream()
                .filter(memory -> memory.getImportance() >= minImportance)
                .sorted(Comparator.comparingDouble(Memory::getImportance).reversed())
                .collect(Collectors.toList());

        logger.info("按重要性检索记忆，阈值: " + minImportance + ", 结果数量: " + importantMemories.size());
        return importantMemories;
    }

    @Override
    public List<Memory> getMemoriesByTimeRange(long startTime, long endTime) {
        List<Memory> timeRangeMemories = memories.stream()
                .filter(memory -> memory.getTimestamp() >= startTime && memory.getTimestamp() <= endTime)
                .sorted(Comparator.comparingLong(Memory::getTimestamp).reversed())
                .collect(Collectors.toList());

        logger.info("按时间范围检索记忆，从 " + startTime + " 到 " + endTime + ", 结果数量: " + timeRangeMemories.size());
        return timeRangeMemories;
    }

    @Override
    public void clear() {
        int size = memories.size();
        memories.clear();
        logger.info("清空所有记忆，之前数量: " + size);
    }

    @Override
    public int size() {
        return memories.size();
    }

    /**
     * 根据关键词检索相关记忆
     *
     * @param keywords 关键词列表
     * @return 相关记忆列表
     */
    public List<Memory> getMemoriesByKeywords(List<String> keywords) {
        List<Memory> relevantMemories = memories.stream()
                .filter(memory -> {
                    String content = memory.getContent().toLowerCase();
                    return keywords.stream().anyMatch(keyword -> content.contains(keyword.toLowerCase()));
                })
                .sorted(Comparator.comparingDouble(Memory::getImportance).reversed())
                .limit(10) // 限制返回数量
                .collect(Collectors.toList());

        logger.info("按关键词检索记忆，关键词: " + keywords + ", 结果数量: " + relevantMemories.size());
        return relevantMemories;
    }

    /**
     * 获取最近添加的记忆
     *
     * @param count 数量
     * @return 最近记忆列表
     */
    public List<Memory> getRecentMemories(int count) {
        List<Memory> recentMemories = memories.stream()
                .sorted(Comparator.comparingLong(Memory::getTimestamp).reversed())
                .limit(count)
                .collect(Collectors.toList());

        return recentMemories;
    }
}