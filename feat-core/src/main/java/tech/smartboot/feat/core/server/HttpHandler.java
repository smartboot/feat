package tech.smartboot.feat.core.server;

public interface HttpHandler {
    void handle(HttpRequest request) throws Throwable;
}
