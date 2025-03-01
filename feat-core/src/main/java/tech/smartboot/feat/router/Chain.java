package tech.smartboot.feat.router;

import java.util.List;
import java.util.concurrent.CompletableFuture;

class Chain {
    private int index;
    private final List<Interceptor> interceptors;
    private final RouterHandler handler;

    public Chain(RouterHandler handler, List<Interceptor> interceptors) {
        this.interceptors = interceptors;
        this.handler = handler;
    }

    public void proceed(Context context, CompletableFuture<Object> completableFuture) throws Throwable {
        if (index < interceptors.size()) {
            interceptors.get(index++).intercept(context, completableFuture, this);
        } else {
            handler.handle(context, completableFuture);
        }
    }
}