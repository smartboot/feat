/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 拦截器链，用于管理和执行一系列拦截器
 * <p>
 * Chain类负责按顺序执行注册的拦截器，并在所有拦截器执行完毕后，
 * 调用最终的目标处理器。它维护了当前执行的位置索引，确保每个拦截器
 * 只被执行一次，并提供了中断机制来控制处理流程。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Chain {
    /**
     * 当前拦截器在链中的索引位置
     */
    private int index;
    
    /**
     * 拦截器列表，按顺序存储所有注册的拦截器
     */
    private final List<Interceptor> interceptors;
    
    /**
     * 最终的目标处理器，当所有拦截器执行完毕后调用
     */
    private final RouterHandler handler;
    
    /**
     * 中断标志，表示处理流程是否被中断
     */
    private boolean isInterrupted = true;

    /**
     * 构造一个拦截器链
     *
     * @param handler      目标处理器
     * @param interceptors 拦截器列表
     */
    Chain(RouterHandler handler, List<Interceptor> interceptors) {
        this.interceptors = interceptors;
        this.handler = handler;
    }

    /**
     * 继续执行拦截器链中的下一个拦截器
     * <p>
     * 如果还有未执行的拦截器，则执行下一个拦截器；
     * 如果所有拦截器都已执行完毕，则调用最终的目标处理器。
     * </p>
     *
     * @param context             请求上下文
     * @param completableFuture   异步完成回调
     * @throws Throwable          处理过程中可能抛出的异常
     */
    public void proceed(Context context, CompletableFuture<Void> completableFuture) throws Throwable {
        if (index < interceptors.size()) {
            interceptors.get(index++).intercept(context, completableFuture, this);
        } else {
            isInterrupted = false;
            handler.handle(context, completableFuture);
        }
    }

    /**
     * 检查处理流程是否被中断
     *
     * @return 如果处理流程被中断返回true，否则返回false
     */
    public boolean isInterrupted() {
        return isInterrupted;
    }
}