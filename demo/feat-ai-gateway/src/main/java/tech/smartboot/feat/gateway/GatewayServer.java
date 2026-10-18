package tech.smartboot.feat.gateway;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.gateway.admin.AdminApi;
import tech.smartboot.feat.gateway.config.ConfigException;
import tech.smartboot.feat.gateway.config.ConfigLoader;
import tech.smartboot.feat.gateway.config.ConfigStore;
import tech.smartboot.feat.gateway.config.GatewayConfig;
import tech.smartboot.feat.gateway.config.KeyHash;
import tech.smartboot.feat.gateway.config.KeyPolicy;
import tech.smartboot.feat.gateway.config.ModelRoute;
import tech.smartboot.feat.gateway.metric.GatewayMetrics;
import tech.smartboot.feat.gateway.metric.UsageLog;
import tech.smartboot.feat.gateway.policy.AuthPolicy;
import tech.smartboot.feat.gateway.policy.QuotaGuard;
import tech.smartboot.feat.gateway.proxy.ChatProxy;
import tech.smartboot.feat.gateway.proxy.Responder;
import tech.smartboot.feat.gateway.proxy.UpstreamPool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 装配入口。数据面只有一个进程、一个 jar，没有数据库、没有外部依赖。
 * <p>
 * 启动：
 * <pre>
 *   java -jar feat-ai-gateway.jar gateway.yaml
 *   java -jar feat-ai-gateway.jar hash sk-your-key     # 生成配置里的 keyHash
 * </pre>
 */
public final class GatewayServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayServer.class);

    private ConfigStore store;
    private UpstreamPool pool;
    private UsageLog usageLog;
    private ChatProxy proxy;
    private AdminApi admin;
    private HttpServer server;

    public static void main(String[] args) {
        if (args.length > 0 && "hash".equals(args[0])) {
            if (args.length < 2) {
                System.out.println("用法: java -jar feat-ai-gateway.jar hash <token>");
                return;
            }
            String hash = KeyHash.sha256Hex(args[1]);
            System.out.println("keyHash: sha256:" + hash.substring(0, 16));
            System.out.println("full:    " + hash);
            return;
        }
        try {
            new GatewayServer().start(args);
        } catch (ConfigException e) {
            System.err.println("[feat-ai-gateway] 配置错误: " + e.getMessage());
            System.exit(2);
        } catch (Throwable t) {
            System.err.println("[feat-ai-gateway] 启动失败: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    public void start(String[] args) throws Exception {
        Path configPath = resolveConfigPath(args);
        if (!Files.exists(configPath)) {
            throw new ConfigException("配置文件不存在: " + configPath.toAbsolutePath());
        }
        GatewayConfig config = ConfigLoader.loadFile(configPath);

        store = new ConfigStore(configPath, config);
        pool = new UpstreamPool();
        store.addListener(new ConfigStore.Listener() {
            @Override
            public void onApply(GatewayConfig applied) {
                pool.retireUnused(applied);
            }
        });

        QuotaGuard quota = new QuotaGuard();
        GatewayMetrics metrics = new GatewayMetrics();
        usageLog = new UsageLog(config.logDir(), config.usageEnabled());
        proxy = new ChatProxy(store, pool, quota, metrics, usageLog);
        admin = new AdminApi(store, metrics, quota, usageLog, pool);

        ServerOptions options = new ServerOptions();
        options.port(config.port());
        if (config.idleTimeoutMs() > 0) {
            options.setIdleTimeout(config.idleTimeoutMs());
        }
        options.setMaxRequestSize(16L * 1024 * 1024);
        options.shutdownHook(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        });

        server = Feat.httpServer(options).httpHandler(new HttpHandler() {
            @Override
            public void handle(HttpRequest request) {
                dispatch(request, new CompletableFuture<Void>());
            }

            @Override
            public void handle(HttpRequest request, CompletableFuture<Void> future) {
                dispatch(request, future);
            }

            @Override
            public void onClose(HttpEndpoint endpoint) {
                // 连接断开 = 调用方放弃了这个回答，立刻取消上游，别继续烧 token
                proxy.onConnectionClose(endpoint);
            }
        });
        server.listen(config.port());
        store.startWatch(1000);
        printSummary(config, configPath);
    }

    private void dispatch(HttpRequest request, CompletableFuture<Void> future) {
        try {
            if (admin.handle(request)) {
                future.complete(null);
                return;
            }
            String path = request.getRequestURI();
            if (!ChatProxy.isProxyPath(path) && !ChatProxy.PATH_MODELS.equals(path)) {
                Responder.error(request, 404, "unknown endpoint: " + path, "invalid_request_error", "unknown_endpoint");
                future.complete(null);
                return;
            }
            KeyPolicy key = AuthPolicy.authenticate(store.current(), request.getHeader("Authorization"));
            if (key == null) {
                request.getResponse().setHeader("WWW-Authenticate", "Bearer");
                Responder.error(request, 401, "invalid or missing api key", "authentication_error", "invalid_api_key");
                future.complete(null);
                return;
            }
            proxy.handle(request, key, future);
        } catch (Throwable t) {
            LOGGER.error("请求处理异常: {}", t.getMessage(), t);
            try {
                Responder.error(request, 500, "gateway internal error: " + t.getMessage(),
                        "server_error", "internal_error");
            } catch (Throwable ignored) {
                // 响应可能已经提交，这里只能忽略
            } finally {
                future.complete(null);
            }
        }
    }

    private static Path resolveConfigPath(String[] args) {
        if (args.length > 0 && !args[0].startsWith("-")) {
            return Paths.get(args[0]);
        }
        String env = System.getenv("FEAT_GW_CONFIG");
        return Paths.get(env == null || env.isEmpty() ? "gateway.yaml" : env);
    }

    private void printSummary(GatewayConfig config, Path configPath) {
        System.out.println();
        System.out.println("  feat-ai-gateway 0.1.0  (data plane)");
        System.out.println("  config   : " + configPath.toAbsolutePath());
        System.out.println("  listen   : http://0.0.0.0:" + config.port());
        System.out.println("  providers: " + config.providers().size() + "  models: " + config.allRoutes().size()
                + "  keys: " + config.keys().size());
        for (Map.Entry<String, List<ModelRoute>> entry : config.allRoutes().entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (ModelRoute route : entry.getValue()) {
                if (sb.length() > 0) {
                    sb.append(" > ");
                }
                sb.append(route.provider()).append('/').append(route.upstream());
            }
            System.out.println("    " + entry.getKey() + "  ->  " + sb);
        }
        System.out.println("  admin    : " + (config.adminToken().isEmpty()
                ? "local only (set admin.token or FEAT_GW_ADMIN_TOKEN to enable remote)"
                : "token protected"));
        System.out.println();
    }

    public void shutdown() {
        if (store != null) {
            store.stopWatch();
        }
        if (pool != null) {
            pool.closeAll();
        }
        if (usageLog != null) {
            usageLog.close();
        }
    }
}
