/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: RouteHandle.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.router;

import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/24
 */
public final class Router extends BaseHttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Router.class);
    /**
     * 默认404
     */
    private final BaseHttpHandler defaultHandler;
    private final NodePath rootPath = new NodePath("/");

    public Router() {
        this(new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws IOException {
                request.getResponse().setHttpStatus(HttpStatus.NOT_FOUND);
            }
        });
    }

    public Router(BaseHttpHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void onHeaderComplete(HttpEndpoint request) throws IOException {
        BaseHttpHandler httpServerHandler = matchHandler(request.getRequestURI());
//        System.out.println("match: " + request.getRequestURI() + " : " + httpServerHandler);
        //注册 URI 与 Handler 的映射关系
        request.getOptions().getUriByteTree().addNode(request.getUri(), httpServerHandler);
        //更新本次请求的实际 Handler
        request.setServerHandler(httpServerHandler);
        httpServerHandler.onHeaderComplete(request);
    }

    @Override
    public void onClose(HttpEndpoint request) {
        LOGGER.warn("connection is closed before route match.");
        defaultHandler.onClose(request);
    }

    @Override
    public void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
        if (request.getProtocol() == HttpProtocolEnum.HTTP_2) {
            BaseHttpHandler httpServerHandler = matchHandler(request.getRequestURI());
            httpServerHandler.handle(request, completableFuture);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * 配置URL路由
     *
     * @param urlPattern  url匹配
     * @param httpHandler 处理handler
     * @return
     */
    public Router route(String urlPattern, BaseHttpHandler httpHandler) {
        rootPath.add(urlPattern, httpHandler);
        return this;
    }

    public Router route(String urlPattern, RouterHandler httpHandler) {
        httpHandler.setUrlPattern(urlPattern);
        return route(urlPattern, new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws Throwable {
                httpHandler.handle(request);
            }
        });
    }

    public Router route(String urlPattern, HttpHandler httpHandler) {
        return route(urlPattern, new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request) throws Throwable {
                httpHandler.handle(request);
            }
        });
    }

    private BaseHttpHandler matchHandler(String uri) {
        BaseHttpHandler httpHandler = rootPath.match(uri);
        if (httpHandler == null) {
            httpHandler = defaultHandler;
        }
        return httpHandler;
    }

}
