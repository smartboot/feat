package tech.smartboot.feat.demo.plus.cache;

import java.util.concurrent.atomic.AtomicLong;

import tech.smartboot.redisun.Redisun;

/**
 * Redis 缓存后端，基于 Feat 生态自带的 {@code redisun} 客户端，不引入 jedis / lettuce。
 * <p>
 * 键统一带前缀，多套环境共用一个 Redis 时不会互相覆盖，也便于用 {@code redis-cli --scan}
 * 找出本应用的键。前缀不是可选项：不带前缀的多实例部署，两套环境一旦共用 db 就会互相读到对方的值，
 * 而缓存读错不会报错，只会返回旧数据。
 * <p>
 * 注意计数是<b>本实例</b>的：{@code hits} / {@code misses} 统计的是本进程的调用次数，
 * 不是 Redis 的全局命中率。真正的全局命中率要看 {@code INFO stats}。
 */
public class RedisCacheStore implements CacheStore {

    private final Redisun redisun;

    private final String address;

    private final String keyPrefix;

    private final int defaultTtlSeconds;

    private final AtomicLong hits = new AtomicLong();

    private final AtomicLong misses = new AtomicLong();

    private final AtomicLong puts = new AtomicLong();

    private final AtomicLong evictions = new AtomicLong();

    public RedisCacheStore(String address, String username, String password, int database,
                           String keyPrefix, int defaultTtlSeconds) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Redis 地址为空，无法启用 Redis 缓存");
        }
        this.address = address;
        final String finalUsername = username;
        final String finalPassword = password;
        this.redisun = Redisun.create(opt -> {
            opt.setAddress(address);
            if (finalUsername != null && !finalUsername.isEmpty()) {
                opt.setUsername(finalUsername);
            }
            if (finalPassword != null && !finalPassword.isEmpty()) {
                opt.setPassword(finalPassword);
            }
            opt.setDatabase(database);
        });
        this.keyPrefix = keyPrefix == null || keyPrefix.isEmpty() ? "plus:cache:" : keyPrefix;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    /**
     * 探活。地址写错、密码不对、Redis 没启动，都会在这里抛出来，而不是等第一个业务请求。
     * <p>
     * {@code dbsize} 挑它是因为开销近似 O(1)：能验证「连得上且认证通过」，又不会像 {@code INFO} 那样
     * 触发一次较为完整的服务端统计。
     * <p>
     * 底层抛出的原始异常只有一句 {@code Connection refused}，看不出是哪个服务——配了多个
     * Redis（会话一个、缓存一个）时无法定位，所以这里把地址拼进 message 再抛。
     * <p>
     * <b>刻意不用 {@code new IllegalStateException(msg, e)} 这种带 cause 的写法。</b>
     * Feat 启动失败时抛的是 {@code new FeatException("application start exception", e)}，
     * 而它的构造函数会把 cause 一路解包到最底层再赋值（{@code FeatException#getRootCause}）。
     * 也就是说：中间层的 message 和类型全部丢失，控制台只剩一句 {@code Connection refused}，
     * 这段精心写的提示一个字都打不出来。所以这里把原始信息拼进自己的 message，并且不设 cause，
     * 让 root cause 就是它自己。堆栈信息由启动日志里 FeatException 的帧补足。
     */
    public void ping() {
        try {
            long size = redisun.dbsize();
            if (size < 0) {
                throw new IllegalStateException("feat-plus 缓存连不上 Redis：" + address + "（dbsize 返回负值）");
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("feat-plus 缓存连不上 Redis：" + address
                    + "（" + e.getClass().getSimpleName() + ": " + e.getMessage() + "）");
        }
    }

    @Override
    public String get(String key) {
        String value = redisun.get(keyPrefix + key);
        if (value == null) {
            misses.incrementAndGet();
        } else {
            hits.incrementAndGet();
        }
        return value;
    }

    @Override
    public void put(String key, String value, int ttlSeconds) {
        int ttl = ttlSeconds > 0 ? ttlSeconds : defaultTtlSeconds;
        redisun.set(keyPrefix + key, value, cmd -> cmd.expire(ttl));
        puts.incrementAndGet();
    }

    @Override
    public boolean evict(String key) {
        boolean removed = redisun.del(keyPrefix + key) > 0;
        if (removed) {
            evictions.incrementAndGet();
        }
        return removed;
    }

    @Override
    public CacheStats stats() {
        return new CacheStats(providerName(), hits.get(), misses.get(), puts.get(), evictions.get());
    }

    @Override
    public String providerName() {
        return "redis";
    }

    public void close() {
        redisun.close();
    }
}
