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

import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;
import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public final class Router implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Router.class);
    /**
     * 默认404
     */
    private final RouterHandlerImpl defaultHandler;
    private final NodePath rootPath = new NodePath("/");
    private final List<InterceptorUnit> interceptors = new ArrayList<>();

    /**
     * 所有的session
     */
    private final Map<String, SessionUnit> sessions = new ConcurrentHashMap<>();
    /**
     * session监听器,用于清理过期的session
     */
    private final HashedWheelTimer timer = new HashedWheelTimer(r -> new Thread(r, "feat-session-timer"), 10, 64);
    /**
     * session的配置
     */
    private final SessionOptions sessionOptions = new SessionOptions();

    public Router() {
        this(request -> request.getResponse().setHttpStatus(HttpStatus.NOT_FOUND));
    }

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
     * @param urlPattern url匹配
     * @param handler    处理handler
     * @return
     */
    public Router route(String urlPattern, RouterHandler handler) {
        rootPath.add(urlPattern, new RouterHandlerImpl(this, urlPattern, handler));
        return this;
    }

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
                SessionUnit session = getSession(request, false);
                if (session != null) {
                    session.updateTimeoutTask();
                }
                completableFuture.whenComplete((obj, throwable) -> {
                    SessionUnit session0 = getSession(request, false);
                    if (session0 != null) {
                        session0.updateTimeoutTask();
                    }
                });
                Chain chain = new Chain(routerHandler.getRouterHandler(), list);
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

    public Router addInterceptor(String urlPattern, Interceptor interceptor) {
        return addInterceptors(Collections.singletonList(urlPattern), interceptor);
    }

    public Router addInterceptors(List<String> urlPatterns, Interceptor interceptor) {
        interceptors.add(new InterceptorUnit(urlPatterns, interceptor));
        return this;
    }


    private class InterceptorUnit {
        private final List<NodePath> path;
        private final Interceptor interceptor;

        public InterceptorUnit(List<String> patterns, Interceptor interceptor) {
            this.interceptor = interceptor;
            this.path = new ArrayList<>();
            for (String pattern : patterns) {
                NodePath nodePath = new NodePath("/" + pattern);
                nodePath.add(pattern, new RouterHandlerImpl(Router.this, pattern, null));
                path.add(nodePath);
            }
        }

        public Interceptor getInterceptor() {
            return interceptor;
        }
    }

    SessionUnit getSession(HttpRequest request, boolean create) {
        SessionUnit sessionUnit = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (Session.DEFAULT_SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    sessionUnit = sessions.get(cookie.getValue());
                    break;
                }
            }
        }
        if (sessionUnit != null) {
            return sessionUnit;
        } else if (!create) {
            return null;
        }
        SessionUnit unit = new SessionUnit();
        Session session = new MemorySession(request) {
            @Override
            public void invalidate() {
                sessions.remove(getSessionId());
                super.invalidate();
            }

            @Override
            public void setMaxAge(int interval) {
                super.setMaxAge(interval);
                unit.pauseTimeoutTask();
            }
        };
        session.setMaxAge(sessionOptions.getMaxAge());
        sessions.put(session.getSessionId(), unit);
        unit.session = session;

        return unit;
    }

    class SessionUnit {
        private Session session;
        private TimerTask timerTask;


        void pauseTimeoutTask() {
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
        }

        synchronized void updateTimeoutTask() {
            pauseTimeoutTask();
            if (session.getMaxAge() <= 0) {
                return;
            }
            timerTask = timer.schedule(() -> {
                LOGGER.info("sessionId:{} has be expired, lastAccessedTime:{} ,maxInactiveInterval:{}", session.getSessionId());
                session.invalidate();
            }, session.getMaxAge(), TimeUnit.SECONDS);
        }

        public Session getSession() {
            return session;
        }
    }
}
