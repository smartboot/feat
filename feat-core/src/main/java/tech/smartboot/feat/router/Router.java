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
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/24
 */
public final class Router implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Router.class);
    /**
     * 默认404
     */
    private final HttpHandler defaultHandler;
    private final NodePath rootPath = new NodePath("/");
    private final List<Interceptor> interceptors = new ArrayList<>();

    public Router() {
        this(request -> request.getResponse().setHttpStatus(HttpStatus.NOT_FOUND));
    }

    public Router(HttpHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void onHeaderComplete(HttpEndpoint request) throws IOException {
        HttpHandler httpServerHandler = matchHandler(request.getRequestURI());
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
            HttpHandler httpServerHandler = matchHandler(request.getRequestURI());
            httpServerHandler.handle(request, completableFuture);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void handle(HttpRequest request) throws Throwable {
        throw new UnsupportedOperationException();
    }

    /**
     * 配置URL路由
     *
     * @param urlPattern url匹配
     * @param handler    处理handler
     * @return
     */
    public Router route(String urlPattern, RouterHandler handler) {
        rootPath.add(urlPattern, new RouterHandlerImpl(urlPattern, handler));
        return this;
    }

    private HttpHandler matchHandler(String uri) {
        HttpHandler httpHandler = rootPath.match(uri);
        if (httpHandler == null) {
            httpHandler = defaultHandler;
        }
        // 检查是否存在匹配的拦截器
        List<Interceptor> list = interceptors.stream().filter(interceptor -> {
            //todo pathPattern
            return false;
        }).collect(Collectors.toList());
        if (list.isEmpty()) {
            return httpHandler;
        }
        RouterHandlerImpl routerHandler = (RouterHandlerImpl) httpHandler;
        return new HttpHandler() {

            @Override
            public void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
                ChainImpl chain = new ChainImpl(routerHandler.getRouterHandler(), list);
                chain.proceed(routerHandler.getContext(request), completableFuture);
            }

            @Override
            public void handle(HttpRequest request) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Router addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    class ChainImpl implements Interceptor.Chain {
        private int index;
        private final List<Interceptor> interceptors;
        private final RouterHandler handler;

        public ChainImpl(RouterHandler handler, List<Interceptor> interceptors) {
            this.interceptors = interceptors;
            this.handler = handler;
        }

        @Override
        public void proceed(Context context, CompletableFuture<Object> completableFuture) throws Throwable {
            if (index < interceptors.size()) {
                interceptors.get(index++).intercept(context, completableFuture, this);
            } else {
                handler.handle(context, completableFuture);
            }
        }
    }
}
