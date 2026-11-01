package tech.smartboot.feat.demo.plus.cache;

/**
 * 缓存运行计数快照。
 * <p>
 * 只做展示与排障，不参与任何判断。命中率算的是本进程启动以来的累计值，
 * 不按时间窗口滑动——窗口统计需要一个后台采样线程，对缓存自身的开销不划算。
 * <p>
 * {@code hitRate} 必须是一个真实字段，不能只在 getter 里算：Feat Cloud 在编译期把返回值
 * 直接拼成 JSON 字节，逐字段调用 getter，<b>没有对应字段的计算属性会被整条丢掉</b>，
 * 接口里静默少一个键，前端只会拿到 undefined。
 */
public class CacheStats {

    private final String provider;

    private final long hits;

    private final long misses;

    private final long puts;

    private final long evictions;

    private final double hitRate;

    public CacheStats(String provider, long hits, long misses, long puts, long evictions) {
        this.provider = provider;
        this.hits = hits;
        this.misses = misses;
        this.puts = puts;
        this.evictions = evictions;
        long total = hits + misses;
        // 保留三位小数；一次都没查过时返回 0，避免除零。
        this.hitRate = total == 0 ? 0 : Math.round(hits * 1000.0 / total) / 1000.0;
    }

    public String getProvider() {
        return provider;
    }

    public long getHits() {
        return hits;
    }

    public long getMisses() {
        return misses;
    }

    public long getPuts() {
        return puts;
    }

    public long getEvictions() {
        return evictions;
    }

    /**
     * 命中率，保留三位小数。
     */
    public double getHitRate() {
        return hitRate;
    }
}
