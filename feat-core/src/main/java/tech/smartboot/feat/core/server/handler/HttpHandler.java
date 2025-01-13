package tech.smartboot.feat.core.server.handler;

import tech.smartboot.feat.core.server.HttpRequest;

public interface HttpHandler {
    void handle(HttpRequest request) throws Throwable;
}
