/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: RouteHandle.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.handler;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.utils.AntPathMatcher;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/24
 */
public final class Router extends BaseHttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Router.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    /**
     * 默认404
     */
    private final BaseHttpHandler defaultHandler;
    private final Map<String, BaseHttpHandler> handlerMap = new ConcurrentHashMap<>();
    private final List<InterceptorInfo> interceptors = new ArrayList<>();

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
//        request.getConfiguration().getUriByteTree().addNode(request.getUri(), httpServerHandler);
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
        LOGGER.info("route: " + urlPattern);
        handlerMap.put(urlPattern, httpHandler);
        return this;
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
        BaseHttpHandler httpHandler = null;
        if (uri == null) {
            return defaultHandler;
        } else {
            httpHandler = handlerMap.get(uri);
        }
        if (httpHandler == null) {
            for (Map.Entry<String, BaseHttpHandler> entity : handlerMap.entrySet()) {
                if (PATH_MATCHER.match(entity.getKey(), uri)) {
                    httpHandler = entity.getValue();
                    System.out.println("request: " + uri + " ,match: " + entity.getKey());
                    break;
                }
            }
            if (httpHandler == null) {
                System.out.println("request: " + uri + " ,match: default    ");
                httpHandler = defaultHandler;
            }
        }
        List<InterceptorInfo> list = interceptors.stream().filter(info -> {
            boolean match = info.patterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, uri));
            if (match) {
                return info.excludes.stream().noneMatch(pattern -> PATH_MATCHER.match(pattern, uri));
            } else {
                return false;
            }
        }).collect(Collectors.toList());
        if (list.isEmpty()) {
            return httpHandler;
        } else {
            BaseHttpHandler finalHttpHandler = httpHandler;
            return new BaseHttpHandler() {
                @Override
                public void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
                    for (InterceptorInfo info : list) {
                        Object o = info.interceptor.apply(request);
                        if (o == null) {
                            continue;
                        }
                        byte[] bytes = JSON.toJSONBytes(o);
                        request.getResponse().write(bytes);
                        completableFuture.complete(o);
                        return;
                    }
                    finalHttpHandler.handle(request, completableFuture);
                }
            };
        }

    }

    public void addInterceptor(String[] patterns, String[] excludes, Interceptor interceptor) {
        addInterceptor(Arrays.asList(patterns), Arrays.asList(excludes), interceptor);
    }

    public void addInterceptor(List<String> patterns, List<String> excludes, Interceptor interceptor) {
        this.interceptors.add(new InterceptorInfo(patterns, excludes, interceptor));
    }

    public interface Interceptor {
        Object apply(HttpRequest request);
    }


    public static class InterceptorInfo {
        private final List<String> patterns;
        private final List<String> excludes;
        private final Interceptor interceptor;

        public InterceptorInfo(List<String> patterns, List<String> excludes, Interceptor interceptor) {
            this.patterns = patterns;
            this.excludes = excludes;
            this.interceptor = interceptor;
        }
    }
}
