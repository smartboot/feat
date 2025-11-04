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

import java.util.concurrent.CompletableFuture;

/**
 * 拦截器接口，用于在请求处理过程中执行自定义逻辑
 * <p>
 * 拦截器可以在请求到达目标处理器之前或之后执行特定的业务逻辑，
 * 如权限验证、日志记录、性能监控等。通过实现该接口，开发者可以
 * 自定义拦截逻辑，并将其集成到请求处理链中。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface Interceptor {

    /**
     * 拦截并处理请求
     * <p>
     * 在请求处理链中执行拦截逻辑。实现类可以通过 [chain.proceed()](file:///Users/zhengjw22mac123/IdeaProjects/feat/feat-core/src/main/java/tech/smartboot/feat/router/Interceptor.java#L25-L25)
     * 方法将请求传递给下一个拦截器或最终的目标处理器。
     * </p>
     *
     * @param context             请求上下文，包含请求和响应的相关信息
     * @param completableFuture   异步完成回调，用于标记当前拦截器处理完成
     * @param chain               拦截器链，用于继续执行后续的拦截器或目标处理器
     * @throws Throwable          处理过程中可能抛出的异常
     */
    void intercept(Context context, CompletableFuture<Void> completableFuture, Chain chain) throws Throwable;


}