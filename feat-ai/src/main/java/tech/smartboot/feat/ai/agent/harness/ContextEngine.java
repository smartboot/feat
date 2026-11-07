/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import tech.smartboot.feat.ai.chat.entity.Message;

import java.util.List;

/**
 * 上下文工程：决定"这一轮模型能看到什么"。
 * <p>
 * 核心原则：**压缩只影响视图，不修改历史**。原始消息始终留在 {@link SessionStore} 与内存历史里，
 * 因此任何一次折叠都可回看、可对比、可回放。
 * </p>
 * <p>
 * P0 只需要滑动窗口；P1 接入折叠摘要与工具输出引用化后，
 * 长任务（50 步以上）的 token 增长应从平方级降到近似线性。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public interface ContextEngine {

    /**
     * 模型窗口上限（token），用于计算压缩阈值
     */
    int windowTokens();

    /**
     * 组装本次发送给模型的消息视图
     */
    List<Message> assemble(StepContext context);

    /**
     * 是否需要压缩
     *
     * @param messages       当前历史
     * @param reservedTokens 需要为模型输出预留的 token
     */
    default boolean needCompact(List<Message> messages, int reservedTokens) {
        int estimated = estimate(messages);
        return estimated + reservedTokens > (int) (windowTokens() * compactRatio());
    }

    /**
     * 是否需要压缩（携带上下文）。
     * <p>
     * 这个重载存在的意义：{@link #needCompact(List, int)} 判断的是**全量历史**，
     * 而"窗口已满"与"历史已满"是两件事。对于会把历史折叠成摘要的引擎（如
     * {@code StablePrefixContextEngine}），历史天然一直在增长，用历史当判据会导致
     * **每一步都压缩一次**——等于每步都把前缀缓存清空，比不压缩更贵。
     * 因此这类引擎必须重写本方法，改用"组装后的窗口"作为判据。
     * </p>
     */
    default boolean needCompact(StepContext context, int reservedTokens) {
        return needCompact(context.getMessages(), reservedTokens);
    }

    /**
     * 压缩：返回新的消息列表（不修改入参）
     *
     * @param targetTokens 目标 token 上限
     */
    List<Message> compact(List<Message> messages, int targetTokens);

    /**
     * 压缩（携带上下文）。
     * <p>
     * 只给 messages 时，实现只能做"删减"；拿到 {@link StepContext} 后，
     * 才有机会用结构化的 {@link Step} 历史生成**摘要**而不是直接丢弃。
     * 默认实现退化为按消息列表压缩，保证老实现不受影响。
     * </p>
     */
    default List<Message> compact(StepContext context, int targetTokens) {
        return compact(context.getMessages(), targetTokens);
    }

    /**
     * 运行结束通知：供有会话内状态的实现清理缓存，避免长跑进程里按会话累积。
     */
    default void onRunFinished(String sessionId) {
    }

    /**
     * 压缩触发水位，默认 0.7
     */
    default double compactRatio() {
        return 0.7d;
    }

    /**
     * 估算消息 token 数
     */
    default int estimate(List<Message> messages) {
        return TokenEstimator.heuristic().estimate(messages);
    }
}
