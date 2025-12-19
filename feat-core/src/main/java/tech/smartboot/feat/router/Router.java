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

import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.router.session.LocalSessionManager;
import tech.smartboot.feat.router.session.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 路由管理器类，负责HTTP请求的路由分发和拦截器管理。
 * 该类采用前缀树（Trie）结构存储路由规则，支持动态路径参数和通配符匹配。
 * 主要功能包括：
 * <ul>
 *   <li>路由注册：支持GET、POST、PUT、DELETE等HTTP方法的注册</li>
 *   <li>路径匹配：支持动态路径参数和通配符匹配</li>
 *   <li>拦截器：支持全局和局部拦截器，可控制请求的生命周期</li>
 *   <li>会话管理：支持基于Cookie的会话管理</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * Router router = new Router();
 * router.get("/user/:id", ctx -> {
 *     String userId = ctx.pathParam("id");
 *     // 处理逻辑
 * });
 * </pre>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public final class Router implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Router.class);

    /**
     * 默认404处理器
     * <p>
     * 当没有找到匹配的路由时，使用此处理器返回404状态码
     * </p>
     */
    private final RouterHandlerImpl defaultHandler;

    /**
     * 根路径节点
     * <p>
     * 所有路由规则的根节点，用于构建路由匹配的前缀树结构
     * </p>
     */
    private final NodePath rootPath = new NodePath("/");

    /**
     * 拦截器单元列表
     * <p>
     * 存储所有注册的拦截器及其匹配路径规则
     * </p>
     */
    private final List<InterceptorUnit> interceptors = new ArrayList<>();


    private SessionManager sessionManager = new LocalSessionManager();


    /**
     * 构造一个路由管理器实例，使用默认的404处理器
     */
    public Router() {
        this(request -> request.getResponse().setHttpStatus(HttpStatus.NOT_FOUND));
    }

    /**
     * 构造一个路由管理器实例，使用指定的HTTP处理器
     *
     * @param httpHandler 默认的HTTP处理器
     */
    public Router(HttpHandler httpHandler) {
        this.defaultHandler = new RouterHandlerImpl(this, "", new RouterHandler() {
            @Override
            public void onHeaderComplete(HttpEndpoint request) throws IOException {
                httpHandler.onHeaderComplete(request);
            }

            @Override
            public void handle(Context request, CompletableFuture<Void> completableFuture) throws Throwable {
                httpHandler.handle(request.Request, completableFuture);
            }

            @Override
            public void onClose(HttpEndpoint request) {
                httpHandler.onClose(request);
            }

            @Override
            public void handle(Context ctx) throws Throwable {
                httpHandler.handle(ctx.Request);
            }
        });
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
    public void handle(HttpRequest request, CompletableFuture<Void> completableFuture) throws Throwable {
        if (request.getProtocol() == HttpProtocol.HTTP_2) {
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
     * @param urlPattern url匹配模式
     * @param handler    处理器
     * @return 路由管理器实例，支持链式调用
     */
    public Router route(String urlPattern, RouterHandler handler) {
        return route(urlPattern, "", handler);
    }

    /**
     * 配置URL路由
     *
     * @param urlPattern url匹配模式
     * @param method     HTTP方法
     * @param handler    处理器
     * @return 路由管理器实例，支持链式调用
     */
    public Router route(String urlPattern, String method, RouterHandler handler) {
        rootPath.add(urlPattern, new RouterHandlerImpl(this, urlPattern, method, handler, defaultHandler));
        return this;
    }

    /**
     * 配置URL路由，支持多个HTTP方法
     *
     * @param urlPattern url匹配模式
     * @param methods    HTTP方法数组
     * @param handler    处理器
     * @return 路由管理器实例，支持链式调用
     */
    public Router route(String urlPattern, String[] methods, RouterHandler handler) {
        for (String method : methods) {
            route(urlPattern, method, handler);
        }
        return this;
    }

    /**
     * 匹配URI并返回对应的HTTP处理器
     *
     * @param uri 待匹配的URI
     * @return 匹配到的HTTP处理器
     */
    private HttpHandler matchHandler(String uri) {
        HttpHandler httpHandler = rootPath.match(uri);
        if (httpHandler == null) {
            httpHandler = defaultHandler;
        }
        if (interceptors.isEmpty()) {
            return httpHandler;
        }
        // 检查是否存在匹配的拦截器
        List<Interceptor> list = interceptors.stream().filter(interceptor -> interceptor.path.stream().anyMatch(pattern -> pattern.match(uri) != null)).map(InterceptorUnit::getInterceptor).collect(Collectors.toList());
        if (list.isEmpty()) {
            return httpHandler;
        }
        RouterHandlerImpl routerHandler = (RouterHandlerImpl) httpHandler;
        return new HttpHandler() {

            @Override
            public void handle(HttpRequest request, CompletableFuture<Void> completableFuture) throws Throwable {
                Chain chain = new Chain(routerHandler.getRouterHandler(request), list);
                chain.proceed(routerHandler.getContext(request), completableFuture);
                if (chain.isInterrupted()) {
                    completableFuture.complete(null);
                }

            }

            @Override
            public void handle(HttpRequest request) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * 添加单个路径模式的拦截器
     *
     * @param urlPattern  URL路径模式
     * @param interceptor 拦截器实例
     * @return 路由管理器实例，支持链式调用
     */
    public Router addInterceptor(String urlPattern, Interceptor interceptor) {
        return addInterceptors(Collections.singletonList(urlPattern), interceptor);
    }

    /**
     * 添加多个路径模式的拦截器
     *
     * @param urlPatterns URL路径模式列表
     * @param interceptor 拦截器实例
     * @return 路由管理器实例，支持链式调用
     */
    public Router addInterceptors(List<String> urlPatterns, Interceptor interceptor) {
        interceptors.add(new InterceptorUnit(urlPatterns, interceptor));
        return this;
    }

    /**
     * 拦截器单元内部类
     * <p>
     * 封装拦截器及其匹配路径规则的内部类
     * </p>
     */
    private class InterceptorUnit {
        /**
         * 路径节点列表
         * <p>
         * 存储所有匹配路径对应的节点对象，用于快速匹配拦截器适用范围
         * </p>
         */
        private final List<NodePath> path;

        /**
         * 拦截器实例
         */
        private final Interceptor interceptor;

        /**
         * 构造拦截器单元
         *
         * @param patterns    路径模式列表
         * @param interceptor 拦截器实例
         */
        public InterceptorUnit(List<String> patterns, Interceptor interceptor) {
            this.interceptor = interceptor;
            this.path = new ArrayList<>();
            for (String pattern : patterns) {
                NodePath nodePath = new NodePath("/" + pattern);
                nodePath.add(pattern, new RouterHandlerImpl(Router.this, pattern, null));
                path.add(nodePath);
            }
        }

        /**
         * 获取拦截器实例
         *
         * @return 拦截器实例
         */
        public Interceptor getInterceptor() {
            return interceptor;
        }
    }

    SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
}