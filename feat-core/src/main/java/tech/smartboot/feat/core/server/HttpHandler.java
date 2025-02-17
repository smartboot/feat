package tech.smartboot.feat.core.server;

import java.util.concurrent.CompletableFuture;

public interface HttpHandler {
    default void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
        try {
            handle(request);
        } finally {
            completableFuture.complete(null);
        }
    }

    void handle(HttpRequest request) throws Throwable;
}
