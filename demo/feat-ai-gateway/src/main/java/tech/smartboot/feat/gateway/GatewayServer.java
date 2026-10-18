package tech.smartboot.feat.gateway;

import tech.smartboot.feat.cloud.CloudOptions;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.gateway.config.ConfigException;
import tech.smartboot.feat.gateway.config.ConfigLoader;
import tech.smartboot.feat.gateway.config.ConfigStore;
import tech.smartboot.feat.gateway.config.GatewayConfig;
import tech.smartboot.feat.gateway.config.KeyHash;
import tech.smartboot.feat.gateway.config.ModelRoute;
import tech.smartboot.feat.gateway.metric.GatewayMetrics;
import tech.smartboot.feat.gateway.metric.UsageLog;
import tech.smartboot.feat.gateway.policy.QuotaGuard;
import tech.smartboot.feat.gateway.proxy.ChatProxy;
import tech.smartboot.feat.gateway.proxy.UpstreamPool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 装配入口。数据面只有一个进程、一个 jar，没有数据库、没有外部依赖。
 * <p>
 * 启动：
 * <pre>
 *   java -jar feat-ai-gateway.jar gateway.yaml
 *   java -jar feat-ai-gateway.jar hash sk-your-key     # 生成配置里的 keyHash
 * </pre>
 * <p>
 * 装配分三块：
 * <ul>
 *   <li>{@code controller.GatewayController} —— AI 网关的自定义路由（含转发端点）；</li>
 *   <li>{@code controller.AdminController} —— 管理接口，纯请求-响应；</li>
 *   <li>这里只负责生命周期：读配置、建对象、注册成 Bean、挂关闭钩子。</li>
 * </ul>
 * 路由一行都不在这个类里。
 */
public final class GatewayServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayServer.class);
    /**
     * 与 {@code ChatRoute#MAX_ASYNC_BODY} 对齐：超过这个大小直接 413，不进内存。
     */
    private static final long MAX_REQUEST_SIZE = 16L * 1024 * 1024;

    private ConfigStore store;
    private UpstreamPool pool;
    private UsageLog usageLog;
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
        final Path configPath = resolveConfigPath(args);
        if (!Files.exists(configPath)) {
            throw new ConfigException("配置文件不存在: " + configPath.toAbsolutePath());
        }
        final GatewayConfig config = ConfigLoader.loadFile(configPath);

        store = new ConfigStore(configPath, config);
        pool = new UpstreamPool();
        store.addListener(new ConfigStore.Listener() {
            @Override
            public void onApply(GatewayConfig applied) {
                pool.retireUnused(applied);
            }
        });

        final QuotaGuard quota = new QuotaGuard();
        final GatewayMetrics metrics = new GatewayMetrics();
        usageLog = new UsageLog(config.logDir(), config.usageEnabled());
        final ChatProxy proxy = new ChatProxy(store, pool, quota, metrics, usageLog);

        server = FeatCloud.cloudServer(new java.util.function.Consumer<CloudOptions>() {
            @Override
            public void accept(CloudOptions opt) {
                opt.setPackages("tech.smartboot.feat.gateway");
                opt.setMaxRequestSize(MAX_REQUEST_SIZE);
                if (config.idleTimeoutMs() > 0) {
                    opt.setIdleTimeout(config.idleTimeoutMs());
                }

                // 运行时对象以外部 Bean 的形式交给 Feat Cloud，
                // @Autowired 按字段名取（生成代码用 loadBean(字段名) 查找）
                opt.registerBean("configStore", store);
                opt.registerBean("chatProxy", proxy);
                opt.registerBean("gatewayMetrics", metrics);
                opt.registerBean("quotaGuard", quota);
                opt.registerBean("usageLog", usageLog);
                opt.registerBean("upstreamPool", pool);
                // 自定义路由由 GatewayController#dataPlaneRoutes 注册，它需要拿到这棵路由树
                opt.registerBean("router", opt.getRouter());
            }
        });

        // FeatCloud 已经挂了一个 application.destroy() 的钩子，这里追加不能覆盖它
        final Runnable cloudHook = server.options().shutdownHook();
        server.options().shutdownHook(new Runnable() {
            @Override
            public void run() {
                if (cloudHook != null) {
                    try {
                        cloudHook.run();
                    } catch (Throwable t) {
                        LOGGER.error("应用上下文销毁失败: {}", t.getMessage(), t);
                    }
                }
                shutdown();
            }
        });

        server.listen(config.port());
        store.startWatch(1000);
        printSummary(config, configPath);
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
        System.out.println("  feat-ai-gateway 0.1.0  (data plane, feat-cloud)");
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
