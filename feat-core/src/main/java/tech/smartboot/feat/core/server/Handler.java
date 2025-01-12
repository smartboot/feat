package tech.smartboot.feat.core.server;

public interface Handler {
    void handle(HttpRequest request, HttpResponse response) throws Throwable;
}
