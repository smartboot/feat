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
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
public class Chain {
    private int index;
    private final List<Interceptor> interceptors;
    private final RouterHandler handler;
    private boolean isInterrupted = true;

    Chain(RouterHandler handler, List<Interceptor> interceptors) {
        this.interceptors = interceptors;
        this.handler = handler;
    }

    public void proceed(Context context, CompletableFuture<Void> completableFuture) throws Throwable {
        if (index < interceptors.size()) {
            interceptors.get(index++).intercept(context, completableFuture, this);
        } else {
            isInterrupted = false;
            handler.handle(context, completableFuture);
        }
    }

    public boolean isInterrupted() {
        return isInterrupted;
    }
}