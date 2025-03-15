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

import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public final class BasicAuthRouterHandler implements RouterHandler {
    private final RouterHandler httpServerHandler;
    private final String basic;

    public BasicAuthRouterHandler(String username, String password, RouterHandler httpServerHandler) {
        this.httpServerHandler = httpServerHandler;
        basic = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    @Override
    public void onHeaderComplete(HttpEndpoint request) throws IOException {
        String clientBasic = request.getHeader(HeaderName.AUTHORIZATION);
        if (StringUtils.equals(clientBasic, this.basic)) {
            httpServerHandler.onHeaderComplete(request);
        } else {
            HttpResponse response = request.getResponse();
            response.setHeader(HeaderName.WWW_AUTHENTICATE, "Basic realm=\"feat\"");
            response.setHttpStatus(HttpStatus.UNAUTHORIZED);
            response.close();
        }
    }

    @Override
    public void onClose(HttpEndpoint request) {
        httpServerHandler.onClose(request);
    }

    @Override
    public void handle(Context request, CompletableFuture<Object> completableFuture) throws Throwable {
        httpServerHandler.handle(request, completableFuture);
    }

    @Override
    public void handle(Context request) throws Throwable {
        throw new UnsupportedOperationException();
    }
}
