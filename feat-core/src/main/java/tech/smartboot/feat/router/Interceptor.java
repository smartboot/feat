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
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface Interceptor {

    /**
     * 拦截请求
     * @param context 上下文
     * @param completableFuture 完成回调
     * @param chain 链
     */
    void intercept(Context context, CompletableFuture<Void> completableFuture, Chain chain) throws Throwable;


}