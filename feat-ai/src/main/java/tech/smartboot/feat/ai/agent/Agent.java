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
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;

import java.util.List;
import java.util.Map;

/**
 * AI Agent核心接口定义
 * 提供统一的Agent操作接口
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v2.0.0
 */
public interface Agent {

    /**
     * 获取Agent名称
     *
     * @return Agent名称
     */
    String getName();

    /**
     * 获取Agent描述
     *
     * @return Agent描述
     */
    String getDescription();

    /**
     * 获取Agent记忆
     *
     * @return AgentMemory实例
     */
    AgentMemory getMemory();

    /**
     * 执行流式对话（支持工具调用）
     *
     * @param input    用户输入内容
     * @param callback 流式响应回调
     */
    void execute(Map<String, String> input, StreamResponseCallback callback);

    /**
     * 添加工具执行器
     *
     * @param executor 工具执行器
     */
    void addTool(ToolExecutor executor);

    /**
     * 移除工具执行器
     *
     * @param toolName 工具名称
     */
    void removeTool(String toolName);

    /**
     * 添加记忆
     *
     * @param memory 记忆对象
     */
    void addMemory(Memory memory);

    /**
     * 清空对话历史
     */
    void clearHistory();

    /**
     * 获取对话历史
     *
     * @return 对话历史列表
     */
    List<Message> getHistory();
}