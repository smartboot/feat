package tech.smartboot.feat.core.server;

import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface HttpHandler {
    default void onHeaderComplete(HttpEndpoint request) throws IOException {
    }

    default void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
        try {
            handle(request);
        } finally {
            completableFuture.complete(null);
        }
    }

    void handle(HttpRequest request) throws Throwable;

    /**
     * 断开 TCP 连接
     */
    default void onClose(HttpEndpoint request) {
    }
}
