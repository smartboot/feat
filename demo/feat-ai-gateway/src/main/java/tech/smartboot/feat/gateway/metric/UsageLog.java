package tech.smartboot.feat.gateway.metric;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * 异步批量落盘的 usage 日志。
 * <p>
 * 网关的第一职责是转发，不是记账：请求线程只做入队，磁盘 IO 全在独立线程。
 * 队列满时丢弃并计数，绝不反压到转发链路——宁可少一条日志，不能卡住一个用户请求。
 */
public final class UsageLog {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int QUEUE_SIZE = 16384;
    private static final int BATCH = 256;

    private final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(QUEUE_SIZE);
    private final LongAdder dropped = new LongAdder();
    private final LongAdder written = new LongAdder();
    private final Path dir;
    private final boolean enabled;
    private volatile boolean running = true;
    private Thread writer;

    public UsageLog(String dir, boolean enabled) {
        this.dir = Paths.get(dir == null || dir.isEmpty() ? "logs" : dir);
        this.enabled = enabled;
        if (enabled) {
            start();
        }
    }

    public void write(UsageRecord r) {
        if (!enabled) {
            return;
        }
        String line = toJson(r);
        if (!queue.offer(line)) {
            dropped.increment();
        }
    }

    public long dropped() {
        return dropped.sum();
    }

    public long written() {
        return written.sum();
    }

    private void start() {
        writer = new Thread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        }, "gw-usage-writer");
        writer.setDaemon(true);
        writer.start();
    }

    private void loop() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        List<String> batch = new ArrayList<String>(BATCH);
        BufferedWriter out = null;
        String currentDay = null;
        while (running || !queue.isEmpty()) {
            batch.clear();
            try {
                String first = queue.poll(500, TimeUnit.MILLISECONDS);
                if (first != null) {
                    batch.add(first);
                    queue.drainTo(batch, BATCH - 1);
                }
            } catch (InterruptedException e) {
                return;
            }
            if (batch.isEmpty()) {
                flushQuietly(out);
                continue;
            }
            String today = dayFormat.format(new Date());
            try {
                if (out == null || !today.equals(currentDay)) {
                    closeQuietly(out);
                    Files.createDirectories(dir);
                    Path file = dir.resolve("usage-" + today + ".jsonl");
                    out = Files.newBufferedWriter(file, UTF8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    currentDay = today;
                }
                for (String line : batch) {
                    out.write(line);
                    out.write('\n');
                }
                written.add(batch.size());
                out.flush();
            } catch (IOException e) {
                dropped.add(batch.size());
                closeQuietly(out);
                out = null;
            }
        }
        closeQuietly(out);
    }

    private static void flushQuietly(BufferedWriter out) {
        if (out != null) {
            try {
                out.flush();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(BufferedWriter out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void close() {
        running = false;
        if (writer != null) {
            try {
                writer.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 手写 JSON，避免每个请求都创建 JSONObject（高并发下这是可观测的开销）。
     */
    static String toJson(UsageRecord r) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        field(sb, "ts", r.tsMs);
        str(sb, "traceId", r.traceId);
        str(sb, "keyId", r.keyId);
        str(sb, "model", r.model);
        str(sb, "provider", r.provider);
        num(sb, "status", r.status);
        bool(sb, "stream", r.stream);
        num(sb, "ttftMs", r.ttftMs);
        num(sb, "totalMs", r.totalMs);
        num(sb, "promptTokens", r.promptTokens);
        num(sb, "completionTokens", r.completionTokens);
        bool(sb, "estimated", r.estimated);
        num(sb, "bytesOut", r.bytesOut);
        str(sb, "error", r.error);
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        return sb.append('}').toString();
    }

    private static void field(StringBuilder sb, String name, long value) {
        sb.append('"').append(name).append("\":").append(value).append(',');
    }

    private static void num(StringBuilder sb, String name, long value) {
        field(sb, name, value);
    }

    private static void bool(StringBuilder sb, String name, boolean value) {
        sb.append('"').append(name).append("\":").append(value).append(',');
    }

    private static void str(StringBuilder sb, String name, String value) {
        if (value == null) {
            return;
        }
        sb.append('"').append(name).append("\":\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\",");
    }
}
