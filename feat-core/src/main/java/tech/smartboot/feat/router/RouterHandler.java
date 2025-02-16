package tech.smartboot.feat.router;

import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RouterHandler implements HttpHandler {
    private List<PathIndex> pathIndexes;

    @Override
    public final void handle(HttpRequest request) throws Throwable {
        Map<String, String> pathParams;
        if (pathIndexes.isEmpty()) {
            pathParams = Collections.emptyMap();
        } else {
            String[] path = request.getRequestURI().split("/");
            HashMap<String, String> params = new HashMap<>();
            pathIndexes.forEach(pathIndex -> params.put(pathIndex.path.substring(1), path[pathIndex.index]));
            pathParams = Collections.unmodifiableMap(params);
        }
        Context context = new Context(request, pathParams);
        handle(context);
    }

    void setUrlPattern(String urlPattern) {
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

    public abstract void handle(Context ctx) throws Throwable;

    static class PathIndex {
        private final String path;
        private final int index;

        public PathIndex(String path, int index) {
            this.path = path;
            this.index = index;
        }
    }
}
