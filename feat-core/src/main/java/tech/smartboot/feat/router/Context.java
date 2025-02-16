package tech.smartboot.feat.router;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

import java.util.Map;

public class Context {
    private final HttpRequest request;
    private final HttpResponse response;
    private final Map<String, String> pathParams;

    public Context(HttpRequest request, Map<String, String> pathParams) {
        this.request = request;
        this.response = request.getResponse();
        this.pathParams = pathParams;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public String getPathParam(String key) {
        return pathParams.get(key);
    }
}
