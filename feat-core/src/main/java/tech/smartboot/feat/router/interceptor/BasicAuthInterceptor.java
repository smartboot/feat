/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router.interceptor;

import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.router.Chain;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.Interceptor;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/18/25
 */
public class BasicAuthInterceptor implements Interceptor {
    private final String basic;

    public BasicAuthInterceptor(String username, String password) {
        basic = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    @Override
    public void intercept(Context context, CompletableFuture<Void> completableFuture, Chain chain) throws Throwable {
        String clientBasic = context.Request.getHeader(HeaderName.AUTHORIZATION);
        if (FeatUtils.equals(clientBasic, this.basic)) {
            chain.proceed(context, completableFuture);
        } else {
            HttpResponse response = context.Response;
            response.setHeader(HeaderName.WWW_AUTHENTICATE, "Basic realm=\"feat\"");
            response.setHttpStatus(HttpStatus.UNAUTHORIZED);
            response.close();
        }
    }
}
