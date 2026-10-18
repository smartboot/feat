package tech.smartboot.feat.gateway.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 一份完整配置快照。
 * <p>
 * 铁律：proxy / policy / metric 三个包只读这个对象，永远不知道它是从文件、etcd 还是控制面来的。
 * 快照一旦构建完成就不可变，热加载是"整份替换"，不是"改字段"。
 */
public final class GatewayConfig {
    private final int version;
    private final int port;
    private final long idleTimeoutMs;
    private final String adminToken;
    private final String logDir;
    private final boolean usageEnabled;
    private final Map<String, Provider> providers;
    private final Map<String, List<ModelRoute>> routes;
    private final List<KeyPolicy> keys;

    public GatewayConfig(int version, int port, long idleTimeoutMs, String adminToken, String logDir,
                         boolean usageEnabled, Map<String, Provider> providers,
                         Map<String, List<ModelRoute>> routes, List<KeyPolicy> keys) {
        this.version = version;
        this.port = port;
        this.idleTimeoutMs = idleTimeoutMs;
        this.adminToken = adminToken == null ? "" : adminToken;
        this.logDir = logDir;
        this.usageEnabled = usageEnabled;
        this.providers = Collections.unmodifiableMap(providers);
        this.routes = Collections.unmodifiableMap(routes);
        this.keys = Collections.unmodifiableList(keys);
    }

    public int version() {
        return version;
    }

    public int port() {
        return port;
    }

    public long idleTimeoutMs() {
        return idleTimeoutMs;
    }

    public String adminToken() {
        return adminToken;
    }

    public String logDir() {
        return logDir;
    }

    public boolean usageEnabled() {
        return usageEnabled;
    }

    public Map<String, Provider> providers() {
        return providers;
    }

    public List<KeyPolicy> keys() {
        return keys;
    }

    public Provider provider(String name) {
        return providers.get(name);
    }

    /**
     * 返回该模型名的路由列表，已按 priority 升序排列。未知模型返回空列表。
     */
    public List<ModelRoute> routes(String model) {
        List<ModelRoute> list = routes.get(model);
        return list == null ? Collections.<ModelRoute>emptyList() : list;
    }

    public Map<String, List<ModelRoute>> allRoutes() {
        return routes;
    }
}
