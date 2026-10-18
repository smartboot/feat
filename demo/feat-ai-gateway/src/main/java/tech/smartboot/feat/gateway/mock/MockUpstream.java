package tech.smartboot.feat.gateway.mock;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.ServerOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 一个假的 OpenAI 兼容上游，用来做本地联调和压测。
 * <p>
 * 它能模拟真实上游最关键的两个特征：首字有延迟、后续按 token 流式吐。
 * 网关的 TTFT 只有对着这样的上游测才有意义。
 * <p>
 * 环境变量：
 * <pre>
 *   MOCK_PORT      默认 18081
 *   MOCK_TTFT_MS   首字延迟，默认 300
 *   MOCK_CHUNK_MS  每个 chunk 间隔，默认 20
 *   MOCK_CHUNKS    分片数量，默认 40
 *   MOCK_BURST     1 = 首字之后一次性吐完（压测用，避免 mock 自身排队影响结论）
 * </pre>
 */
public final class MockUpstream {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String TEXT = "feat 是一个用 Java 写的高性能 Web 框架，"
            + "它采用 AIO 模型，内存占用低，启动速度快，适合在资源受限的环境中运行。";

    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()));

    public static void main(String[] args) throws Exception {
        int port = intEnv("MOCK_PORT", args.length > 0 ? Integer.parseInt(args[0]) : 18081);
        ServerOptions options = new ServerOptions();
        options.port(port);
        options.setIdleTimeout(600000);
        Feat.httpServer(options).httpHandler(new HttpHandler() {
            @Override
            public void handle(HttpRequest request) {
                handle(request, new CompletableFuture<Void>());
            }

            @Override
            public void handle(HttpRequest request, CompletableFuture<Void> future) {
                MockUpstream.handle(request, future);
            }
        }).listen(port);
        System.out.println("[mock-upstream] listening on http://127.0.0.1:" + port
                + "  ttft=" + intEnv("MOCK_TTFT_MS", 300) + "ms"
                + "  chunk=" + intEnv("MOCK_CHUNK_MS", 20) + "ms"
                + "  chunks=" + intEnv("MOCK_CHUNKS", 40));
    }

    private static void handle(HttpRequest request, CompletableFuture<Void> future) {
        String path = request.getRequestURI();
        try {
            if ("/v1/models".equals(path)) {
                writeJson(request, 200, "{\"object\":\"list\",\"data\":[{\"id\":\"mock-model\",\"object\":\"model\"}]}");
                future.complete(null);
                return;
            }
            if ("/v1/chat/completions".equals(path) || "/v1/completions".equals(path)) {
                chat(request, path, future);
                return;
            }
            if ("/v1/embeddings".equals(path)) {
                writeJson(request, 200, "{\"object\":\"list\",\"data\":[{\"object\":\"embedding\",\"index\":0,"
                        + "\"embedding\":[0.1,0.2,0.3]}],\"usage\":{\"prompt_tokens\":8,\"total_tokens\":8}}");
                future.complete(null);
                return;
            }
            writeJson(request, 404, "{\"error\":{\"message\":\"not found\"}}");
            future.complete(null);
        } catch (Throwable t) {
            writeJson(request, 500, "{\"error\":{\"message\":\"" + t.getMessage() + "\"}}");
            future.complete(null);
        }
    }

    private static void chat(HttpRequest request, String path, CompletableFuture<Void> future) throws IOException {
        JSONObject body = JSON.parseObject(readBody(request));
        final String model = body == null ? "mock-model" : body.getString("model");
        boolean stream = body != null && Boolean.TRUE.equals(body.getBoolean("stream"));
        JSONObject streamOptions = body == null ? null : body.getJSONObject("stream_options");
        boolean includeUsage = streamOptions != null && Boolean.TRUE.equals(streamOptions.getBoolean("include_usage"));

        final int chunks = intEnv("MOCK_CHUNKS", 40);
        final int chunkMs = intEnv("MOCK_CHUNK_MS", 20);
        final int ttftMs = intEnv("MOCK_TTFT_MS", 300);
        // 压测时把分片一次性吐完：mock 自己不能成为瓶颈，否则测出来的是 mock 的排队时长
        final boolean burst = intEnv("MOCK_BURST", 0) == 1;

        if (!stream) {
            long promptTokens = estimatePrompt(body);
            long completionTokens = chunks * 3L;
            TIMER.schedule(() -> {
                writeJson(request, 200, completionJson(model, promptTokens, completionTokens));
                future.complete(null);
            }, ttftMs + (long) chunks * chunkMs, TimeUnit.MILLISECONDS);
            return;
        }

        final HttpResponse response = request.getResponse();
        response.setHeader("Content-Type", "text/event-stream");
        response.setHeader("Cache-Control", "no-cache");
        response.getOutputStream().flush();

        final String[] tokens = splitTokens(TEXT);
        final long promptTokens = estimatePrompt(body);
        final String id = "chatcmpl-mock-" + System.nanoTime();
        final int[] cursor = new int[]{0};
        final boolean[] aborted = new boolean[]{false};

        // 串行调度：写完一片再排下一片，避免多线程同时写同一个响应导致 SSE 乱序
        final Runnable[] emitter = new Runnable[1];
        emitter[0] = () -> {
            if (aborted[0]) {
                return;
            }
            try {
                if (cursor[0] < chunks) {
                    String token = tokens[cursor[0] % tokens.length];
                    String chunk = "data: {\"id\":\"" + id + "\",\"object\":\"chat.completion.chunk\","
                            + "\"created\":" + (System.currentTimeMillis() / 1000) + ","
                            + "\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\""
                            + token + "\"},\"finish_reason\":null}]}\n\n";
                    write(response, chunk);
                    cursor[0]++;
                    if (!burst) {
                        TIMER.schedule(emitter[0], chunkMs, TimeUnit.MILLISECONDS);
                        return;
                    }
                    emitter[0].run();
                    return;
                }
                write(response, "data: {\"id\":\"" + id + "\",\"object\":\"chat.completion.chunk\","
                        + "\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n");
                if (includeUsage) {
                    write(response, "data: {\"id\":\"" + id + "\",\"object\":\"chat.completion.chunk\","
                            + "\"choices\":[],\"usage\":{\"prompt_tokens\":" + promptTokens
                            + ",\"completion_tokens\":" + (chunks * 3L)
                            + ",\"total_tokens\":" + (promptTokens + chunks * 3L) + "}}\n\n");
                }
                write(response, "data: [DONE]\n\n");
                response.close();
                future.complete(null);
            } catch (Throwable t) {
                // 客户端已断开（说明网关把取消传下来了），停止发送
                aborted[0] = true;
                response.close();
                future.complete(null);
            }
        };
        TIMER.schedule(emitter[0], ttftMs, TimeUnit.MILLISECONDS);
    }

    private static void write(HttpResponse response, String text) throws IOException {
        response.write(text.getBytes(UTF8));
        response.getOutputStream().flush();
    }

    private static String completionJson(String model, long prompt, long completion) {
        return "{\"id\":\"chatcmpl-mock\",\"object\":\"chat.completion\",\"created\":" + (System.currentTimeMillis() / 1000)
                + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\","
                + "\"content\":\"" + TEXT + "\"},\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":" + prompt + ",\"completion_tokens\":" + completion
                + ",\"total_tokens\":" + (prompt + completion) + "}}";
    }

    private static long estimatePrompt(JSONObject body) {
        if (body == null) {
            return 10;
        }
        Object messages = body.get("messages");
        return messages == null ? 10 : Math.max(1, JSON.toJSONString(messages).length() / 4L);
    }

    private static String[] splitTokens(String text) {
        // 按字符切，模拟"一个字一个 token"的最坏情况，更接近中文真实流式表现
        String[] tokens = new String[text.length()];
        for (int i = 0; i < text.length(); i++) {
            tokens[i] = String.valueOf(text.charAt(i));
        }
        return tokens;
    }

    private static String readBody(HttpRequest request) throws IOException {
        BodyInputStream in = request.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] buf = new byte[4096];
        int read;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
        }
        return new String(out.toByteArray(), UTF8);
    }

    private static void writeJson(HttpRequest request, int status, String body) {
        HttpResponse response = request.getResponse();
        try {
            byte[] bytes = body.getBytes(UTF8);
            response.setHttpStatus(status, "OK");
            response.setHeader("Content-Type", "application/json;charset=utf-8");
            response.setHeader("Content-Length", String.valueOf(bytes.length));
            response.write(bytes);
        } catch (IOException ignored) {
        } finally {
            response.close();
        }
    }

    private static int intEnv(String name, int def) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
