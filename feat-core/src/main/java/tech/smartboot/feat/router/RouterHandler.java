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

import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 路由处理器接口，定义了处理HTTP请求的标准方法
 * <p>
 * RouterHandler接口提供了处理HTTP请求的统一入口，支持同步和异步两种处理模式。
 * 实现类需要提供具体的业务逻辑处理代码，处理请求参数、执行业务操作并生成响应结果。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface RouterHandler {
    /**
     * 请求头接收完成回调方法
     * <p>
     * 当HTTP请求头接收完成后调用此方法，可以在此方法中进行请求头相关的处理，
     * 如解析请求头参数、验证请求合法性等操作。
     * </p>
     *
     * @param request HTTP端点对象
     * @throws IOException IO异常
     */
    default void onHeaderComplete(HttpEndpoint request) throws IOException {
    }

    /**
     * 异步处理HTTP请求
     * <p>
     * 使用CompletableFuture实现异步处理，处理完成后需要调用completableFuture.complete()方法。
     * 默认实现会调用同步处理方法，并在处理完成后自动完成CompletableFuture。
     * </p>
     *
     * @param context             请求上下文对象
     * @param completableFuture   异步完成回调
     * @throws Throwable 处理过程中可能抛出的异常
     */
    default void handle(Context context, CompletableFuture<Void> completableFuture) throws Throwable {
        try {
            handle(context);
        } finally {
            completableFuture.complete(null);
        }
    }

    /**
     * 同步处理HTTP请求（核心处理方法）
     * <p>
     * 实现类需要重写此方法提供具体的业务逻辑处理代码。
     * 通过Context对象可以访问请求参数、会话信息和响应对象等。
     * </p>
     *
     * @param ctx 请求上下文对象
     * @throws Throwable 处理过程中可能抛出的异常
     */
    void handle(Context ctx) throws Throwable;

    /**
     * 断开 TCP 连接回调方法
     * <p>
     * 当客户端断开TCP连接时调用此方法，可以在此方法中进行资源清理等操作。
     * </p>
     *
     * @param context HTTP端点对象
     */
    default void onClose(HttpEndpoint context) {
    }
}