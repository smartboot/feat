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
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 4/22/25
 */
public final class AsyncResponse {
    private final CompletableFuture<RestResult> future = new CompletableFuture<>();

    public CompletableFuture<RestResult> getFuture() {
        return future;
    }

    public void complete(RestResult result) {
        future.complete(result);
    }

    public void complete() {
        complete(null);
    }
}
