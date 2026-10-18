package tech.smartboot.feat.gateway.policy;

import tech.smartboot.feat.gateway.config.KeyPolicy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内配额：RPM 滑动窗口、TPM 滑动窗口、月度 token 累计。
 * <p>
 * v0.1 是单机语义。多副本时 N 个实例等于 N 倍配额，这个问题不是换个存储能解决的，
 * 需要分布式限流（Redis 或配额租借），留给云原生版。
 * <p>
 * TPM 只能事后计量：请求进来时不知道会消耗多少 token，所以策略是"本期已超就拒绝下一请求"，
 * 而不是"预估后拒绝当前请求"。这是所有 LLM 网关的通行做法。
 */
public final class QuotaGuard {
    private static final int WINDOW_SECONDS = 60;

    private final Map<String, Window> windows = new ConcurrentHashMap<String, Window>();
    private final Map<String, Monthly> monthly = new ConcurrentHashMap<String, Monthly>();

    public static final class Decision {
        public static final Decision ALLOW = new Decision(true, 200, "");
        private final boolean allowed;
        private final int status;
        private final String message;

        private Decision(boolean allowed, int status, String message) {
            this.allowed = allowed;
            this.status = status;
            this.message = message;
        }

        public boolean allowed() {
            return allowed;
        }

        public int status() {
            return status;
        }

        public String message() {
            return message;
        }
    }

    public Decision check(KeyPolicy key) {
        Window window = windowOf(key.id());
        if (key.rpm() > 0 && !window.tryAcquireRequest(key.rpm())) {
            return new Decision(false, 429, "rate limit exceeded: rpm=" + key.rpm() + " (key=" + key.id() + ")");
        }
        if (key.tpm() > 0 && window.tokensExceeded(key.tpm())) {
            return new Decision(false, 429, "token rate limit exceeded: tpm=" + key.tpm() + " (key=" + key.id() + ")");
        }
        if (key.monthlyTokens() > 0) {
            Monthly m = monthlyOf(key.id());
            if (m.tokens.get() >= key.monthlyTokens()) {
                return new Decision(false, 429, "monthly quota exhausted: " + key.monthlyTokens() + " tokens (key=" + key.id() + ")");
            }
        }
        return Decision.ALLOW;
    }

    /**
     * 请求结束后回填真实用量。estimated 只影响日志，不影响配额计算——预估也是消耗。
     */
    public void record(KeyPolicy key, long totalTokens) {
        if (key == null || totalTokens <= 0) {
            return;
        }
        windowOf(key.id()).addTokens(totalTokens);
        monthlyOf(key.id()).tokens.addAndGet(totalTokens);
    }

    public long monthlyTokens(String keyId) {
        Monthly m = monthly.get(keyId);
        return m == null ? 0 : m.tokens.get();
    }

    public long windowTokens(String keyId) {
        Window w = windows.get(keyId);
        return w == null ? 0 : w.currentTokens();
    }

    public void reset() {
        windows.clear();
        monthly.clear();
    }

    private Window windowOf(String keyId) {
        Window w = windows.get(keyId);
        if (w == null) {
            windows.putIfAbsent(keyId, new Window());
            w = windows.get(keyId);
        }
        return w;
    }

    private Monthly monthlyOf(String keyId) {
        String period = new SimpleDateFormat("yyyy-MM").format(new Date());
        Monthly m = monthly.get(keyId);
        if (m == null || !period.equals(m.period)) {
            m = new Monthly(period);
            monthly.put(keyId, m);
        }
        return m;
    }

    private static final class Monthly {
        private final String period;
        private final AtomicLong tokens = new AtomicLong();

        private Monthly(String period) {
            this.period = period;
        }
    }

    private static final class Window {
        private final long[] requestBuckets = new long[WINDOW_SECONDS];
        private final long[] tokenBuckets = new long[WINDOW_SECONDS];
        private long lastSecond = -1;
        private long requestTotal;
        private long tokenTotal;

        synchronized boolean tryAcquireRequest(int rpm) {
            roll(currentSecond());
            if (requestTotal >= rpm) {
                return false;
            }
            requestBuckets[(int) (currentSecond() % WINDOW_SECONDS)]++;
            requestTotal++;
            return true;
        }

        synchronized boolean tokensExceeded(long tpm) {
            roll(currentSecond());
            return tokenTotal >= tpm;
        }

        synchronized void addTokens(long tokens) {
            roll(currentSecond());
            tokenBuckets[(int) (currentSecond() % WINDOW_SECONDS)] += tokens;
            tokenTotal += tokens;
        }

        synchronized long currentTokens() {
            roll(currentSecond());
            return tokenTotal;
        }

        private void roll(long second) {
            if (lastSecond < 0) {
                lastSecond = second;
                return;
            }
            long delta = second - lastSecond;
            if (delta <= 0) {
                return;
            }
            if (delta >= WINDOW_SECONDS) {
                java.util.Arrays.fill(requestBuckets, 0);
                java.util.Arrays.fill(tokenBuckets, 0);
                requestTotal = 0;
                tokenTotal = 0;
            } else {
                for (long i = lastSecond + 1; i <= second; i++) {
                    int idx = (int) (i % WINDOW_SECONDS);
                    requestTotal -= requestBuckets[idx];
                    requestBuckets[idx] = 0;
                    tokenTotal -= tokenBuckets[idx];
                    tokenBuckets[idx] = 0;
                }
            }
            lastSecond = second;
        }

        private long currentSecond() {
            return System.currentTimeMillis() / 1000L;
        }
    }
}
