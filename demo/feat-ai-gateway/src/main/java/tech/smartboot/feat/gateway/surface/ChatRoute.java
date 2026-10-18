package tech.smartboot.feat.gateway.surface;

import tech.smartboot.feat.cloud.AsyncBodyReadUpgrade;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.gateway.config.ConfigStore;
import tech.smartboot.feat.gateway.config.KeyPolicy;
import tech.smartboot.feat.gateway.policy.AuthPolicy;
import tech.smartboot.feat.gateway.proxy.ChatProxy;
import tech.smartboot.feat.gateway.proxy.Responder;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.RouterHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 数据面入口。OpenAI 兼容的转发端点都挂在这里。
 * <p>
 * 为什么不是 {@code @Controller}：{@code @Controller} 生成的处理器只实现 {@code handle()}，
 * {@code onClose} 走接口默认的空实现，于是"客户端断开 → 取消上游"这条铁律没法实现。
 * 详见 DESIGN.md ADR-8。{@link RouterHandler} 是框架里唯一还留着 {@code onClose} 覆写点的地方。
 * <p>
 * 这里只做三件事：读体不阻塞网络线程、鉴权、把活交给 {@link ChatProxy}。
 * 转发逻辑一行都不在这里，方便以后加控制面时整包不动。
 */
public final class ChatRoute implements RouterHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatRoute.class);
    /**
     * 超过这个大小就不做异步缓冲，直接让下游的读体逻辑报 413/400。
     * 与 ServerOptions#maxRequestSize 保持一致。
     */
    private static final int MAX_ASYNC_BODY = 16 * 1024 * 1024;

    private final ConfigStore store;
    private final ChatProxy proxy;

    public ChatRoute(ConfigStore store, ChatProxy proxy) {
        this.store = store;
        this.proxy = proxy;
    }

    /**
     * 请求头到达时就把请求体切成"异步攒包"模式。
     * <p>
     * 不做这一步，控制器里读 body 就是一次真实的 socket 读，会占住网络线程。
     * 做这一步之后，读 body 变成从内存缓冲区读，耗时与 body 大小线性相关而与网络无关。
     * <p>
     * 只有带 Content-Length 的请求能这么干；chunked 请求长度未知，只能退回同步读。
     */
    @Override
    public void onHeaderComplete(HttpEndpoint request) throws IOException {
        long contentLength = request.getContentLength();
        if (contentLength > 0 && contentLength <= MAX_ASYNC_BODY && request.getUpgrade() == null) {
            request.setUpgrade(new AsyncBodyReadUpgrade(request, (int) contentLength));
        }
    }

    @Override
    public void handle(Context ctx) throws Throwable {
        throw new UnsupportedOperationException("数据面只支持异步入口");
    }

    @Override
    public void handle(Context ctx, CompletableFuture<Void> completableFuture) throws Throwable {
        HttpRequest request = ctx.Request;
        try {
            KeyPolicy key = AuthPolicy.authenticate(store.current(), request.getHeader("Authorization"));
            if (key == null) {
                request.getResponse().setHeader("WWW-Authenticate", "Bearer");
                Responder.error(request, 401, "invalid or missing api key", "authentication_error", "invalid_api_key");
                completableFuture.complete(null);
                return;
            }
            // 每条终止路径（正常结束 / 上游失败 / 客户端断开）都由代理负责 complete
            proxy.handle(request, key, completableFuture);
        } catch (Throwable t) {
            LOGGER.error("请求处理异常: {}", t.getMessage(), t);
            try {
                Responder.error(request, 500, "gateway internal error: " + t.getMessage(),
                        "server_error", "internal_error");
            } catch (Throwable ignored) {
                // 响应可能已经提交，这里只能忽略
            } finally {
                completableFuture.complete(null);
            }
        }
    }

    /**
     * 连接断开 = 调用方放弃了这个回答，立刻取消上游，别继续烧 token。
     */
    @Override
    public void onClose(HttpEndpoint request) {
        proxy.onConnectionClose(request);
    }
}
