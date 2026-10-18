package tech.smartboot.feat.gateway.proxy;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.gateway.config.GatewayConfig;
import tech.smartboot.feat.gateway.config.Provider;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 上游连接池。
 * <p>
 * HttpClient 建立一次连接成本很高（TCP + TLS），绝不能每个请求 new 一个。
 * 这里按 provider 的 fingerprint 复用，并在配置热加载后回收不再被引用的客户端。
 * <p>
 * 引用计数的意义：配置换掉后旧客户端可能还有在途的流式请求，必须等它们结束才能关，
 * 否则正在输出的回答会被拦腰切断。
 */
public final class UpstreamPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamPool.class);

    /**
     * 所有上游客户端共享一个 I/O 线程组。
     * <p>
     * 这一条是压测压出来的：feat 的 HttpClient 默认自己建 AsynchronousChannelGroup，
     * 意味着每条上游连接都会带一组线程。1000 并发时直接 "unable to create new native thread"。
     * 共享 group 之后，连接数可以远大于线程数——这才是 AIO 该有的样子。
     */
    private static final AsynchronousChannelGroup SHARED_GROUP = createSharedGroup();

    private final Map<String, Entry> entries = new ConcurrentHashMap<String, Entry>();

    private static AsynchronousChannelGroup createSharedGroup() {
        int threads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        ExecutorService executor = Executors.newFixedThreadPool(threads, new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "gw-upstream-io-" + seq.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
        try {
            return AsynchronousChannelGroup.withThreadPool(executor);
        } catch (IOException e) {
            throw new IllegalStateException("创建共享 I/O 线程组失败", e);
        }
    }

    public Lease acquire(Provider provider) {
        Entry entry = entries.get(provider.fingerprint());
        if (entry == null) {
            Entry created = new Entry(createClient(provider));
            Entry existing = entries.putIfAbsent(provider.fingerprint(), created);
            entry = existing != null ? existing : created;
        }
        entry.refs.incrementAndGet();
        return new Lease(entry);
    }

    /**
     * 配置生效后调用：关闭所有不再被新快照引用的客户端。
     */
    public void retireUnused(GatewayConfig config) {
        Set<String> live = new HashSet<String>();
        for (Provider provider : config.providers().values()) {
            live.add(provider.fingerprint());
        }
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            if (live.contains(e.getKey())) {
                continue;
            }
            Entry entry = entries.remove(e.getKey());
            if (entry != null) {
                entry.retired = true;
                entry.closeIfIdle();
                LOGGER.info("回收未再使用的上游连接池: {}", e.getKey());
            }
        }
    }

    public void closeAll() {
        for (Entry entry : entries.values()) {
            entry.retired = true;
            entry.closeIfIdle();
        }
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    private static HttpClient createClient(Provider provider) {
        return Feat.httpClient(provider.baseUrl(), options -> {
            // 共享 I/O 线程组：连接数不再等于线程数
            options.group(SHARED_GROUP);
            options.maxConnections(provider.maxConnections());
            options.connectTimeout(provider.connectTimeoutMs());
            options.idleTimeout(provider.idleTimeoutMs());
        });
    }

    public static final class Lease {
        private final Entry entry;
        private boolean released;

        private Lease(Entry entry) {
            this.entry = entry;
        }

        public HttpClient client() {
            return entry.client;
        }

        public void release() {
            if (released) {
                return;
            }
            released = true;
            entry.refs.decrementAndGet();
            entry.closeIfIdle();
        }
    }

    private static final class Entry {
        private final HttpClient client;
        private final AtomicInteger refs = new AtomicInteger();
        private volatile boolean retired;

        private Entry(HttpClient client) {
            this.client = client;
        }

        private void closeIfIdle() {
            if (retired && refs.get() <= 0) {
                try {
                    client.close();
                } catch (Throwable t) {
                    LOGGER.warn("关闭上游连接池失败: {}", t.getMessage());
                }
            }
        }
    }
}
