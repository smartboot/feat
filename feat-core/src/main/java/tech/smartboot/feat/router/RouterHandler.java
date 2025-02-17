package tech.smartboot.feat.router;

import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface RouterHandler {
    default void onHeaderComplete(HttpEndpoint request) throws IOException {
    }

    default void handle(Context request, CompletableFuture<Object> completableFuture) throws Throwable {
        try {
            handle(request);
        } finally {
            completableFuture.complete(null);
        }
    }

    void handle(Context ctx) throws Throwable;

    /**
     * 断开 TCP 连接
     */
    default void onClose(HttpEndpoint request) {
    }
}
