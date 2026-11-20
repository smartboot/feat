package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.chat.entity.ChatChunk;
import tech.smartboot.feat.ai.chat.entity.ChatResponse;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ToolCall;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 mock 上游的端到端测试：验证 onChunk 元数据回调、usage/id/finish_reason 回填、
 * tool 消息序列化与 future.cancel 上游取消。
 * <p>
 * 不依赖外部服务，mock 上游用 JDK 内置 HttpServer 模拟 OpenAI / Anthropic 协议。
 * </p>
 */
public class ChatModelMockUpstreamTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeClass
    public static void startMockUpstream() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", new MockChatCompletionsHandler());
        server.setExecutor(null);
        server.start();
        int port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port + "/v1";
    }

    @AfterClass
    public static void stopMockUpstream() {
        server.stop(0);
    }

    /**
     * mock 上游：按请求体中的 stream 字段返回 SSE 流或完整 JSON
     */
    static class MockChatCompletionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = readAll(exchange.getRequestBody());
            JSONObject request = JSON.parseObject(body);
            boolean stream = Boolean.TRUE.equals(request.getBoolean("stream"));
            if (stream) {
                writeSseStream(exchange, request);
            } else {
                writeNonStreamResponse(exchange, request);
            }
        }

        private void writeNonStreamResponse(HttpExchange exchange, JSONObject request) throws IOException {
            JSONObject response = new JSONObject();
            response.put("id", "chatcmpl-mock-001");
            response.put("model", request.getString("model"));
            response.put("usage", JSONObject.of("prompt_tokens", 10, "completion_tokens", 20, "total_tokens", 30));
            JSONArray choices = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "assistant");
            message.put("content", "hello");
            choices.add(JSONObject.of("index", 0, "finish_reason", "stop", "message", message));
            response.put("choices", choices);
            byte[] bytes = response.toJSONString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void writeSseStream(HttpExchange exchange, JSONObject request) throws IOException {
            // OpenAI chunk 序列（含 role 帧、文本增量、tool_calls 增量、finish_reason、usage 帧、[DONE]）
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            List<String> events = new ArrayList<>();
            events.add(chunk("assistant", null, null, null));
            events.add(chunk(null, "he", null, null));
            events.add(chunk(null, "llo", null, null));
            JSONObject toolCall = new JSONObject();
            toolCall.put("index", 0);
            toolCall.put("id", "call_mock_1");
            JSONObject function = new JSONObject();
            function.put("name", "get_weather");
            function.put("arguments", "{\"city\"");
            toolCall.put("function", function);
            events.add(chunk(null, null, null, Collections.singletonList(toolCall)));
            JSONObject toolCall2 = new JSONObject();
            toolCall2.put("index", 0);
            JSONObject function2 = new JSONObject();
            function2.put("arguments", ":\"hangzhou\"}");
            toolCall2.put("function", function2);
            events.add(chunk(null, null, null, Collections.singletonList(toolCall2)));
            events.add(chunk(null, null, "tool_calls", null));
            events.add(chunk(null, null, "stop", null));
            // usage 帧：choices 为空、顶层携带 usage
            events.add(JSONObject.of("id", "chatcmpl-mock-001", "model", request.getString("model"),
                    "choices", new JSONArray(), "usage",
                    JSONObject.of("prompt_tokens", 10, "completion_tokens", 20, "total_tokens", 30)).toJSONString());
            events.add("[DONE]");
            try (OutputStream os = exchange.getResponseBody()) {
                for (String event : events) {
                    os.write(("data: " + event + "\n\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }
        }

        private String chunk(String role, String content, String finishReason, List<JSONObject> toolCalls) {
            JSONObject delta = new JSONObject();
            if (role != null) {
                delta.put("role", role);
            }
            if (content != null) {
                delta.put("content", content);
            }
            if (toolCalls != null) {
                delta.put("tool_calls", toolCalls);
            }
            JSONObject chunk = new JSONObject();
            chunk.put("id", "chatcmpl-mock-001");
            chunk.put("model", "mock-model");
            chunk.put("choices", Collections.singletonList(JSONObject.of("index", 0, "delta", delta, "finish_reason", finishReason)));
            return chunk.toJSONString();
        }

        private byte[] readAll(InputStream is) throws IOException {
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int read;
            while ((read = is.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    /**
     * 统计型监听器：记录 chunk、文本增量与终态
     */
    static class RecordingListener implements ChatStreamListener {
        final List<ChatChunk> chunks = Collections.synchronizedList(new ArrayList<>());
        final StringBuilder content = new StringBuilder();
        final AtomicReference<ChatResponse> completion = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        @Override
        public void onChunk(ChatChunk chunk) {
            chunks.add(chunk);
        }

        @Override
        public void onStreamResponse(String content) {
            this.content.append(content);
        }

        @Override
        public void onCompletion(ChatResponse response) {
            completion.set(response);
        }

        @Override
        public void onError(Throwable throwable) {
            error.set(throwable);
        }
    }

    private ChatModel model() {
        return FeatAI.chatModel(o -> {
            o.baseUrl(baseUrl);
            o.apiKey("sk-mock");
            o.model("mock-model");
            o.includeUsage(true);
        });
    }

    private static byte[] readAll(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        while ((read = is.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    @Test
    public void streamEmitsChunkMetadataAndBackfillsUsage() throws Exception {
        RecordingListener listener = new RecordingListener();
        CompletableFuture<ChatResponse> future = model().chatStream("hi", listener);
        ChatResponse response = future.get(10, TimeUnit.SECONDS);

        // 文本增量照常
        Assert.assertEquals("hello", listener.content.toString());
        // 终态回填元数据
        Assert.assertEquals("chatcmpl-mock-001", response.getId());
        Assert.assertEquals("mock-model", response.getModel());
        Assert.assertNotNull(response.getUsage());
        Assert.assertEquals(10, response.getUsage().getPromptTokens());
        Assert.assertEquals(20, response.getUsage().getCompletionTokens());
        Assert.assertEquals(30, response.getUsage().getTotalTokens());
        Assert.assertEquals("stop", response.getFinishReason());

        // onChunk：usage 帧作为独立 chunk 携带，finish_reason 在对应帧上
        ChatChunk usageChunk = null;
        for (ChatChunk chunk : listener.chunks) {
            if (chunk.getUsage() != null) {
                usageChunk = chunk;
                break;
            }
        }
        Assert.assertNotNull("usage 帧未通过 onChunk 暴露", usageChunk);
        Assert.assertEquals(30, usageChunk.getUsage().getTotalTokens());
        boolean hasFinishReason = false;
        for (ChatChunk chunk : listener.chunks) {
            if ("tool_calls".equals(chunk.getFinishReason()) || "stop".equals(chunk.getFinishReason())) {
                hasFinishReason = true;
                break;
            }
        }
        Assert.assertTrue("finish_reason 未通过 onChunk 暴露", hasFinishReason);

        // tool_calls 增量分片转发
        List<ToolCall> toolCallChunks = new ArrayList<>();
        for (ChatChunk chunk : listener.chunks) {
            if (chunk.getToolCalls() != null) {
                toolCallChunks.addAll(chunk.getToolCalls());
            }
        }
        Assert.assertEquals(2, toolCallChunks.size());
        Assert.assertEquals("call_mock_1", toolCallChunks.get(0).getId());
        Assert.assertEquals("get_weather", toolCallChunks.get(0).getName());
        Assert.assertEquals("{\"city\"", toolCallChunks.get(0).getArguments());
        Assert.assertEquals(":\"hangzhou\"}", toolCallChunks.get(1).getArguments());

        // 聚合后的 toolCalls 保留在终态响应
        Assert.assertNotNull(response.getToolCalls());
        Assert.assertEquals(1, response.getToolCalls().size());
        Assert.assertEquals("call_mock_1", response.getToolCalls().get(0).getId());
        Assert.assertEquals("{\"city\":\"hangzhou\"}", response.getToolCalls().get(0).getArguments());
    }

    @Test
    public void nonStreamParsesUsageAndFinishReason() throws Exception {
        ChatResponse response = model().chat("hi").get(10, TimeUnit.SECONDS);
        Assert.assertEquals("hello", response.getContent());
        Assert.assertEquals("chatcmpl-mock-001", response.getId());
        Assert.assertEquals("mock-model", response.getModel());
        Assert.assertEquals("stop", response.getFinishReason());
        Assert.assertNotNull(response.getUsage());
        Assert.assertEquals(30, response.getUsage().getTotalTokens());
    }

    @Test
    public void toolMessagesSerializeInOpenAiFormat() throws Exception {
        // 多轮工具对话：assistant 携带 tool_calls，tool 消息回传结果
        ToolCall toolCall = new ToolCall();
        toolCall.setIndex(0);
        toolCall.setId("call_1");
        toolCall.setName("get_weather");
        toolCall.setArguments("{\"city\":\"hangzhou\"}");

        List<Message> messages = new ArrayList<>();
        messages.add(Message.ofUser("杭州天气"));
        messages.add(Message.ofAssistant(null, Collections.singletonList(toolCall)));
        messages.add(Message.ofTool("call_1", "sunny"));

        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer captureServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        captureServer.createContext("/v1/chat/completions", exchange -> {
            byte[] body = readAll(exchange.getRequestBody());
            capturedBody.set(new String(body, StandardCharsets.UTF_8));
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        captureServer.start();
        try {
            String url = "http://127.0.0.1:" + captureServer.getAddress().getPort() + "/v1";
            ChatModel chatModel = FeatAI.chatModel(o -> {
                o.baseUrl(url);
                o.model("mock-model");
            });
            chatModel.chat(messages, null).get(10, TimeUnit.SECONDS);
            Assert.assertNotNull("未捕获到请求体", capturedBody.get());
            JSONObject body = JSON.parseObject(capturedBody.get());
            JSONArray jsonMessages = body.getJSONArray("messages");
            Assert.assertEquals(3, jsonMessages.size());

            // assistant 消息：tool_calls 为 OpenAI wire 格式（type:function + function 嵌套）
            JSONObject assistant = jsonMessages.getJSONObject(1);
            Assert.assertEquals("assistant", assistant.getString("role"));
            JSONArray toolCalls = assistant.getJSONArray("tool_calls");
            Assert.assertEquals(1, toolCalls.size());
            JSONObject wireToolCall = toolCalls.getJSONObject(0);
            Assert.assertEquals("call_1", wireToolCall.getString("id"));
            Assert.assertEquals("function", wireToolCall.getString("type"));
            Assert.assertEquals("get_weather", wireToolCall.getJSONObject("function").getString("name"));
            Assert.assertEquals("{\"city\":\"hangzhou\"}", wireToolCall.getJSONObject("function").getString("arguments"));
            Assert.assertNull("通用结构不应透传到 wire 层", wireToolCall.getString("index"));

            // tool 消息：role=tool + tool_call_id + content
            JSONObject toolMessage = jsonMessages.getJSONObject(2);
            Assert.assertEquals("tool", toolMessage.getString("role"));
            Assert.assertEquals("call_1", toolMessage.getString("tool_call_id"));
            Assert.assertEquals("sunny", toolMessage.getString("content"));
        } finally {
            captureServer.stop(0);
        }
    }

    @Test
    public void includeUsageInjectsStreamOptions() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer captureServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        captureServer.createContext("/v1/chat/completions", exchange -> {
            byte[] body = readAll(exchange.getRequestBody());
            capturedBody.set(new String(body, StandardCharsets.UTF_8));
            // 返回一个立即结束的流
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            }
        });
        captureServer.start();
        try {
            String url = "http://127.0.0.1:" + captureServer.getAddress().getPort() + "/v1";
            ChatModel chatModel = FeatAI.chatModel(o -> {
                o.baseUrl(url);
                o.model("mock-model");
                o.includeUsage(true);
            });
            chatModel.chatStream("hi", new RecordingListener()).get(10, TimeUnit.SECONDS);
            Assert.assertNotNull(capturedBody.get());
            JSONObject body = JSON.parseObject(capturedBody.get());
            JSONObject streamOptions = body.getJSONObject("stream_options");
            Assert.assertNotNull("include_usage 未注入", streamOptions);
            Assert.assertEquals(Boolean.TRUE, streamOptions.getBoolean("include_usage"));
        } finally {
            captureServer.stop(0);
        }
    }

    @Test
    public void anthropicToolMessagesMapToContentBlocks() throws Exception {
        // Anthropic：assistant 的 tool_calls 映射为 tool_use 块，tool 消息映射为 user + tool_result 块
        ToolCall toolCall = new ToolCall();
        toolCall.setIndex(0);
        toolCall.setId("toolu_1");
        toolCall.setName("get_weather");
        toolCall.setArguments("{\"city\":\"hangzhou\"}");

        List<Message> messages = new ArrayList<>();
        messages.add(Message.ofUser("杭州天气"));
        messages.add(Message.ofAssistant(null, Collections.singletonList(toolCall)));
        messages.add(Message.ofTool("toolu_1", "sunny"));

        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer anthropicServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        anthropicServer.createContext("/v1/messages", exchange -> {
            capturedBody.set(new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8));
            // 非流式完整响应（content 数组 + usage + stop_reason）
            JSONObject response = new JSONObject();
            response.put("id", "msg_mock_001");
            response.put("model", "claude-mock");
            response.put("stop_reason", "end_turn");
            response.put("content", Collections.singletonList(JSONObject.of("type", "text", "text", "hi")));
            response.put("usage", JSONObject.of("input_tokens", 5, "output_tokens", 7));
            byte[] bytes = response.toJSONString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        anthropicServer.start();
        try {
            String url = "http://127.0.0.1:" + anthropicServer.getAddress().getPort();
            ChatModel chatModel = FeatAI.chatModel(o -> {
                o.baseUrl(url);
                o.model("claude-mock");
                o.specProvider(tech.smartboot.feat.ai.chat.provider.anthropic.AnthropicProvider::new);
            });
            ChatResponse response = chatModel.chat(messages, null).get(10, TimeUnit.SECONDS);
            Assert.assertNotNull(capturedBody.get());
            JSONObject body = JSON.parseObject(capturedBody.get());
            // 通用 tool_calls 不应透传到 Anthropic 请求
            Assert.assertNull(body.getJSONArray("messages").getJSONObject(1).getJSONArray("tool_calls"));

            // user + assistant(tool_use) + user(tool_result)
            JSONArray jsonMessages = body.getJSONArray("messages");
            Assert.assertEquals(3, jsonMessages.size());
            JSONObject assistant = jsonMessages.getJSONObject(1);
            Assert.assertEquals("assistant", assistant.getString("role"));
            JSONArray content = assistant.getJSONArray("content");
            Assert.assertEquals(1, content.size());
            JSONObject toolUse = content.getJSONObject(0);
            Assert.assertEquals("tool_use", toolUse.getString("type"));
            Assert.assertEquals("toolu_1", toolUse.getString("id"));
            Assert.assertEquals("get_weather", toolUse.getString("name"));
            Assert.assertEquals("hangzhou", toolUse.getJSONObject("input").getString("city"));

            JSONObject toolResult = jsonMessages.getJSONObject(2);
            Assert.assertEquals("user", toolResult.getString("role"));
            JSONObject block = toolResult.getJSONArray("content").getJSONObject(0);
            Assert.assertEquals("tool_result", block.getString("type"));
            Assert.assertEquals("toolu_1", block.getString("tool_use_id"));
            Assert.assertEquals("sunny", block.getString("content"));

            // 非流式解析：id/stop_reason 映射/usage 汇总
            Assert.assertEquals("msg_mock_001", response.getId());
            Assert.assertEquals("stop", response.getFinishReason());
            Assert.assertNotNull(response.getUsage());
            Assert.assertEquals(5, response.getUsage().getPromptTokens());
            Assert.assertEquals(7, response.getUsage().getCompletionTokens());
            Assert.assertEquals(12, response.getUsage().getTotalTokens());
        } finally {
            anthropicServer.stop(0);
        }
    }

    @Test
    public void anthropicStreamParsesMetadataAndUsage() throws Exception {
        HttpServer anthropicServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        anthropicServer.createContext("/v1/messages", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            List<String> events = new ArrayList<>();
            events.add("message_start: " + JSONObject.of("type", "message_start", "message",
                    JSONObject.of("id", "msg_mock_002", "model", "claude-mock", "usage", JSONObject.of("input_tokens", 5))).toJSONString());
            events.add("content_block_delta: " + JSONObject.of("type", "content_block_delta", "index", 0,
                    "delta", JSONObject.of("type", "text_delta", "text", "hi")).toJSONString());
            events.add("message_delta: " + JSONObject.of("type", "message_delta",
                    "delta", JSONObject.of("stop_reason", "end_turn"), "usage", JSONObject.of("output_tokens", 7)).toJSONString());
            events.add("message_stop: " + JSONObject.of("type", "message_stop").toJSONString());
            try (OutputStream os = exchange.getResponseBody()) {
                for (String event : events) {
                    String[] parts = event.split(": ", 2);
                    os.write(("event: " + parts[0] + "\ndata: " + parts[1] + "\n\n").getBytes(StandardCharsets.UTF_8));
                }
                os.flush();
            }
        });
        anthropicServer.start();
        try {
            String url = "http://127.0.0.1:" + anthropicServer.getAddress().getPort();
            ChatModel chatModel = FeatAI.chatModel(o -> {
                o.baseUrl(url);
                o.model("claude-mock");
                o.specProvider(tech.smartboot.feat.ai.chat.provider.anthropic.AnthropicProvider::new);
            });
            RecordingListener listener = new RecordingListener();
            ChatResponse response = chatModel.chatStream("hi", listener).get(10, TimeUnit.SECONDS);

            Assert.assertEquals("hi", listener.content.toString());
            Assert.assertEquals("msg_mock_002", response.getId());
            Assert.assertEquals("claude-mock", response.getModel());
            // end_turn 映射为 OpenAI 兼容的 stop
            Assert.assertEquals("stop", response.getFinishReason());
            Assert.assertNotNull(response.getUsage());
            Assert.assertEquals(5, response.getUsage().getPromptTokens());
            Assert.assertEquals(7, response.getUsage().getCompletionTokens());
            Assert.assertEquals(12, response.getUsage().getTotalTokens());
            // onChunk 携带 id 与 usage
            Assert.assertTrue("onChunk 未触发", !listener.chunks.isEmpty());
            Assert.assertEquals("msg_mock_002", listener.chunks.get(0).getId());
            boolean usageChunkFound = false;
            for (ChatChunk chunk : listener.chunks) {
                if (chunk.getUsage() != null && chunk.getUsage().getCompletionTokens() == 7) {
                    usageChunkFound = true;
                    break;
                }
            }
            Assert.assertTrue("message_delta 的 usage 未通过 onChunk 暴露", usageChunkFound);
        } finally {
            anthropicServer.stop(0);
        }
    }

    @Test
    public void cancelStopsUpstreamAndStaysSilent() throws Exception {
        // 慢速流式上游：每 100ms 一帧，共 50 帧
        AtomicInteger serverFramesSent = new AtomicInteger();
        HttpServer slowServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slowServer.createContext("/v1/chat/completions", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                try (OutputStream os = exchange.getResponseBody()) {
                    for (int i = 0; i < 50; i++) {
                        JSONObject chunk = new JSONObject();
                        chunk.put("id", "chatcmpl-slow");
                        chunk.put("model", "mock-model");
                        chunk.put("choices", Collections.singletonList(JSONObject.of("index", 0, "delta", JSONObject.of("content", "x"))));
                        os.write(("data: " + chunk.toJSONString() + "\n\n").getBytes(StandardCharsets.UTF_8));
                        os.flush();
                        serverFramesSent.incrementAndGet();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    os.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                    // 客户端断连导致写失败，符合预期
                }
            }
        });
        slowServer.start();
        try {
            String url = "http://127.0.0.1:" + slowServer.getAddress().getPort() + "/v1";
            ChatModel chatModel = FeatAI.chatModel(o -> {
                o.baseUrl(url);
                o.model("mock-model");
            });
            RecordingListener listener = new RecordingListener();
            CompletableFuture<ChatResponse> future = chatModel.chatStream("hi", listener);
            // 收到几帧后取消
            int target = 3;
            long deadline = System.currentTimeMillis() + 10000;
            while (listener.content.length() < target && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Assert.assertTrue("未收到预期帧数", listener.content.length() >= target);
            boolean cancelled = future.cancel(true);
            Assert.assertTrue("future 应可被取消", cancelled);

            // 静默：取消后不应再有回调、不应触发 onCompletion/onError
            int receivedAtCancel = listener.content.length();
            int sentAtCancel = serverFramesSent.get();
            Thread.sleep(600);
            Assert.assertTrue("取消后仍收到回调", listener.content.length() <= receivedAtCancel + 1);
            Assert.assertTrue("上游仍在生成", serverFramesSent.get() <= sentAtCancel + 2);
            Assert.assertNull("取消不应触发 onCompletion", listener.completion.get());
            Assert.assertNull("取消不应触发 onError", listener.error.get());

            // 取消终态：get() 直接抛 CancellationException
            try {
                future.get(1, TimeUnit.SECONDS);
                Assert.fail("取消后的 future 不应正常完成");
            } catch (CancellationException expected) {
                // 取消终态
            } catch (TimeoutException unexpected) {
                Assert.fail("取消后的 future 不应悬空");
            }
        } finally {
            slowServer.stop(0);
        }
    }
}
