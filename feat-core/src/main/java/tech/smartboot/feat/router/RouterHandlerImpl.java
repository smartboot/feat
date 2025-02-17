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

class RouterHandlerImpl implements HttpHandler {
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

    public final void handle(HttpRequest request) throws Throwable {
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
        Context context = new Context(request, pathParams);
        routerHandler.handle(context);
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
