package tech.smartboot.feat.core.server.handler;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

public interface HttpHandler {
    void handle(HttpRequest request, HttpResponse response) throws Throwable;
}
