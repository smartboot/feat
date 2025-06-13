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

import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
final class RouterHandlerImpl implements HttpHandler {
    private final Router router;
    private final String urlPattern;
    private Map<String, RouterUnit> methodHandlers;
    private RouterUnit routerUnit;
    private final RouterHandlerImpl routerDefaultHandler;

    public RouterHandlerImpl(Router router, String urlPattern, RouterHandler routerHandler) {
        this(router, urlPattern, null, routerHandler, null);
    }

    public RouterHandlerImpl(Router router, String urlPattern, String method, RouterHandler routerHandler, RouterHandlerImpl routerDefaultHandler) {
        this.router = router;
        this.urlPattern = urlPattern;
        this.routerDefaultHandler = routerDefaultHandler;
        String[] path = urlPattern.split("/");
        List<PathIndex> pathIndexes = new ArrayList<>();
        for (int i = 0; i < path.length; i++) {
            if (path[i].startsWith(":")) {
                pathIndexes.add(new PathIndex(path[i], i));
            }
        }
        if (pathIndexes.isEmpty()) {
            pathIndexes = Collections.emptyList();
        }
        this.routerUnit = new RouterUnit(method, pathIndexes, routerHandler);
    }

    @Override
    public void onHeaderComplete(HttpEndpoint request) throws IOException {
        RouterHandler handler = getRouterHandler(request);
        handler.onHeaderComplete(request);
    }

    @Override
    public void handle(HttpRequest request, CompletableFuture<Void> completableFuture) throws Throwable {
        RouterUnit handler = getRouterUnit(request);
        handler.routerHandler.handle(getContext(request, handler.pathIndexes), completableFuture);
    }

    public RouterHandler getRouterHandler(HttpRequest request) {
        return getRouterUnit(request).routerHandler;
    }

    public Context getContext(HttpRequest request) {
        RouterUnit handler = getRouterUnit(request);
        return getContext(request, handler.pathIndexes);
    }

    public Context getContext(HttpRequest request, List<PathIndex> pathIndexes) {
        Map<String, String> pathParams;
        if (pathIndexes.isEmpty()) {
            pathParams = Collections.emptyMap();
        } else {
            String[] path = request.getRequestURI().split("/");
            HashMap<String, String> params = new HashMap<>();
            for (PathIndex pathIndex : pathIndexes) {
                //此时说明是最后一个路径，并且是空字符串
                if (pathIndex.index == path.length) {
                    params.put(pathIndex.path.substring(1), "");
                } else {
                    params.put(pathIndex.path.substring(1), path[pathIndex.index]);
                }

            }
            pathParams = Collections.unmodifiableMap(params);
        }
        return new Context(router, request, pathParams);
    }

    public void handle(HttpRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onClose(HttpEndpoint request) {
        getRouterUnit(request).routerHandler.onClose(request);
    }

    private RouterUnit getRouterUnit(HttpRequest request) {
        RouterUnit handler = methodHandlers == null ? null : methodHandlers.get(request.getMethod());
        if (handler != null) {
            return handler;
        }
        if (FeatUtils.isBlank(routerUnit.method) || FeatUtils.equals(routerUnit.method, request.getMethod())) {
            return routerUnit;
        }
        return routerDefaultHandler.routerUnit;
    }

    static class PathIndex {
        private final String path;
        private final int index;

        public PathIndex(String path, int index) {
            this.path = path;
            this.index = index;
        }
    }

    public void addMethodHandler(RouterHandlerImpl methodHandler) {
        if (FeatUtils.isBlank(methodHandler.routerUnit.method)) {
            if (FeatUtils.isBlank(routerUnit.method)) {
                throw new FeatException("urlPattern:[" + urlPattern + "] is duplicate");
            }
            if (methodHandlers == null) {
                methodHandlers = new HashMap<>();
            }
            if (methodHandlers.containsKey(routerUnit.method)) {
                throw new FeatException("urlPattern:[" + urlPattern + "],method:[" + routerUnit.method + "] is duplicate");
            }
            //将原始的迁移到map中
            methodHandlers.put(routerUnit.method, routerUnit);
            this.routerUnit = methodHandler.routerUnit;
            return;
        }
        if (methodHandlers == null) {
            methodHandlers = new HashMap<>();
        }
        if (FeatUtils.equals(routerUnit.method, methodHandler.routerUnit.method) || methodHandlers.containsKey(methodHandler.routerUnit.method)) {
            throw new FeatException("urlPattern:[" + urlPattern + "],method:[" + routerUnit.method + "] is duplicate");
        }
        methodHandlers.put(methodHandler.routerUnit.method, methodHandler.routerUnit);
    }

    static class RouterUnit {
        private final String method;
        private final List<PathIndex> pathIndexes;
        private final RouterHandler routerHandler;

        public RouterUnit(String method, List<PathIndex> pathIndexes, RouterHandler routerHandler) {
            this.method = method;
            this.pathIndexes = pathIndexes;
            this.routerHandler = routerHandler;
        }
    }
}
