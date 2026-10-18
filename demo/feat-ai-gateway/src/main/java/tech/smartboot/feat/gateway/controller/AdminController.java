package tech.smartboot.feat.gateway.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
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
 * 现在没有 UI，但这几个 API 就是 UI 的合同：{@code POST /admin/config} 的语义和"改文件重载"完全一致，
 * 控制面只是在替用户写这份 YAML，而不是发明另一套配置逻辑。
 * <p>
 * 全部方法返回 {@code void} 并自己写响应：生成的同步处理器在方法返回后完成 future，
 * 这时 body 已经落盘，语义就是"处理完毕"。
 */
@Controller
public class AdminController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int MAX_CONFIG_BODY = 2 * 1024 * 1024;

    @Autowired
    private ConfigStore configStore;

    @Autowired
    private GatewayMetrics gatewayMetrics;

    @Autowired
    private QuotaGuard quotaGuard;

    @Autowired
    private UsageLog usageLog;

    @Autowired
    private UpstreamPool upstreamPool;

    public AdminController() {
    }

    public void setConfigStore(ConfigStore configStore) {
        this.configStore = configStore;
    }

    public void setGatewayMetrics(GatewayMetrics gatewayMetrics) {
        this.gatewayMetrics = gatewayMetrics;
    }

    public void setQuotaGuard(QuotaGuard quotaGuard) {
        this.quotaGuard = quotaGuard;
    }

    public void setUsageLog(UsageLog usageLog) {
        this.usageLog = usageLog;
    }

    public void setUpstreamPool(UpstreamPool upstreamPool) {
        this.upstreamPool = upstreamPool;
    }

    // ------------------------------------------------------------------ 探针

    @RequestMapping(value = "/healthz", method = RequestMethod.GET)
    public void health(HttpRequest request) {
        Responder.ok(request, "{\"status\":\"ok\"}");
    }

    @RequestMapping(value = "/metrics", method = RequestMethod.GET)
    public void metrics(HttpRequest request) {
        if (!authorized(request)) {
            deny(request);
            return;
        }
        writeText(request, gatewayMetrics.prometheus());
    }

    // ------------------------------------------------------------------ 配置

    @RequestMapping(value = "/admin/config", method = RequestMethod.GET)
    public void getConfig(HttpRequest request) {
        if (!authorized(request)) {
            deny(request);
            return;
        }
        Responder.ok(request, configJson().toJSONString());
    }

    /**
     * 提交新快照。{@code If-Match: <version>} 做乐观锁，冲突返回 409 —— 控制面据此提示"别人改过了"。
     */
    @RequestMapping(value = "/admin/config", method = RequestMethod.POST)
    public void applyConfig(HttpRequest request) {
        if (!authorized(request)) {
            deny(request);
            return;
        }
        String ifMatch = request.getHeader("If-Match");
        long current = configStore.revision();
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
            Responder.ok(request, "{\"version\":" + configStore.apply(config) + "}");
        } catch (ConfigException e) {
            Responder.error(request, 400, e.getMessage(), "invalid_config", "config_invalid");
        }
    }

    @RequestMapping(value = "/admin/reload", method = RequestMethod.POST)
    public void reload(HttpRequest request) {
        if (!authorized(request)) {
            deny(request);
            return;
        }
        try {
            configStore.reloadFromFile();
            Responder.ok(request, "{\"version\":" + configStore.revision() + "}");
        } catch (ConfigException e) {
            Responder.error(request, 400, e.getMessage(), "invalid_config", "config_invalid");
        }
    }

    @RequestMapping(value = "/admin/stats", method = RequestMethod.GET)
    public void stats(HttpRequest request) {
        if (!authorized(request)) {
            deny(request);
            return;
        }
        Responder.ok(request, statsJson().toJSONString());
    }

    // ------------------------------------------------------------------ 内部

    private boolean authorized(HttpRequest request) {
        String expected = configStore.current().adminToken();
        if (expected.isEmpty()) {
            // 没配 token 时只允许本机访问，避免管理接口裸奔在公网
            String remote = request.getRemoteAddr();
            return "127.0.0.1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote) || "::1".equals(remote);
        }
        String actual = request.getHeader("X-Admin-Token");
        if (actual == null) {
            actual = request.getParameter("token");
        }
        return expected.equals(actual);
    }

    private static void deny(HttpRequest request) {
        Responder.error(request, 401, "admin token required", "authentication_error", "admin_unauthorized");
    }

    private JSONObject configJson() {
        GatewayConfig config = configStore.current();
        JSONObject root = new JSONObject();
        root.put("version", configStore.revision());
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
        GatewayConfig config = configStore.current();
        JSONObject root = new JSONObject();
        root.put("uptimeMs", gatewayMetrics.uptimeMs());
        root.put("configVersion", configStore.revision());
        root.put("requests", gatewayMetrics.requests());
        root.put("errors", gatewayMetrics.errors());
        root.put("inflight", gatewayMetrics.inflight());
        root.put("qpsAvg60s", gatewayMetrics.qps());
        root.put("promptTokens", gatewayMetrics.promptTokens());
        root.put("completionTokens", gatewayMetrics.completionTokens());
        root.put("estimatedRequests", gatewayMetrics.estimatedCount());

        JSONObject usage = new JSONObject();
        usage.put("written", usageLog.written());
        usage.put("dropped", usageLog.dropped());
        root.put("usageLog", usage);
        root.put("upstreamPools", upstreamPool.size());

        JSONObject models = new JSONObject();
        for (Map.Entry<String, LongAdder> entry : gatewayMetrics.modelRequests().entrySet()) {
            models.put(entry.getKey(), entry.getValue().sum());
        }
        root.put("requestsByModel", models);

        JSONObject keys = new JSONObject();
        for (KeyPolicy key : config.keys()) {
            JSONObject node = new JSONObject();
            node.put("monthlyTokens", quotaGuard.monthlyTokens(key.id()));
            node.put("windowTokens", quotaGuard.windowTokens(key.id()));
            node.put("rpm", key.rpm());
            node.put("tpm", key.tpm());
            node.put("monthlyLimit", key.monthlyTokens());
            keys.put(key.id(), node);
        }
        root.put("keys", keys);
        return root;
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
