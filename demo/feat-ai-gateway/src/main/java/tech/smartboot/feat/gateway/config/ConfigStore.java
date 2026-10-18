package tech.smartboot.feat.gateway.config;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 配置快照的唯一入口。
 * <p>
 * 文件、watch、将来的控制面推送、admin API，全部走 {@link #apply(GatewayConfig)}。
 * 数据面代码只调 {@link #current()}，永远不知道配置从哪来。
 */
public final class ConfigStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigStore.class);

    private final Path sourcePath;
    private final AtomicLong revision = new AtomicLong(1);
    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private volatile GatewayConfig current;
    private volatile FileTime lastModified;
    private volatile boolean watching = true;

    public ConfigStore(Path sourcePath, GatewayConfig initial) {
        this.sourcePath = sourcePath;
        this.current = initial;
        this.revision.set(Math.max(1, initial.version()));
        this.lastModified = readLastModified();
    }

    public GatewayConfig current() {
        return current;
    }

    public long revision() {
        return revision.get();
    }

    public Path sourcePath() {
        return sourcePath;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * 整份替换。失败时抛异常，旧快照保持不变。
     */
    public synchronized long apply(GatewayConfig config) {
        long next = Math.max(revision.get() + 1, config.version());
        current = config;
        revision.set(next);
        for (Listener listener : listeners) {
            try {
                listener.onApply(config);
            } catch (Throwable t) {
                LOGGER.error("配置应用回调失败: {}", t.getMessage());
            }
        }
        LOGGER.info("配置已生效, version={}, providers={}, models={}, keys={}",
                next, config.providers().size(), config.allRoutes().size(), config.keys().size());
        return next;
    }

    /**
     * 从文件重载。返回 true 表示已应用新配置。
     */
    public boolean reloadFromFile() {
        if (sourcePath == null) {
            throw new ConfigException("未指定配置文件路径，无法重载");
        }
        GatewayConfig config = ConfigLoader.loadFile(sourcePath);
        apply(config);
        lastModified = readLastModified();
        return true;
    }

    /**
     * 轮询 mtime 实现热加载：不引入额外依赖，也不需要 operator 记住 reload 接口。
     * 解析失败只打日志，旧快照继续服务。
     */
    public void startWatch(final long intervalMs) {
        if (sourcePath == null) {
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (watching) {
                    try {
                        Thread.sleep(intervalMs);
                    } catch (InterruptedException e) {
                        return;
                    }
                    FileTime now = readLastModified();
                    if (now == null || now.equals(lastModified)) {
                        continue;
                    }
                    lastModified = now;
                    try {
                        reloadFromFile();
                        LOGGER.info("检测到配置文件变更，已热加载: {}", sourcePath);
                    } catch (Throwable t) {
                        LOGGER.warn("配置文件变更但校验失败，继续使用旧快照: {}", t.getMessage());
                    }
                }
            }
        }, "gw-config-watch");
        thread.setDaemon(true);
        thread.start();
    }

    public void stopWatch() {
        watching = false;
    }

    private FileTime readLastModified() {
        if (sourcePath == null) {
            return null;
        }
        try {
            if (!Files.exists(sourcePath)) {
                return null;
            }
            return Files.getLastModifiedTime(sourcePath);
        } catch (IOException e) {
            return null;
        }
    }

    public interface Listener {
        void onApply(GatewayConfig config);
    }
}
