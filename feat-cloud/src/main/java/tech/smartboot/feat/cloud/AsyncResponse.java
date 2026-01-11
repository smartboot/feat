/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import java.util.concurrent.CompletableFuture;

/**
 * 异步响应处理类
 * <p>
 * 该类用于处理HTTP请求的异步响应，通过CompletableFuture机制实现非阻塞的响应处理。
 * 当异步操作完成时，会自动将结果转换为JSON格式并发送给客户端。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 4/22/25
 */
public final class AsyncResponse {
    /**
     * 用于处理异步响应结果的CompletableFuture对象
     * <p>
     * 该对象封装了异步操作的结果，当操作完成时会触发响应处理流程。
     * </p>
     */
    private final CompletableFuture<RestResult> future = new CompletableFuture<>();

    /**
     * 获取异步响应的CompletableFuture对象
     *
     * @return 用于处理异步响应结果的CompletableFuture对象
     */
    public CompletableFuture<RestResult> getFuture() {
        return future;
    }

    /**
     * 完成异步响应处理，设置响应结果
     * <p>
     * 当异步操作完成后，调用此方法设置响应结果，触发响应处理流程。
     * </p>
     *
     * @param result 响应结果对象
     */
    public AsyncResponse complete(RestResult result) {
        future.complete(result);
        return this;
    }

    /**
     * 完成异步响应处理，不设置响应结果
     * <p>
     * 当异步操作完成后但不需要返回特定结果时，调用此方法完成响应处理。
     * </p>
     */
    public AsyncResponse complete() {
        complete(null);
        return this;
    }
}