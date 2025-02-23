/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: BasicAuthServerHandle.java
 * Date: 2021-02-23
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.router;

import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/23
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
        String clientBasic = request.getHeader(HeaderNameEnum.AUTHORIZATION.getName());
        if (StringUtils.equals(clientBasic, this.basic)) {
            httpServerHandler.onHeaderComplete(request);
        } else {
            HttpResponse response = request.getResponse();
            response.setHeader(HeaderNameEnum.WWW_AUTHENTICATE.getName(), "Basic realm=\"feat\"");
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
