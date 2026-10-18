package tech.smartboot.feat.gateway.surface;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.gateway.proxy.Responder;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.RouterHandler;

import java.util.concurrent.CompletableFuture;

/**
 * {@code /v1/*} 的兜底：未知端点也要返回 OpenAI 的错误结构，不能吐 HTML。
 * <p>
 * 挂在通配符上，不会遮蔽已经精确注册的 {@code /v1/chat/completions} —— 路由树先查精确节点再查通配符。
 */
public final class NotFoundRoute implements RouterHandler {

    @Override
    public void handle(Context ctx) throws Throwable {
        respond(ctx.Request);
    }

    @Override
    public void handle(Context ctx, CompletableFuture<Void> completableFuture) throws Throwable {
        respond(ctx.Request);
    }

    @Override
    public void onClose(HttpEndpoint request) {
        // 无在途状态，不需要处理
    }

    private static void respond(HttpRequest request) {
        Responder.error(request, 404, "unknown endpoint: " + request.getRequestURI(),
                "invalid_request_error", "unknown_endpoint");
    }
}
