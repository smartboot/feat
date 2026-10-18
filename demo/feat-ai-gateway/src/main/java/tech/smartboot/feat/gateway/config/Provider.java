package tech.smartboot.feat.gateway.config;

/**
 * 一个上游模型服务商。不可变对象，只描述"怎么连"，不持有连接。
 */
public final class Provider {
    private final String name;
    private final String baseUrl;
    private final String apiKey;
    private final int maxConnections;
    private final int connectTimeoutMs;
    private final int idleTimeoutMs;
    private final String authHeader;
    private final String authPrefix;

    public Provider(String name, String baseUrl, String apiKey, int maxConnections, int connectTimeoutMs,
                    int idleTimeoutMs, String authHeader, String authPrefix) {
        this.name = name;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey;
        this.maxConnections = maxConnections;
        this.connectTimeoutMs = connectTimeoutMs;
        this.idleTimeoutMs = idleTimeoutMs;
        this.authHeader = authHeader == null || authHeader.isEmpty() ? "Authorization" : authHeader;
        this.authPrefix = authPrefix == null ? "Bearer " : authPrefix;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String v = url.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    public String name() {
        return name;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public int maxConnections() {
        return maxConnections;
    }

    public int connectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int idleTimeoutMs() {
        return idleTimeoutMs;
    }

    public String authHeader() {
        return authHeader;
    }

    public String authValue() {
        return apiKey.isEmpty() ? "" : authPrefix + apiKey;
    }

    /**
     * 连接池的复用标识：只有这些项完全一致，才能共用同一个 HttpClient。
     */
    public String fingerprint() {
        return name + '|' + baseUrl + '|' + maxConnections + '|' + idleTimeoutMs + '|' + apiKey.hashCode();
    }
}
