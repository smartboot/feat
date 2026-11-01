package tech.smartboot.feat.demo.plus.cache;

import java.util.Locale;

import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.Value;

/**
 * 按配置选缓存后端，对上层只暴露 {@link CacheStore}。
 * <p>
 * 配置全部来自 {@code feat.yaml} 的 {@code feat-plus.cache} 块，一处改动影响全局——
 * 业务代码里不该再出现 second 个「缓存开关」。默认内存，改 {@code provider: redis} 即切到 Redis。
 * <p>
 * 三条刻意的选择：
 * <ul>
 *   <li><b>配置项写错直接启动失败</b>，不静默降级到内存。缓存降级后应用照常工作，只是慢，
 *       没人会发现配置根本没生效；而「配了 Redis 却是内存在跑」在多实例部署下会表现为
 *       「偶发读到旧数据」，比启动失败难查一个数量级。</li>
 *   <li><b>启用 Redis 时先探活</b>，连不上就抛异常。缓存是加速手段，但配了 Redis 就是明确的部署意图，
 *       连不上属于部署错误，应该在发布窗口暴露，而不是等第一个请求。</li>
 *   <li><b>启动时打印实际后端</b>，因为上面两条只能挡住一部分配置错误，剩下的靠日志核对。</li>
 * </ul>
 * <p>
 * Bean 名必须是 {@code cacheStore}：Feat 的 {@code @Autowired} 按字段名匹配 Bean，
 * 其他组件里字段写成 {@code private CacheStore cacheStore} 才能注入到这里。
 */
@Bean("cacheStore")
public class CacheStoreProvider implements CacheStore {

    private static final String PROVIDER_MEMORY = "memory";

    private static final String PROVIDER_REDIS = "redis";

    @Value("${feat-plus.cache.provider:memory}")
    private String provider;

    @Value("${feat-plus.cache.default-ttl-seconds:300}")
    private String defaultTtlSeconds;

    @Value("${feat-plus.cache.key-prefix:plus:cache:}")
    private String keyPrefix;

    @Value("${feat-plus.cache.redis.address}")
    private String redisAddress;

    @Value("${feat-plus.cache.redis.username}")
    private String redisUsername;

    @Value("${feat-plus.cache.redis.password}")
    private String redisPassword;

    @Value("${feat-plus.cache.redis.database:0}")
    private String redisDatabase;

    private CacheStore delegate;

    @PostConstruct
    public void init() {
        String name = provider == null ? PROVIDER_MEMORY : provider.trim().toLowerCase(Locale.ROOT);
        int ttl = parseTtl();

        if (PROVIDER_MEMORY.equals(name)) {
            delegate = new MemoryCacheStore(ttl);
            System.out.println("[feat-plus] cache provider=memory defaultTtl=" + ttl + "s");
            return;
        }
        if (PROVIDER_REDIS.equals(name)) {
            if (redisAddress == null || redisAddress.trim().isEmpty()) {
                throw new IllegalStateException(
                        "feat-plus.cache.provider=redis，但 feat-plus.cache.redis.address 未配置");
            }
            RedisCacheStore redis = new RedisCacheStore(redisAddress.trim(), redisUsername, redisPassword,
                    parseDatabase(), keyPrefix, ttl);
            try {
                redis.ping();
            } catch (RuntimeException e) {
                throw new IllegalStateException("feat-plus 缓存连不上 Redis：" + redisAddress.trim()
                        + "（" + e.getMessage() + "）", e);
            }
            delegate = redis;
            System.out.println("[feat-plus] cache provider=redis address=" + redisAddress.trim()
                    + " database=" + parseDatabase() + " prefix=" + keyPrefix + " defaultTtl=" + ttl + "s");
            return;
        }
        throw new IllegalStateException(
                "未知的缓存后端：feat-plus.cache.provider=" + provider + "，可选值为 memory / redis");
    }

    private int parseTtl() {
        if (defaultTtlSeconds == null || defaultTtlSeconds.trim().isEmpty()) {
            return 300;
        }
        try {
            int ttl = Integer.parseInt(defaultTtlSeconds.trim());
            if (ttl <= 0) {
                throw new IllegalStateException(
                        "feat-plus.cache.default-ttl-seconds 必须大于 0，当前为 " + defaultTtlSeconds);
            }
            return ttl;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "feat-plus.cache.default-ttl-seconds 不是合法整数：" + defaultTtlSeconds, e);
        }
    }

    private int parseDatabase() {
        if (redisDatabase == null || redisDatabase.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(redisDatabase.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "feat-plus.cache.redis.database 不是合法整数：" + redisDatabase, e);
        }
    }

    private CacheStore delegate() {
        if (delegate == null) {
            // 正常路径下 @PostConstruct 已经跑过；走到这里说明有人在构造阶段就调了缓存，
            // 直接报错比返回一个空缓存好——后者会静默降低性能，看起来一切正常。
            throw new IllegalStateException("缓存尚未初始化，不能在 @PostConstruct 之前使用 CacheStore");
        }
        return delegate;
    }

    @Override
    public String get(String key) {
        return delegate().get(key);
    }

    @Override
    public void put(String key, String value, int ttlSeconds) {
        delegate().put(key, value, ttlSeconds);
    }

    @Override
    public boolean evict(String key) {
        return delegate().evict(key);
    }

    @Override
    public CacheStats stats() {
        return delegate().stats();
    }

    @Override
    public String providerName() {
        return delegate().providerName();
    }

    @PreDestroy
    public void destroy() {
        if (delegate instanceof RedisCacheStore) {
            ((RedisCacheStore) delegate).close();
        }
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setDefaultTtlSeconds(String defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public void setRedisAddress(String redisAddress) {
        this.redisAddress = redisAddress;
    }

    public void setRedisUsername(String redisUsername) {
        this.redisUsername = redisUsername;
    }

    public void setRedisPassword(String redisPassword) {
        this.redisPassword = redisPassword;
    }

    public void setRedisDatabase(String redisDatabase) {
        this.redisDatabase = redisDatabase;
    }
}
