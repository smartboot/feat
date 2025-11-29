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
 * <p>
 * 该枚举定义了AI代理在其生命周期中可能处于的各种状态，
 * 便于外部监控和控制代理的行为。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public enum AgentState {

    /**
     * 空闲状态，表示Agent当前没有任务正在执行，可以接受新的任务
     * <p>
     * 在此状态下，Agent已完成之前的所有任务或者刚刚初始化完成，
     * 正在等待新的指令或任务分配。
     * </p>
     */
    IDLE,

    /**
     * 运行状态，表示Agent正在处理任务的主逻辑阶段
     * <p>
     * 在此状态下，Agent正在进行推理、分析问题、制定计划等核心处理工作。
     * 这通常是Agent处理任务的主要阶段。
     * </p>
     */
    RUNNING,

    /**
     * 完成状态，表示Agent已经成功完成了当前的任务
     * <p>
     * 在此状态下，Agent已成功解决问题或完成指定任务，
     * 并准备输出最终结果或等待下一个任务。
     * </p>
     */
    FINISHED,

    /**
     * 错误状态，表示Agent在执行过程中遇到了异常或错误
     * <p>
     * 在此状态下，Agent由于某种原因无法继续正常执行任务，
     * 如网络异常、参数错误、资源不足等情况。
     * </p>
     */
    ERROR,

    /**
     * 工具执行状态，表示Agent正在调用外部工具或执行特定的功能模块
     * <p>
     * 在此状态下，Agent暂停主逻辑处理，转而执行某个特定的工具函数，
     * 如文件操作、网络搜索、数学计算等。
     * </p>
     */
    TOOL_EXECUTION
}