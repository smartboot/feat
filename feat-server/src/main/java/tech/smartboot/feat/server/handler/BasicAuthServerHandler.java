/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: BasicAuthServerHandle.java
 * Date: 2021-02-23
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server.handler;

import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HttpStatus;
import tech.smartboot.feat.common.utils.StringUtils;
import tech.smartboot.feat.server.HttpRequest;
import tech.smartboot.feat.server.HttpResponse;
import tech.smartboot.feat.server.HttpServerHandler;
import tech.smartboot.feat.server.impl.Request;

import java.io.IOException;
import java.util.Base64;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/23
 */
public final class BasicAuthServerHandler extends HttpServerHandler {
    private final HttpServerHandler httpServerHandler;
    private final String basic;

    public BasicAuthServerHandler(String username, String password, HttpServerHandler httpServerHandler) {
        this.httpServerHandler = httpServerHandler;
        basic = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    @Override
    public void onHeaderComplete(Request request) throws IOException {
        String clientBasic = request.getHeader(HeaderNameEnum.AUTHORIZATION.getName());
        if (StringUtils.equals(clientBasic, this.basic)) {
            httpServerHandler.onHeaderComplete(request);
        } else {
            HttpResponse response = request.newHttpRequest().getResponse();
            response.setHeader(HeaderNameEnum.WWW_AUTHENTICATE.getName(), "Basic realm=\"feat\"");
            response.setHttpStatus(HttpStatus.UNAUTHORIZED);
            response.close();
        }
    }

    @Override
    public void onClose(Request request) {
        httpServerHandler.onClose(request);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Throwable {
        httpServerHandler.handle(request, response);
    }

}