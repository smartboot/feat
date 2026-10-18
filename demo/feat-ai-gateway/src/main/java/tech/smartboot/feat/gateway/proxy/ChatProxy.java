package tech.smartboot.feat.gateway.proxy;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.client.stream.Stream;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.gateway.config.ConfigStore;
import tech.smartboot.feat.gateway.config.GatewayConfig;
import tech.smartboot.feat.gateway.config.KeyPolicy;
import tech.smartboot.feat.gateway.config.ModelRoute;
import tech.smartboot.feat.gateway.config.Provider;
import tech.smartboot.feat.gateway.metric.GatewayMetrics;
import tech.smartboot.feat.gateway.metric.UsageLog;
import tech.smartboot.feat.gateway.metric.UsageRecord;
import tech.smartboot.feat.gateway.policy.QuotaGuard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenAI 兼容的转发核心。
 * <p>
 * 链路：解析 -> 路由 -> 上游请求 -> 首字透传 -> 计量。
 * <p>
 * 三个不能妥协的点：
 * <ol>
 *   <li>上游每个 chunk 立刻写给客户端并 flush，绝不聚合成完整响应；</li>
 *   <li>调用方断开时立刻取消上游，否则一个已无人接收的长回答会继续烧 token；</li>
 *   <li>计量全异步，转发线程里不碰磁盘。</li>
 * </ol>
 */
public final class ChatProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatProxy.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int MAX_REQUEST_BODY = 8 * 1024 * 1024;
    private static final int MAX_CAPTURED_BODY = 4 * 1024 * 1024;

    public static final String PATH_CHAT = "/v1/chat/completions";
    public static final String PATH_COMPLETIONS = "/v1/completions";
    public static final String PATH_EMBEDDINGS = "/v1/embeddings";
    public static final String PATH_MODELS = "/v1/models";

    private final ConfigStore store;
    private final UpstreamPool pool;
    private final QuotaGuard quota;
    private final GatewayMetrics metrics;
    private final UsageLog usageLog;
    /**
     * 连接 -> 在途请求。feat 在连接关闭时回调 HttpHandler#onClose，
     * 我们靠这张表把"客户端跑了"翻译成"取消上游"。
     */
    private final Map<HttpEndpoint, Ctx> inflight = new ConcurrentHashMap<HttpEndpoint, Ctx>();

    public ChatProxy(ConfigStore store, UpstreamPool pool, QuotaGuard quota,
                     GatewayMetrics metrics, UsageLog usageLog) {
        this.store = store;
        this.pool = pool;
        this.quota = quota;
        this.metrics = metrics;
        this.usageLog = usageLog;
    }

    public static boolean isProxyPath(String path) {
        return PATH_CHAT.equals(path) || PATH_COMPLETIONS.equals(path) || PATH_EMBEDDINGS.equals(path);
    }

    /**
     * {@code GET /v1/models}：只暴露配置里声明过的模型名，不透出上游清单。
     */
    public void handle(HttpRequest request, KeyPolicy key, CompletableFuture<Void> future) {
        String path = request.getRequestURI();
        if (PATH_MODELS.equals(path)) {
            listModels(request);
            future.complete(null);
            return;
        }
        if (!isProxyPath(path)) {
            Responder.error(request, 404, "unknown endpoint: " + path, "invalid_request_error", "unknown_endpoint");
            future.complete(null);
            return;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            Responder.error(request, 405, "method not allowed: " + request.getMethod(),
                    "invalid_request_error", "method_not_allowed");
            future.complete(null);
            return;
        }

        long startMs = System.currentTimeMillis();
        long startNanos = System.nanoTime();

        byte[] raw;
        JSONObject payload;
        try {
            raw = readBody(request);
            payload = JSON.parseObject(new String(raw, UTF8));
            if (payload == null) {
                throw new IllegalArgumentException("请求体不是合法的 JSON 对象");
            }
        } catch (IllegalArgumentException e) {
            Responder.error(request, 400, e.getMessage(), "invalid_request_error", "invalid_json");
            future.complete(null);
            return;
        } catch (IOException e) {
            Responder.error(request, 400, "读取请求体失败: " + e.getMessage(), "invalid_request_error", "body_read_error");
            future.complete(null);
            return;
        }

        String model = payload.getString("model");
        if (model == null || model.isEmpty()) {
            Responder.error(request, 400, "缺少 model 字段", "invalid_request_error", "missing_model");
            future.complete(null);
            return;
        }

        GatewayConfig config = store.current();
        List<ModelRoute> routes = config.routes(model);
        if (routes.isEmpty()) {
            Responder.error(request, 404, "模型未配置: " + model, "invalid_request_error", "model_not_found");
            future.complete(null);
            return;
        }
        if (!key.allowsModel(model)) {
            Responder.error(request, 403, "该 key 不允许调用模型: " + model, "invalid_request_error", "model_forbidden");
            future.complete(null);
            return;
        }

        QuotaGuard.Decision decision = quota.check(key);
        if (!decision.allowed()) {
            metrics.onRejected(key.id());
            Responder.error(request, decision.status(), decision.message(), "rate_limit_error", "rate_limited");
            future.complete(null);
            return;
        }

        boolean stream = Boolean.TRUE.equals(payload.getBoolean("stream"));
        if (stream) {
            // 不带这个参数，OpenAI 流式响应不会返回 usage，计量只能靠估算
            JSONObject options = payload.getJSONObject("stream_options");
            if (options == null) {
                options = new JSONObject();
                payload.put("stream_options", options);
            }
            options.put("include_usage", true);
        }

        Ctx ctx = new Ctx(request, key, path, model, stream, raw, payload, routes, startMs, startNanos, future);
        if (request instanceof HttpEndpoint) {
            inflight.put((HttpEndpoint) request, ctx);
        }
        metrics.onRequestStart(model);
        attempt(ctx);
    }

    /**
     * 由 HttpHandler#onClose 调用：连接已断，若还有在途请求就取消它。
     */
    public void onConnectionClose(HttpEndpoint endpoint) {
        Ctx ctx = inflight.remove(endpoint);
        if (ctx != null) {
            onClientGone(ctx);
        }
    }

    public int inflightCount() {
        return inflight.size();
    }

    // ------------------------------------------------------------------ 转发

    private void attempt(final Ctx ctx) {
        if (ctx.index >= ctx.routes.size()) {
            String reason = ctx.lastError == null ? "所有上游均不可用" : ctx.lastError;
            metrics.onRequestEnd(ctx.key.id(), -1, elapsedMs(ctx.startNanos), 0, 0, false, ctx.provider, true);
            Responder.error(ctx.request, 502, reason, "upstream_error", "no_available_provider");
            writeUsage(ctx, 502, 0, 0, false, reason);
            detach(ctx);
            return;
        }

        final ModelRoute route = ctx.routes.get(ctx.index);
        GatewayConfig config = store.current();
        final Provider provider = config.provider(route.provider());
        if (provider == null) {
            ctx.lastError = "provider 不存在: " + route.provider();
            ctx.index++;
            attempt(ctx);
            return;
        }

        JSONObject upstreamBody = (JSONObject) ctx.payload.clone();
        upstreamBody.put("model", route.upstream());

        UpstreamPool.Lease lease;
        try {
            lease = pool.acquire(provider);
        } catch (Throwable t) {
            ctx.lastError = "获取上游连接失败: " + t.getMessage();
            ctx.index++;
            attempt(ctx);
            return;
        }
        ctx.provider = provider.name();
        ctx.lease = lease;

        HttpPost post;
        try {
            post = lease.client().post(ctx.path);
        } catch (Throwable t) {
            ctx.lastError = "创建上游请求失败: " + t.getMessage();
            lease.release();
            ctx.lease = null;
            ctx.index++;
            attempt(ctx);
            return;
        }

        post.header(header -> {
            if (!provider.apiKey().isEmpty()) {
                header.set(provider.authHeader(), provider.authValue());
            }
            header.set("Accept", ctx.stream ? "text/event-stream" : "application/json");
        });
        post.postJson(upstreamBody);
        post.onResponseHeader(upstream -> onUpstreamHeader(ctx, upstream));
        post.onResponseBody(new Stream() {
            @Override
            public void stream(tech.smartboot.feat.core.client.HttpResponse response, byte[] bytes, boolean end) {
                onUpstreamBody(ctx, bytes, end);
            }
        });
        post.onFailure(throwable -> onUpstreamFailure(ctx, throwable));
        ctx.current = post;
        post.submit();
    }

    private void onUpstreamHeader(Ctx ctx, tech.smartboot.feat.core.client.HttpResponse upstream) {
        if (ctx.finished.get()) {
            return;
        }
        ctx.upstreamStatus = upstream.statusCode();
        if (ctx.upstreamStatus < 200 || ctx.upstreamStatus >= 300) {
            ctx.errBuf = new ByteArrayOutputStream(1024);
            return;
        }

        HttpResponse out = ctx.request.getResponse();
        try {
            String contentType = upstream.getContentType();
            out.setHttpStatus(ctx.upstreamStatus, "OK");
            out.setHeader("Content-Type", contentType == null ? "application/json;charset=utf-8" : contentType);
            out.setHeader("X-Feat-Gateway-Provider", ctx.provider);
            if (ctx.stream) {
                out.setHeader("Cache-Control", "no-cache");
                out.setHeader("X-Accel-Buffering", "no");
            }
            // 立刻提交响应头，首字节才能第一时间到达客户端
            out.getOutputStream().flush();
            ctx.committed = true;
            ctx.firstByteNanos = System.nanoTime();
        } catch (IOException e) {
            onClientGone(ctx);
        }
    }

    private void onUpstreamBody(Ctx ctx, byte[] bytes, boolean end) {
        if (ctx.finished.get()) {
            return;
        }
        if (end) {
            if (ctx.committed) {
                complete(ctx);
            } else {
                onUpstreamErrorBodyComplete(ctx);
            }
            return;
        }
        if (bytes.length == 0) {
            return;
        }
        if (!ctx.committed) {
            if (ctx.errBuf != null && ctx.errBuf.size() < MAX_CAPTURED_BODY) {
                ctx.errBuf.write(bytes, 0, bytes.length);
            }
            return;
        }
        if (clientGone(ctx)) {
            onClientGone(ctx);
            return;
        }
        try {
            HttpResponse out = ctx.request.getResponse();
            out.write(bytes);
            out.getOutputStream().flush();
            ctx.bytesOut += bytes.length;
            if (ctx.ttftNanos < 0) {
                ctx.ttftNanos = System.nanoTime();
            }
            if (ctx.stream) {
                sniffStreamUsage(ctx, bytes);
            } else if (ctx.bodyBuf != null && ctx.bodyBuf.size() < MAX_CAPTURED_BODY) {
                ctx.bodyBuf.write(bytes, 0, bytes.length);
            }
        } catch (IOException e) {
            onClientGone(ctx);
        }
    }

    private void onUpstreamErrorBodyComplete(Ctx ctx) {
        int status = ctx.upstreamStatus;
        if (shouldFallback(status) && ctx.index + 1 < ctx.routes.size()) {
            ctx.index++;
            ctx.errBuf = null;
            releaseLease(ctx);
            attempt(ctx);
            return;
        }
        String body = ctx.errBuf == null ? "" : new String(ctx.errBuf.toByteArray(), UTF8);
        if (body.isEmpty()) {
            body = "{\"error\":{\"message\":\"upstream returned " + status + "\",\"type\":\"upstream_error\"}}";
        }
        Responder.json(ctx.request, status, body);
        String error = "upstream status " + status;
        metrics.onRequestEnd(ctx.key.id(), -1, elapsedMs(ctx.startNanos), 0, 0, false, ctx.provider, true);
        writeUsage(ctx, status, 0, 0, false, error);
        releaseLease(ctx);
        detach(ctx);
        ctx.finished.set(true);
        finishFuture(ctx);
    }

    private void onUpstreamFailure(Ctx ctx, Throwable throwable) {
        if (ctx.finished.get()) {
            return;
        }
        ctx.lastError = throwable.getMessage() == null ? throwable.toString() : throwable.getMessage();
        LOGGER.debug("上游请求失败 model={} provider={}: {}", ctx.model, ctx.provider, ctx.lastError);

        if (!ctx.committed) {
            releaseLease(ctx);
            if (ctx.index + 1 < ctx.routes.size()) {
                ctx.index++;
                attempt(ctx);
                return;
            }
            metrics.onRequestEnd(ctx.key.id(), -1, elapsedMs(ctx.startNanos), 0, 0, false, ctx.provider, true);
            Responder.error(ctx.request, 502, ctx.lastError, "upstream_error", "upstream_unavailable");
            writeUsage(ctx, 502, 0, 0, false, ctx.lastError);
            detach(ctx);
            ctx.finished.set(true);
            finishFuture(ctx);
            return;
        }

        // 已经向客户端输出过内容，无法再改状态码，只能断开——让客户端知道流被中断
        try {
            ctx.request.getResponse().close();
        } catch (Throwable ignored) {
        }
        metrics.onRequestEnd(ctx.key.id(), ttftMs(ctx), elapsedMs(ctx.startNanos), 0, 0, false, ctx.provider, true);
        writeUsage(ctx, 502, 0, 0, false, "stream interrupted: " + ctx.lastError);
        releaseLease(ctx);
        detach(ctx);
        ctx.finished.set(true);
        finishFuture(ctx);
    }

    private void complete(Ctx ctx) {
        if (!ctx.finished.compareAndSet(false, true)) {
            return;
        }
        detach(ctx);
        try {
            ctx.request.getResponse().close();
        } catch (Throwable ignored) {
        }
        finishFuture(ctx);

        parseNonStreamUsage(ctx);
        long prompt = ctx.promptTokens;
        long completion = ctx.completionTokens;
        boolean estimated = false;
        if (prompt <= 0 && completion <= 0) {
            estimated = true;
            prompt = Math.max(1, ctx.rawBody.length / 4L);
            completion = Math.max(0, ctx.bytesOut / 4L);
        }
        quota.record(ctx.key, prompt + completion);
        // 非流式请求没有"首字"概念，硬塞一个 TTFT 会把指标带偏
        metrics.onRequestEnd(ctx.key.id(), ctx.stream ? ttftMs(ctx) : -1, elapsedMs(ctx.startNanos),
                prompt, completion, estimated, ctx.provider, false);
        writeUsage(ctx, ctx.upstreamStatus, prompt, completion, estimated, null);
        releaseLease(ctx);
    }

    /**
     * 客户端断开：停止下游写入，取消上游请求，按 499 记账。
     * <p>
     * 不做这一步，用户关掉页面后上游仍会把整段回答生成完——既是成本，也是连接泄漏。
     */
    private void onClientGone(Ctx ctx) {
        if (!ctx.finished.compareAndSet(false, true)) {
            return;
        }
        detach(ctx);
        HttpRest current = ctx.current;
        if (current != null) {
            try {
                current.close();
            } catch (Throwable t) {
                LOGGER.debug("取消上游请求失败: {}", t.getMessage());
            }
        }
        // 已经吐出去的部分要计入成本，否则中断请求会被记成 0 消耗
        long prompt = Math.max(1, ctx.rawBody.length / 4L);
        long completion = Math.max(0, ctx.bytesOut / 4L);
        quota.record(ctx.key, prompt + completion);
        metrics.onRequestEnd(ctx.key.id(), ctx.stream ? ttftMs(ctx) : -1, elapsedMs(ctx.startNanos),
                prompt, completion, true, ctx.provider, true);
        writeUsage(ctx, 499, prompt, completion, true, "client_closed");
        releaseLease(ctx);
        // 这里故意不 finishFuture。
        // 连接已经断了，feat 在 future 完成回调里还会走一遍 keepConnection -> finishResponse ->
        // FeatOutputStream#close()，而这一步必然对已关闭的 writeBuffer 写入失败，被框架记成
        // "SEVERE IOException: writeBuffer has closed" + 40 行栈。客户端中途关页面是正常事件，
        // 不该在日志里长这样。future 只被 HTTP 层的一个 lambda 引用，Ctx 又已从 inflight 摘除，
        // 两者都不可达后由 GC 回收，不会泄漏。
    }

    /**
     * feat 的异步模型：handler 返回后框架会检查 CompletableFuture，
     * 没完成才认为是异步响应、不会关连接。所以每条终止路径都必须完成它。
     */
    private static void finishFuture(Ctx ctx) {
        ctx.future.complete(null);
    }

    private void releaseLease(Ctx ctx) {
        UpstreamPool.Lease lease = ctx.lease;
        ctx.lease = null;
        if (lease != null) {
            lease.release();
        }
    }

    private void detach(Ctx ctx) {
        if (ctx.request instanceof HttpEndpoint) {
            inflight.remove((HttpEndpoint) ctx.request);
        }
    }

    private void parseNonStreamUsage(Ctx ctx) {
        if (ctx.bodyBuf == null || ctx.bodyBuf.size() == 0) {
            return;
        }
        try {
            JSONObject json = JSON.parseObject(new String(ctx.bodyBuf.toByteArray(), UTF8));
            if (json == null) {
                return;
            }
            JSONObject usage = json.getJSONObject("usage");
            if (usage != null) {
                ctx.promptTokens = usage.getLongValue("prompt_tokens");
                ctx.completionTokens = usage.getLongValue("completion_tokens");
            }
        } catch (Exception ignored) {
            // 上游返回的不是标准 JSON 时，计量退化为估算
        }
    }

    // ------------------------------------------------------------------ 用量

    private void sniffStreamUsage(Ctx ctx, byte[] bytes) {
        // 绝大多数 chunk 不含 usage，先用一次 byte 扫描挡掉，避免每个 chunk 都做字符串解析
        if (!contains(bytes, "usage")) {
            return;
        }
        String text = new String(bytes, UTF8);
        int from = 0;
        while (from < text.length()) {
            int lineEnd = text.indexOf('\n', from);
            String line = lineEnd < 0 ? text.substring(from) : text.substring(from, lineEnd);
            from = lineEnd < 0 ? text.length() : lineEnd + 1;
            int idx = line.indexOf("data:");
            if (idx < 0 || line.indexOf("\"usage\"") < 0) {
                continue;
            }
            String data = line.substring(idx + 5).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) {
                continue;
            }
            try {
                JSONObject usage = JSON.parseObject(data).getJSONObject("usage");
                if (usage != null) {
                    ctx.promptTokens = usage.getLongValue("prompt_tokens");
                    ctx.completionTokens = usage.getLongValue("completion_tokens");
                }
            } catch (Exception ignored) {
                // usage 行解析失败不影响转发，计量退化为估算
            }
        }
    }

    private static boolean contains(byte[] bytes, String ascii) {
        byte[] target = ascii.getBytes(UTF8);
        outer:
        for (int i = 0; i <= bytes.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (bytes[i + j] != target[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private void writeUsage(Ctx ctx, int status, long prompt, long completion, boolean estimated, String error) {
        UsageRecord record = new UsageRecord();
        record.tsMs = ctx.startMs;
        record.traceId = ctx.traceId;
        record.keyId = ctx.key.id();
        record.model = ctx.model;
        record.provider = ctx.provider;
        record.status = status;
        record.stream = ctx.stream;
        record.ttftMs = ttftMs(ctx);
        record.totalMs = elapsedMs(ctx.startNanos);
        record.promptTokens = prompt;
        record.completionTokens = completion;
        record.estimated = estimated;
        record.bytesOut = ctx.bytesOut;
        record.error = error;
        usageLog.write(record);
    }

    private static long ttftMs(Ctx ctx) {
        if (ctx.ttftNanos < 0) {
            return -1;
        }
        return (ctx.ttftNanos - ctx.startNanos) / 1000000L;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1000000L;
    }

    private static boolean shouldFallback(int status) {
        return status == 429 || status >= 500;
    }

    // ------------------------------------------------------------------ 其它

    /**
     * {@code GET /v1/models}：只暴露配置里声明过的模型名，不透出上游清单。
     */
    public void listModels(HttpRequest request) {
        GatewayConfig config = store.current();
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"object\":\"list\",\"data\":[");
        boolean first = true;
        for (Map.Entry<String, List<ModelRoute>> entry : config.allRoutes().entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"id\":\"").append(entry.getKey())
                    .append("\",\"object\":\"model\",\"owned_by\":\"feat-ai-gateway\"}");
        }
        sb.append("]}");
        Responder.ok(request, sb.toString());
    }

    private static byte[] readBody(HttpRequest request) throws IOException {
        long contentLength = request.getContentLength();
        BodyInputStream in = request.getInputStream();
        if (contentLength > MAX_REQUEST_BODY) {
            throw new IllegalArgumentException("请求体过大: " + contentLength + " bytes");
        }
        if (contentLength > 0) {
            byte[] buf = new byte[(int) contentLength];
            int offset = 0;
            while (offset < buf.length) {
                int read = in.read(buf, offset, buf.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            if (offset != buf.length) {
                byte[] smaller = new byte[offset];
                System.arraycopy(buf, 0, smaller, 0, offset);
                return smaller;
            }
            return buf;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] buf = new byte[4096];
        int read;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            if (out.size() > MAX_REQUEST_BODY) {
                throw new IllegalArgumentException("请求体过大");
            }
        }
        return out.toByteArray();
    }

    private static boolean clientGone(Ctx ctx) {
        if (ctx.request instanceof HttpEndpoint) {
            AioSession session = ((HttpEndpoint) ctx.request).getAioSession();
            return session == null || session.isInvalid();
        }
        return false;
    }

    private static final class Ctx {
        private final HttpRequest request;
        private final KeyPolicy key;
        private final String path;
        private final String model;
        private final boolean stream;
        private final byte[] rawBody;
        private final JSONObject payload;
        private final List<ModelRoute> routes;
        private final long startMs;
        private final long startNanos;
        private final String traceId;
        private final CompletableFuture<Void> future;

        private final AtomicBoolean finished = new AtomicBoolean();

        private int index;
        private volatile String provider;
        private volatile int upstreamStatus;
        private volatile boolean committed;
        private volatile long firstByteNanos = -1;
        private volatile long ttftNanos = -1;
        private volatile long bytesOut;
        private volatile long promptTokens;
        private volatile long completionTokens;
        private volatile String lastError;
        private volatile HttpRest current;
        private volatile UpstreamPool.Lease lease;
        private ByteArrayOutputStream errBuf;
        private final ByteArrayOutputStream bodyBuf;

        private Ctx(HttpRequest request, KeyPolicy key, String path, String model, boolean stream,
                    byte[] rawBody, JSONObject payload, List<ModelRoute> routes, long startMs, long startNanos,
                    CompletableFuture<Void> future) {
            this.request = request;
            this.key = key;
            this.path = path;
            this.model = model;
            this.stream = stream;
            this.rawBody = rawBody;
            this.payload = payload;
            this.routes = routes;
            this.startMs = startMs;
            this.startNanos = startNanos;
            this.future = future;
            this.traceId = Long.toHexString(ThreadLocalRandom.current().nextLong() & 0xffffffffL);
            this.bodyBuf = stream ? null : new ByteArrayOutputStream(2048);
        }
    }
}
