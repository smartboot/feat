package tech.smartboot.feat.gateway.metric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

/**
 * 进程内指标。全部无锁累加，读写都不阻塞转发路径。
 * <p>
 * 只保留聚合值，不保留请求明细——明细在 usage 日志里，否则高并发下内存会无界增长。
 */
public final class GatewayMetrics {
    private static final int QPS_WINDOW = 60;

    private final long startTimeMs = System.currentTimeMillis();
    private final LongAdder requests = new LongAdder();
    private final LongAdder errors = new LongAdder();
    private final LongAdder inflight = new LongAdder();
    private final LongAdder ttftSumMs = new LongAdder();
    private final LongAdder ttftCount = new LongAdder();
    private final LongAdder durationSumMs = new LongAdder();
    private final LongAdder promptTokens = new LongAdder();
    private final LongAdder completionTokens = new LongAdder();
    private final LongAdder estimatedCount = new LongAdder();

    private final Map<String, LongAdder> modelRequests = new ConcurrentHashMap<String, LongAdder>();
    private final Map<String, LongAdder> providerErrors = new ConcurrentHashMap<String, LongAdder>();
    private final Map<String, LongAdder> keyTokens = new ConcurrentHashMap<String, LongAdder>();

    private final AtomicLongArray qpsCounts = new AtomicLongArray(QPS_WINDOW);
    private final AtomicLongArray qpsStamps = new AtomicLongArray(QPS_WINDOW);

    public GatewayMetrics() {
        for (int i = 0; i < QPS_WINDOW; i++) {
            qpsStamps.set(i, -1);
        }
    }

    public void onRequestStart(String model) {
        requests.increment();
        inflight.increment();
        adder(modelRequests, model == null ? "unknown" : model).increment();
        markQps();
    }

    public void onRequestEnd(String keyId, long ttftMs, long durationMs, long prompt, long completion,
                             boolean estimated, String provider, boolean failed) {
        inflight.decrement();
        durationSumMs.add(durationMs);
        if (ttftMs >= 0) {
            ttftSumMs.add(ttftMs);
            ttftCount.increment();
        }
        if (prompt > 0) {
            promptTokens.add(prompt);
        }
        if (completion > 0) {
            completionTokens.add(completion);
        }
        if (estimated) {
            estimatedCount.increment();
        }
        if (keyId != null) {
            adder(keyTokens, keyId).add(prompt + completion);
        }
        if (failed) {
            errors.increment();
            if (provider != null) {
                adder(providerErrors, provider).increment();
            }
        }
    }

    public void onRejected(String keyId) {
        requests.increment();
        errors.increment();
        markQps();
    }

    public long requests() {
        return requests.sum();
    }

    public long errors() {
        return errors.sum();
    }

    public long inflight() {
        return inflight.sum();
    }

    public long uptimeMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    public long promptTokens() {
        return promptTokens.sum();
    }

    public long completionTokens() {
        return completionTokens.sum();
    }

    public long estimatedCount() {
        return estimatedCount.sum();
    }

    public long qps() {
        long nowSec = System.currentTimeMillis() / 1000;
        long sum = 0;
        for (int i = 0; i < QPS_WINDOW; i++) {
            if (qpsStamps.get(i) > nowSec - QPS_WINDOW) {
                sum += qpsCounts.get(i);
            }
        }
        return sum / QPS_WINDOW;
    }

    public Map<String, LongAdder> modelRequests() {
        return modelRequests;
    }

    public Map<String, LongAdder> providerErrors() {
        return providerErrors;
    }

    public Map<String, LongAdder> keyTokens() {
        return keyTokens;
    }

    /**
     * Prometheus 文本格式。命名遵循 OpenMetrics 习惯：单位后缀 + _total 计数。
     */
    public String prometheus() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("# HELP feat_gateway_requests_total Total proxied requests\n");
        sb.append("# TYPE feat_gateway_requests_total counter\n");
        sb.append("feat_gateway_requests_total ").append(requests.sum()).append('\n');

        sb.append("# HELP feat_gateway_errors_total Failed or rejected requests\n");
        sb.append("# TYPE feat_gateway_errors_total counter\n");
        sb.append("feat_gateway_errors_total ").append(errors.sum()).append('\n');

        sb.append("# HELP feat_gateway_inflight_requests In-flight proxied requests\n");
        sb.append("# TYPE feat_gateway_inflight_requests gauge\n");
        sb.append("feat_gateway_inflight_requests ").append(inflight.sum()).append('\n');

        sb.append("# HELP feat_gateway_qps_avg_60s Average QPS over the last 60 seconds\n");
        sb.append("# TYPE feat_gateway_qps_avg_60s gauge\n");
        sb.append("feat_gateway_qps_avg_60s ").append(qps()).append('\n');

        sb.append("# HELP feat_gateway_ttft_ms First token latency\n");
        sb.append("# TYPE feat_gateway_ttft_ms summary\n");
        sb.append("feat_gateway_ttft_ms_sum ").append(ttftSumMs.sum()).append('\n');
        sb.append("feat_gateway_ttft_ms_count ").append(ttftCount.sum()).append('\n');

        sb.append("# HELP feat_gateway_duration_ms Request duration\n");
        sb.append("# TYPE feat_gateway_duration_ms summary\n");
        sb.append("feat_gateway_duration_ms_sum ").append(durationSumMs.sum()).append('\n');
        sb.append("feat_gateway_duration_ms_count ").append(requests.sum()).append('\n');

        sb.append("# HELP feat_gateway_tokens_total Tokens accounted by the gateway\n");
        sb.append("# TYPE feat_gateway_tokens_total counter\n");
        sb.append("feat_gateway_tokens_total{type=\"prompt\"} ").append(promptTokens.sum()).append('\n');
        sb.append("feat_gateway_tokens_total{type=\"completion\"} ").append(completionTokens.sum()).append('\n');
        sb.append("feat_gateway_tokens_total{type=\"estimated\"} ").append(estimatedCount.sum()).append('\n');

        for (Map.Entry<String, LongAdder> e : modelRequests.entrySet()) {
            sb.append("feat_gateway_model_requests_total{model=\"")
                    .append(escape(e.getKey())).append("\"} ").append(e.getValue().sum()).append('\n');
        }
        for (Map.Entry<String, LongAdder> e : providerErrors.entrySet()) {
            sb.append("feat_gateway_provider_errors_total{provider=\"")
                    .append(escape(e.getKey())).append("\"} ").append(e.getValue().sum()).append('\n');
        }
        for (Map.Entry<String, LongAdder> e : keyTokens.entrySet()) {
            sb.append("feat_gateway_key_tokens_total{key_id=\"")
                    .append(escape(e.getKey())).append("\"} ").append(e.getValue().sum()).append('\n');
        }

        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        sb.append("# HELP feat_gateway_jvm_heap_used_bytes JVM heap in use\n");
        sb.append("# TYPE feat_gateway_jvm_heap_used_bytes gauge\n");
        sb.append("feat_gateway_jvm_heap_used_bytes ").append(used).append('\n');
        sb.append("feat_gateway_jvm_heap_max_bytes ").append(rt.maxMemory()).append('\n');
        sb.append("feat_gateway_uptime_ms ").append(uptimeMs()).append('\n');
        return sb.toString();
    }

    private static String escape(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void markQps() {
        long sec = System.currentTimeMillis() / 1000;
        int idx = (int) (sec % QPS_WINDOW);
        if (qpsStamps.get(idx) != sec) {
            synchronized (this) {
                if (qpsStamps.get(idx) != sec) {
                    qpsStamps.set(idx, sec);
                    qpsCounts.set(idx, 0);
                }
            }
        }
        qpsCounts.incrementAndGet(idx);
    }

    private static LongAdder adder(Map<String, LongAdder> map, String key) {
        LongAdder adder = map.get(key);
        if (adder == null) {
            map.putIfAbsent(key, new LongAdder());
            adder = map.get(key);
        }
        return adder;
    }
}
