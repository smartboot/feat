package tech.smartboot.feat.gateway.admin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.gateway.config.ConfigException;
import tech.smartboot.feat.gateway.config.ConfigLoader;
import tech.smartboot.feat.gateway.config.ConfigStore;
import tech.smartboot.feat.gateway.config.GatewayConfig;
import tech.smartboot.feat.gateway.config.KeyPolicy;
import tech.smartboot.feat.gateway.config.ModelRoute;
import tech.smartboot.feat.gateway.config.Provider;
import tech.smartboot.feat.gateway.metric.GatewayMetrics;
import tech.smartboot.feat.gateway.metric.UsageLog;
import tech.smartboot.feat.gateway.policy.QuotaGuard;
import tech.smartboot.feat.gateway.proxy.Responder;
import tech.smartboot.feat.gateway.proxy.UpstreamPool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * 管理接口 = 将来控制面的全部后端。
 * <p>
 * 现在没有 UI，但这几个 API 就是 UI 的合同：POST /admin/config 的语义和"改文件重载"完全一致，
 * 控制面只是在替用户写这份 YAML，而不是发明另一套配置逻辑。
 */
public final class AdminApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminApi.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int MAX_CONFIG_BODY = 2 * 1024 * 1024;

    private final ConfigStore store;
    private final GatewayMetrics metrics;
    private final QuotaGuard quota;
    private final UsageLog usageLog;
    private final UpstreamPool pool;

    public AdminApi(ConfigStore store, GatewayMetrics metrics, QuotaGuard quota,
                    UsageLog usageLog, UpstreamPool pool) {
        this.store = store;
        this.metrics = metrics;
        this.quota = quota;
        this.usageLog = usageLog;
        this.pool = pool;
    }

    /**
     * @return true 表示该请求已被管理接口处理
     */
    public boolean handle(HttpRequest request) {
        String path = request.getRequestURI();
        if ("/healthz".equals(path)) {
            Responder.ok(request, "{\"status\":\"ok\"}");
            return true;
        }
        if (!path.startsWith("/admin/") && !"/metrics".equals(path)) {
            return false;
        }
        if (!authorized(request)) {
            Responder.error(request, 401, "admin token required", "authentication_error", "admin_unauthorized");
            return true;
        }

        if ("/metrics".equals(path)) {
            writeText(request, metrics.prometheus());
            return true;
        }
        if ("/admin/config".equals(path)) {
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                Responder.ok(request, configJson().toJSONString());
            } else if ("POST".equalsIgnoreCase(request.getMethod())) {
                applyConfig(request);
            } else {
                Responder.error(request, 405, "method not allowed", "invalid_request_error", "method_not_allowed");
            }
            return true;
        }
        if ("/admin/reload".equals(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            try {
                store.reloadFromFile();
                Responder.ok(request, "{\"version\":" + store.revision() + "}");
            } catch (ConfigException e) {
                Responder.error(request, 400, e.getMessage(), "invalid_config", "config_invalid");
            }
            return true;
        }
        if ("/admin/stats".equals(path) && "GET".equalsIgnoreCase(request.getMethod())) {
            Responder.ok(request, statsJson().toJSONString());
            return true;
        }
        Responder.error(request, 404, "unknown admin endpoint: " + path, "invalid_request_error", "unknown_endpoint");
        return true;
    }

    // ------------------------------------------------------------------

    private void applyConfig(HttpRequest request) {
        String ifMatch = request.getHeader("If-Match");
        long current = store.revision();
        if (ifMatch != null && !ifMatch.trim().isEmpty() && !"*".equals(ifMatch.trim())) {
            try {
                long expected = Long.parseLong(ifMatch.replace("\"", "").trim());
                if (expected != current) {
                    Responder.error(request, 409,
                            "配置版本冲突，当前版本 " + current + "，请重新拉取后再提交",
                            "conflict", "version_mismatch");
                    return;
                }
            } catch (NumberFormatException e) {
                Responder.error(request, 400, "If-Match 必须是版本号", "invalid_request_error", "invalid_if_match");
                return;
            }
        }
        String yaml;
        try {
            yaml = readText(request);
        } catch (IOException e) {
            Responder.error(request, 400, "读取请求体失败: " + e.getMessage(), "invalid_request_error", "body_read_error");
            return;
        }
        try {
            GatewayConfig config = ConfigLoader.load(yaml);
            long version = store.apply(config);
            Responder.ok(request, "{\"version\":" + version + "}");
        } catch (ConfigException e) {
            Responder.error(request, 400, e.getMessage(), "invalid_config", "config_invalid");
        }
    }

    private JSONObject configJson() {
        GatewayConfig config = store.current();
        JSONObject root = new JSONObject();
        root.put("version", store.revision());
        root.put("port", config.port());
        root.put("idleTimeoutMs", config.idleTimeoutMs());

        JSONArray providers = new JSONArray();
        for (Provider provider : config.providers().values()) {
            JSONObject node = new JSONObject();
            node.put("name", provider.name());
            node.put("baseUrl", provider.baseUrl());
            // 密钥永远不回显，只告诉调用方有没有配
            node.put("apiKeyConfigured", !provider.apiKey().isEmpty());
            node.put("maxConnections", provider.maxConnections());
            providers.add(node);
        }
        root.put("providers", providers);

        JSONArray models = new JSONArray();
        for (Map.Entry<String, List<ModelRoute>> entry : config.allRoutes().entrySet()) {
            for (ModelRoute route : entry.getValue()) {
                JSONObject node = new JSONObject();
                node.put("name", route.name());
                node.put("provider", route.provider());
                node.put("upstream", route.upstream());
                node.put("priority", route.priority());
                models.add(node);
            }
        }
        root.put("models", models);

        JSONArray keys = new JSONArray();
        for (KeyPolicy key : config.keys()) {
            JSONObject node = new JSONObject();
            node.put("id", key.id());
            node.put("keyHash", key.keyHash().substring(0, Math.min(16, key.keyHash().length())) + "...");
            node.put("models", key.models());
            node.put("rpm", key.rpm());
            node.put("tpm", key.tpm());
            node.put("monthlyTokens", key.monthlyTokens());
            keys.add(node);
        }
        root.put("keys", keys);
        return root;
    }

    private JSONObject statsJson() {
        GatewayConfig config = store.current();
        JSONObject root = new JSONObject();
        root.put("uptimeMs", metrics.uptimeMs());
        root.put("configVersion", store.revision());
        root.put("requests", metrics.requests());
        root.put("errors", metrics.errors());
        root.put("inflight", metrics.inflight());
        root.put("qpsAvg60s", metrics.qps());
        root.put("promptTokens", metrics.promptTokens());
        root.put("completionTokens", metrics.completionTokens());
        root.put("estimatedRequests", metrics.estimatedCount());

        JSONObject usage = new JSONObject();
        usage.put("written", usageLog.written());
        usage.put("dropped", usageLog.dropped());
        root.put("usageLog", usage);
        root.put("upstreamPools", pool.size());

        JSONObject models = new JSONObject();
        for (Map.Entry<String, LongAdder> entry : metrics.modelRequests().entrySet()) {
            models.put(entry.getKey(), entry.getValue().sum());
        }
        root.put("requestsByModel", models);

        JSONObject keys = new JSONObject();
        for (KeyPolicy key : config.keys()) {
            JSONObject node = new JSONObject();
            node.put("monthlyTokens", quota.monthlyTokens(key.id()));
            node.put("windowTokens", quota.windowTokens(key.id()));
            node.put("rpm", key.rpm());
            node.put("tpm", key.tpm());
            node.put("monthlyLimit", key.monthlyTokens());
            keys.put(key.id(), node);
        }
        root.put("keys", keys);
        return root;
    }

    // ------------------------------------------------------------------

    private boolean authorized(HttpRequest request) {
        String expected = store.current().adminToken();
        if (expected.isEmpty()) {
            // 没配 token 时只允许本机访问，避免管理接口裸奔在公网
            String remote = request.getRemoteAddr();
            return "127.0.0.1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote) || "::1".equals(remote);
        }
        String actual = request.getHeader("X-Admin-Token");
        if (actual == null) {
            String query = request.getParameter("token");
            actual = query;
        }
        return expected.equals(actual);
    }

    private static void writeText(HttpRequest request, String body) {
        try {
            byte[] bytes = body.getBytes(UTF8);
            request.getResponse().setHeader("Content-Type", "text/plain;version=0.0.4;charset=utf-8");
            request.getResponse().setHeader("Content-Length", String.valueOf(bytes.length));
            request.getResponse().write(bytes);
        } catch (IOException e) {
            LOGGER.debug("输出 metrics 失败: {}", e.getMessage());
        } finally {
            request.getResponse().close();
        }
    }

    private static String readText(HttpRequest request) throws IOException {
        long length = request.getContentLength();
        if (length > MAX_CONFIG_BODY) {
            throw new IOException("配置体过大: " + length);
        }
        InputStream in = request.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream(length > 0 ? (int) length : 4096);
        byte[] buf = new byte[8192];
        int read;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            if (out.size() > MAX_CONFIG_BODY) {
                throw new IOException("配置体过大");
            }
        }
        return new String(out.toByteArray(), UTF8);
    }
}
