package tech.smartboot.feat.demo.plus.cache;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内缓存，默认后端。
 * <p>
 * 过期采用「惰性删除 + 定量清扫」：读的时候顺手判断过期，另外每写满 {@value #SWEEP_INTERVAL}
 * 次做一次全量扫描。只用惰性删除会出事——写了一万个键、之后再没被读过一次，
 * 这些键就永远留在堆里，表现为内存慢慢涨、GC 越来越频繁，而缓存本身「看起来没占多少」。
 * <p>
 * 不启动后台线程：缓存是给业务加速的，不该反过来给应用引入一条常驻线程和它的启停顺序问题。
 */
public class MemoryCacheStore implements CacheStore {

    /**
     * 每写多少次触发一次全量清扫。取值偏大是有意的：清扫是 O(n) 的，太频繁会把
     * 缓存变成负优化；偏小则过期键堆积。256 在几百到几万键的规模下都还合适。
     */
    private static final int SWEEP_INTERVAL = 256;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    private final int defaultTtlSeconds;

    private final AtomicLong hits = new AtomicLong();

    private final AtomicLong misses = new AtomicLong();

    private final AtomicLong puts = new AtomicLong();

    private final AtomicLong evictions = new AtomicLong();

    private final AtomicInteger writesSinceSweep = new AtomicInteger();

    public MemoryCacheStore(int defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    @Override
    public String get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            misses.incrementAndGet();
            return null;
        }
        if (entry.expireAtMillis <= System.currentTimeMillis()) {
            // 带值删除：并发下这个键可能刚被别的线程替换成新值，不能无条件 remove。
            entries.remove(key, entry);
            misses.incrementAndGet();
            return null;
        }
        hits.incrementAndGet();
        return entry.value;
    }

    @Override
    public void put(String key, String value, int ttlSeconds) {
        int ttl = ttlSeconds > 0 ? ttlSeconds : defaultTtlSeconds;
        entries.put(key, new Entry(value, System.currentTimeMillis() + ttl * 1000L));
        puts.incrementAndGet();
        if (writesSinceSweep.incrementAndGet() >= SWEEP_INTERVAL) {
            writesSinceSweep.set(0);
            sweepExpired();
        }
    }

    @Override
    public boolean evict(String key) {
        boolean removed = entries.remove(key) != null;
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
        return "memory";
    }

    /**
     * 当前存活的键数量，仅用于测试与排障。
     */
    public int size() {
        return entries.size();
    }

    private void sweepExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAtMillis <= now) {
                it.remove();
            }
        }
    }

    private static final class Entry {
        private final String value;

        private final long expireAtMillis;

        private Entry(String value, long expireAtMillis) {
            this.value = value;
            this.expireAtMillis = expireAtMillis;
        }
    }
}
