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

/**
 * Agent状态枚举，用于表示AI代理的当前运行状态
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public enum AgentState {

    /**
     * 空闲状态，表示Agent当前没有任务正在执行，可以接受新的任务
     */
    IDLE,

    /**
     * 运行状态，表示Agent正在处理任务的主逻辑阶段
     */
    RUNNING,

    /**
     * 完成状态，表示Agent已经成功完成了当前的任务
     */
    FINISHED,

    /**
     * 错误状态，表示Agent在执行过程中遇到了异常或错误
     */
    ERROR,

    /**
     * 工具执行状态，表示Agent正在调用外部工具或执行特定的功能模块
     */
    TOOL_EXECUTION
}