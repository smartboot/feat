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

import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
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
 * 路由处理器实现类，负责具体路由请求的处理和分发
 * <p>
 * RouterHandlerImpl是RouterHandler接口的具体实现，负责：
 * <ul>
 *   <li>管理不同HTTP方法的处理器</li>
 *   <li>解析URL路径参数</li>
 *   <li>构建请求上下文对象</li>
 *   <li>分发请求到具体的处理器</li>
 * </ul>
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
final class RouterHandlerImpl implements HttpHandler {
    /**
     * 关联的路由器实例
     */
    private final Router router;
    
    /**
     * URL模式字符串
     * <p>
     * 表示当前处理器对应的URL模式，如"/user/:id"等
     * </p>
     */
    private final String urlPattern;
    
    /**
     * HTTP方法处理器映射表
     * <p>
     * 通过HTTP方法（GET、POST等）映射到对应的处理器单元
     * </p>
     */
    private Map<String, RouterUnit> methodHandlers;
    
    /**
     * 默认路由单元
     * <p>
     * 当没有指定HTTP方法或找不到对应方法的处理器时使用此路由单元
     * </p>
     */
    private RouterUnit routerUnit;
    
    /**
     * 默认的路由处理器实现
     * <p>
     * 当当前处理器无法处理请求时，转发给此默认处理器
     * </p>
     */
    private final RouterHandlerImpl routerDefaultHandler;

    /**
     * 构造一个路由处理器实现
     *
     * @param router       关联的路由器实例
     * @param urlPattern   URL模式
     * @param routerHandler 路由处理器
     */
    public RouterHandlerImpl(Router router, String urlPattern, RouterHandler routerHandler) {
        this(router, urlPattern, null, routerHandler, null);
    }

    /**
     * 构造一个路由处理器实现
     *
     * @param router              关联的路由器实例
     * @param urlPattern          URL模式
     * @param method              HTTP方法
     * @param routerHandler       路由处理器
     * @param routerDefaultHandler 默认路由处理器实现
     */
    public RouterHandlerImpl(Router router, String urlPattern, String method, RouterHandler routerHandler, RouterHandlerImpl routerDefaultHandler) {
        this.router = router;
        this.urlPattern = urlPattern;
        this.routerDefaultHandler = routerDefaultHandler;
        String[] path = urlPattern.split("/");
        List<PathIndex> pathIndexes = new ArrayList<>();
        for (int i = 0; i < path.length; i++) {
            if (path[i].startsWith(":")) {
                pathIndexes.add(new PathIndex(path[i].substring(1), i));
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

    /**
     * 获取请求对应的路由处理器
     *
     * @param request HTTP请求对象
     * @return 路由处理器实例
     */
    public RouterHandler getRouterHandler(HttpRequest request) {
        return getRouterUnit(request).routerHandler;
    }

    /**
     * 获取请求上下文对象
     *
     * @param request HTTP请求对象
     * @return 请求上下文对象
     */
    public Context getContext(HttpRequest request) {
        RouterUnit handler = getRouterUnit(request);
        return getContext(request, handler.pathIndexes);
    }

    /**
     * 根据路径索引获取请求上下文对象
     *
     * @param request     HTTP请求对象
     * @param pathIndexes 路径索引列表
     * @return 请求上下文对象
     */
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
                    params.put(pathIndex.path, "");
                } else {
                    params.put(pathIndex.path, path[pathIndex.index]);
                }

            }
            pathParams = Collections.unmodifiableMap(params);
        }
        return new Context(router, request, pathParams);
    }

    /**
     * 同步处理HTTP请求（不支持）
     *
     * @param request HTTP请求对象
     */
    public void handle(HttpRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onClose(HttpEndpoint request) {
        getRouterUnit(request).routerHandler.onClose(request);
    }

    /**
     * 获取请求对应的路由单元
     *
     * @param request HTTP请求对象
     * @return 路由单元实例
     */
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

    /**
     * 路径索引内部类
     * <p>
     * 用于存储路径参数的名称和在路径中的索引位置
     * </p>
     */
    static class PathIndex {
        /**
         * 路径参数名称
         */
        private final String path;
        
        /**
         * 路径参数在URL路径中的索引位置
         */
        private final int index;

        /**
         * 构造一个路径索引实例
         *
         * @param path  路径参数名称
         * @param index 索引位置
         */
        public PathIndex(String path, int index) {
            this.path = path;
            this.index = index;
        }
    }

    /**
     * 添加HTTP方法处理器
     *
     * @param methodHandler 方法处理器
     */
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

    /**
     * 路由单元内部类
     * <p>
     * 封装特定HTTP方法的路由处理器及相关信息
     * </p>
     */
    static class RouterUnit {
        /**
         * HTTP方法
         * <p>
         * 如GET、POST等，如果为空则表示默认处理器
         * </p>
         */
        private final String method;
        
        /**
         * 路径索引列表
         * <p>
         * 存储URL路径中参数的位置信息，用于提取路径参数
         * </p>
         */
        private final List<PathIndex> pathIndexes;
        
        /**
         * 路由处理器实例
         */
        private final RouterHandler routerHandler;

        /**
         * 构造一个路由单元实例
         *
         * @param method        HTTP方法
         * @param pathIndexes   路径索引列表
         * @param routerHandler 路由处理器
         */
        public RouterUnit(String method, List<PathIndex> pathIndexes, RouterHandler routerHandler) {
            this.method = method;
            this.pathIndexes = pathIndexes;
            this.routerHandler = routerHandler;
        }
    }
}