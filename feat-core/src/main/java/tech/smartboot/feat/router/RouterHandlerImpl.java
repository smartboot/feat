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
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
final class RouterHandlerImpl implements HttpHandler {
    private List<PathIndex> pathIndexes;
    private final RouterHandler routerHandler;

    public RouterHandlerImpl(String urlPattern, RouterHandler routerHandler) {
        this.routerHandler = routerHandler;
        String[] path = urlPattern.split("/");
        pathIndexes = new ArrayList<>();
        for (int i = 0; i < path.length; i++) {
            if (path[i].startsWith(":")) {
                pathIndexes.add(new PathIndex(path[i], i));
            }
        }
        if (pathIndexes.isEmpty()) {
            pathIndexes = Collections.emptyList();
        }
    }

    @Override
    public void onHeaderComplete(HttpEndpoint request) throws IOException {
        routerHandler.onHeaderComplete(request);
    }

    @Override
    public void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
        Context context = getContext(request);
        routerHandler.handle(context, completableFuture);
    }

    public RouterHandler getRouterHandler() {
        return routerHandler;
    }

    public Context getContext(HttpRequest request) {
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
        return new Context(request, pathParams);
    }

    public void handle(HttpRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onClose(HttpEndpoint request) {
        routerHandler.onClose(request);
    }

    static class PathIndex {
        private final String path;
        private final int index;

        public PathIndex(String path, int index) {
            this.path = path;
            this.index = index;
        }
    }
}
