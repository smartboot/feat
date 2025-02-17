package tech.smartboot.feat.router;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

import java.util.Map;

public class Context {
    public final HttpRequest Request;
    public final HttpResponse Response;
    private final Map<String, String> pathParams;

    public Context(HttpRequest request, Map<String, String> pathParams) {
        this.Request = request;
        this.Response = request.getResponse();
        this.pathParams = pathParams;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public String getPathParam(String key) {
        return pathParams.get(key);
    }

    public String getParameter(String name) {
        return Request.getParameter(name);
    }
}
