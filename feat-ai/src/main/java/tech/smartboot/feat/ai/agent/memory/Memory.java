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

/**
 * 记忆接口
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface Memory {
    
    /**
     * 获取记忆内容
     *
     * @return 记忆内容
     */
    String getContent();
    
    /**
     * 获取记忆创建时间戳
     *
     * @return 创建时间戳
     */
    long getTimestamp();
    
    /**
     * 获取记忆重要性评分
     *
     * @return 重要性评分 (0-1)
     */
    double getImportance();
}