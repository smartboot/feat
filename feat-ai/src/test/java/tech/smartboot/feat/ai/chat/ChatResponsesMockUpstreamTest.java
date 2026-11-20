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
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.ai.chat.provider.openai.OpenAiResponsesProvider;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 mock 上游的 Responses API 端到端测试：
 * 验证 input/instructions/tools 扁平格式的请求映射、语义事件流的增量解析、
 * 终态事件（response.completed / response.incomplete）的 usage 与 finish_reason 回填。
 * <p>
 * 不依赖外部服务，mock 上游用 JDK 内置 HttpServer 模拟 OpenAI Responses 协议。
 * </p>
 */
public class ChatResponsesMockUpstreamTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeClass
    public static void startMockUpstream() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", new MockResponsesHandler());
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
     * mock 上游：按请求体中的 stream 字段返回 SSE 语义事件流或完整 JSON
     */
    static class MockResponsesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = readAll(exchange.getRequestBody());
            JSONObject request = JSON.parseObject(body);
            if (Boolean.TRUE.equals(request.getBoolean("stream"))) {
                writeSseStream(exchange, request);
            } else {
                writeNonStreamResponse(exchange, request);
            }
        }

        private void writeNonStreamResponse(HttpExchange exchange, JSONObject request) throws IOException {
            JSONObject response = mockResponse(request.getString("model"), "completed");
            byte[] bytes = response.toJSONString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void writeSseStream(HttpExchange exchange, JSONObject request) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            List<JSONObject> events = new ArrayList<>();
            // 元数据帧
            events.add(JSONObject.of("type", "response.created", "response",
                    JSONObject.of("id", "resp_mock_001", "object", "response", "model", request.getString("model"), "status", "in_progress")));
            // function_call 项开始：暴露 call_id 与 name
            events.add(JSONObject.of("type", "response.output_item.added", "output_index", 0, "item",
                    JSONObject.of("type", "function_call", "id", "fc_mock_1", "call_id", "call_mock_1",
                            "name", "get_weather", "arguments", "", "status", "in_progress")));
            // 参数增量分片
            events.add(JSONObject.of("type", "response.function_call_arguments.delta", "output_index", 0, "delta", "{\"city\""));
            events.add(JSONObject.of("type", "response.function_call_arguments.delta", "output_index", 0, "delta", ":\"hangzhou\"}"));
            // 推理摘要增量
            events.add(JSONObject.of("type", "response.reasoning_summary_text.delta", "item_id", "rs_1", "delta", "thinking"));
            // 文本增量
            events.add(JSONObject.of("type", "response.output_text.delta", "item_id", "msg_1", "delta", "he"));
            events.add(JSONObject.of("type", "response.output_text.delta", "item_id", "msg_1", "delta", "llo"));
            // 终态事件：携带完整 response 对象，无 [DONE]
            events.add(JSONObject.of("type", "response.completed", "response", mockResponse(request.getString("model"), "completed")));
            try (OutputStream os = exchange.getResponseBody()) {
                for (JSONObject event : events) {
                    os.write(("data: " + event.toJSONString() + "\n\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }
        }
    }

    /**
     * 完整 response 对象：reasoning + message(output_text) + function_call
     */
    static JSONObject mockResponse(String model, String status) {
        JSONObject response = new JSONObject();
        response.put("id", "resp_mock_001");
        response.put("object", "response");
        response.put("model", model);
        response.put("status", status);
        JSONArray output = new JSONArray();
        output.add(JSONObject.of("type", "reasoning", "id", "rs_mock_1", "summary",
                Collections.singletonList(JSONObject.of("type", "summary_text", "text", "thinking"))));
        output.add(JSONObject.of("type", "message", "id", "msg_mock_1", "role", "assistant", "status", "completed",
                "content", Collections.singletonList(JSONObject.of("type", "output_text", "text", "hello", "annotations", new JSONArray()))));
        output.add(JSONObject.of("type", "function_call", "id", "fc_mock_1", "call_id", "call_mock_1",
                "name", "get_weather", "arguments", "{\"city\":\"hangzhou\"}", "status", "completed"));
        response.put("output", output);
        response.put("usage", JSONObject.of("input_tokens", 10, "output_tokens", 20, "total_tokens", 30));
        return response;
    }

    /**
     * 统计型监听器：记录 chunk、文本/推理增量与终态
     */
    static class RecordingListener implements ChatStreamListener {
        final List<ChatChunk> chunks = Collections.synchronizedList(new ArrayList<>());
        final StringBuilder content = new StringBuilder();
        final StringBuilder reasoning = new StringBuilder();
        final AtomicReference<ChatResponse> completion = new AtomicReference<>();

        @Override
        public void onChunk(ChatChunk chunk) {
            chunks.add(chunk);
        }

        @Override
        public void onStreamResponse(String content) {
            this.content.append(content);
        }

        @Override
        public void onReasoning(String reasoning) {
            this.reasoning.append(reasoning);
        }

        @Override
        public void onCompletion(ChatResponse response) {
            completion.set(response);
        }
    }

    private ChatModel model() {
        return FeatAI.chatModel(o -> {
            o.baseUrl(baseUrl);
            o.apiKey("sk-mock");
            o.model("mock-model");
            o.specProvider(OpenAiResponsesProvider::new);
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

    /**
     * 流式语义事件：文本/推理/工具参数增量与终态回填
     */
    @Test
    public void streamParsesSemanticEvents() throws Exception {
        RecordingListener listener = new RecordingListener();
        CompletableFuture<ChatResponse> future = model().chatStream("hi", listener);
        ChatResponse response = future.get(10, TimeUnit.SECONDS);

        Assert.assertEquals("hello", listener.content.toString());
        Assert.assertEquals("thinking", listener.reasoning.toString());

        // 终态元数据回填
        Assert.assertEquals("resp_mock_001", response.getId());
        Assert.assertEquals("mock-model", response.getModel());
        Assert.assertEquals("tool_calls", response.getFinishReason());
        Assert.assertNotNull(response.getUsage());
        Assert.assertEquals(10, response.getUsage().getPromptTokens());
        Assert.assertEquals(20, response.getUsage().getCompletionTokens());
        Assert.assertEquals(30, response.getUsage().getTotalTokens());

        // 聚合后的 toolCalls：call_id 映射为 id
        Assert.assertNotNull(response.getToolCalls());
        Assert.assertEquals(1, response.getToolCalls().size());
        Assert.assertEquals("call_mock_1", response.getToolCalls().get(0).getId());
        Assert.assertEquals("get_weather", response.getToolCalls().get(0).getName());
        Assert.assertEquals("{\"city\":\"hangzhou\"}", response.getToolCalls().get(0).getArguments());

        // function_call 增量通过 onChunk 分片暴露：added 1 帧 + arguments.delta 2 帧
        List<ToolCall> toolCallChunks = new ArrayList<>();
        for (ChatChunk chunk : listener.chunks) {
            if (chunk.getToolCalls() != null) {
                toolCallChunks.addAll(chunk.getToolCalls());
            }
        }
        Assert.assertEquals(3, toolCallChunks.size());
        Assert.assertEquals("call_mock_1", toolCallChunks.get(0).getId());
        Assert.assertEquals("get_weather", toolCallChunks.get(0).getName());
        // 参数为累积值：空参数表现为 "{}"
        Assert.assertEquals("{}", toolCallChunks.get(0).getArguments());
        Assert.assertEquals("{\"city\"", toolCallChunks.get(1).getArguments());
        Assert.assertEquals("{\"city\":\"hangzhou\"}", toolCallChunks.get(2).getArguments());
    }

    /**
     * 非流式：output 项解析、usage 字段映射、finish_reason 推断
     */
    @Test
    public void nonStreamParsesOutputItems() throws Exception {
        ChatResponse response = model().chat("hi").get(10, TimeUnit.SECONDS);
        Assert.assertEquals("hello", response.getContent());
        Assert.assertEquals("thinking", response.getReasoningContent());
        Assert.assertEquals("resp_mock_001", response.getId());
        Assert.assertEquals("mock-model", response.getModel());
        Assert.assertEquals("tool_calls", response.getFinishReason());
        Assert.assertNotNull(response.getUsage());
        Assert.assertEquals(10, response.getUsage().getPromptTokens());
        Assert.assertEquals(20, response.getUsage().getCompletionTokens());
        Assert.assertEquals(30, response.getUsage().getTotalTokens());
        Assert.assertEquals("call_mock_1", response.getToolCalls().get(0).getId());
    }

    /**
     * 请求映射：system 并入 instructions、多轮工具对话转 function_call /
     * function_call_output 项、工具定义为扁平格式
     */
    @Test
    public void requestMapsMessagesToInputItems() throws Exception {
        ToolCall toolCall = new ToolCall();
        toolCall.setIndex(0);
        toolCall.setId("call_1");
        toolCall.setName("get_weather");
        toolCall.setArguments("{\"city\":\"hangzhou\"}");

        List<Message> messages = new ArrayList<>();
        messages.add(Message.ofSystem("你是一个天气助手"));
        messages.add(Message.ofUser("杭州天气"));
        messages.add(Message.ofAssistant(null, Collections.singletonList(toolCall)));
        messages.add(Message.ofTool("call_1", "sunny"));

        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer captureServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        captureServer.createContext("/v1/responses", exchange -> {
            capturedBody.set(new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8));
            byte[] bytes = mockResponse("mock-model", "completed").toJSONString().getBytes(StandardCharsets.UTF_8);
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
                o.system("保持简洁");
                o.specProvider(OpenAiResponsesProvider::new);
            });
            Tool tool = Tool.of("get_weather").description("查询天气").addStringParam("city", "城市名");
            chatModel.chat(messages, Collections.singletonList(tool)).get(10, TimeUnit.SECONDS);

            Assert.assertNotNull(capturedBody.get());
            JSONObject body = JSON.parseObject(capturedBody.get());
            // system 消息与 options.system 合并为顶层 instructions，messages 数组不存在
            Assert.assertEquals("保持简洁\n你是一个天气助手", body.getString("instructions"));
            Assert.assertNull(body.getJSONArray("messages"));
            Assert.assertEquals("/v1/responses 端点", "mock-model", body.getString("model"));

            // input 项：user 文本 + assistant function_call + tool function_call_output
            JSONArray input = body.getJSONArray("input");
            Assert.assertEquals(3, input.size());
            JSONObject userItem = input.getJSONObject(0);
            Assert.assertEquals("user", userItem.getString("role"));
            Assert.assertEquals("杭州天气", userItem.getString("content"));

            JSONObject functionCall = input.getJSONObject(1);
            Assert.assertEquals("function_call", functionCall.getString("type"));
            Assert.assertEquals("call_1", functionCall.getString("call_id"));
            Assert.assertEquals("get_weather", functionCall.getString("name"));
            Assert.assertEquals("{\"city\":\"hangzhou\"}", functionCall.getString("arguments"));

            JSONObject functionOutput = input.getJSONObject(2);
            Assert.assertEquals("function_call_output", functionOutput.getString("type"));
            Assert.assertEquals("call_1", functionOutput.getString("call_id"));
            Assert.assertEquals("sunny", functionOutput.getString("output"));

            // 工具定义为 Responses 扁平格式（无 function 嵌套）
            JSONArray tools = body.getJSONArray("tools");
            Assert.assertEquals(1, tools.size());
            JSONObject toolJson = tools.getJSONObject(0);
            Assert.assertEquals("function", toolJson.getString("type"));
            Assert.assertEquals("get_weather", toolJson.getString("name"));
            Assert.assertNull("扁平格式不应有 function 嵌套", toolJson.getJSONObject("function"));
            Assert.assertNotNull(toolJson.getJSONObject("parameters"));
        } finally {
            captureServer.stop(0);
        }
    }

    /**
     * incomplete 终态：max_output_tokens 截断映射为 finish_reason=length
     */
    @Test
    public void incompleteStatusMapsFinishReason() throws Exception {
        HttpServer incompleteServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        incompleteServer.createContext("/v1/responses", exchange -> {
            JSONObject response = new JSONObject();
            response.put("id", "resp_mock_002");
            response.put("object", "response");
            response.put("model", "mock-model");
            response.put("status", "incomplete");
            response.put("incomplete_details", JSONObject.of("reason", "max_output_tokens"));
            response.put("output", Collections.singletonList(JSONObject.of("type", "message", "role", "assistant",
                    "content", Collections.singletonList(JSONObject.of("type", "output_text", "text", "truncat")))));
            byte[] bytes = response.toJSONString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        incompleteServer.start();
        try {
            String url = "http://127.0.0.1:" + incompleteServer.getAddress().getPort() + "/v1";
            ChatModel chatModel = FeatAI.chatModel(o -> {
                o.baseUrl(url);
                o.model("mock-model");
                o.specProvider(OpenAiResponsesProvider::new);
            });
            ChatResponse response = chatModel.chat("hi").get(10, TimeUnit.SECONDS);
            Assert.assertEquals("truncat", response.getContent());
            Assert.assertEquals("length", response.getFinishReason());
        } finally {
            incompleteServer.stop(0);
        }
    }
}
