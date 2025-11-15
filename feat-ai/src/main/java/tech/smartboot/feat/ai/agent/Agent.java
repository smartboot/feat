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

import tech.smartboot.feat.ai.agent.tool.ToolExecutor;

/**
 * AI Agent核心接口定义
 * 提供统一的Agent操作接口，基于ReAct（Reasoning + Acting）范式
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
     * 添加工具执行器
     *
     * @param executor 工具执行器
     */
    void addTool(ToolExecutor executor);


    /**
     * 获取Agent状态
     *
     * @return Agent状态
     */
    AgentState getState();

    /**
     * 设置最大迭代次数
     *
     * @param maxIterations 最大迭代次数
     */
    void setMaxIterations(int maxIterations);
}