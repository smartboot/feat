package tech.smartboot.feat.router;

import tech.smartboot.feat.core.server.impl.HttpEndpoint;

import java.io.IOException;

public interface RouterHandler {
    default void onHeaderComplete(HttpEndpoint request) throws IOException {
    }

    void handle(Context ctx) throws Throwable;

    /**
     * 断开 TCP 连接
     */
    default void onClose(HttpEndpoint request) {
    }
}
