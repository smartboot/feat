/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.mcp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.mcp.model.Tool;
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.ai.mcp.server.model.ServerResource;
import tech.smartboot.feat.ai.mcp.server.model.ServerTool;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * MCP 协议符合性回归测试。
 * <p>
 * 覆盖版本协商、握手校验、JSON-RPC 错误码、cursor 分页与资源订阅等规范细节。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class McpConformanceTest {
    private static final int PORT = 3003;
    private static final String ENDPOINT = "http://localhost:" + PORT + "/mcp";
    private static final String PROTOCOL_VERSION = "2025-06-18";

    private HttpServer httpServer;
    private String sessionId;

    @Before
    public void init() throws Exception {
        McpServer mcp = new McpServer(opt -> opt.toolEnable().resourceEnable().setPageSize(2));
        for (int i = 0; i < 3; i++) {
            mcp.addTool(ServerTool.of("tool" + i).description("测试工具").inputSchema(Tool.stringProperty("a", "参数"))
                    .doAction(context -> {
                        return ToolResult.ofText("ok");
                    }));
        }
        mcp.addResource(ServerResource.ofText("file:///a.txt", "a.txt", "text/plain", "aaa"));

        Router router = new Router();
        mcp.enableStreamable(router);
        httpServer = Feat.httpServer(opt -> opt.debug(false)).httpHandler(router).listen(PORT);

        sessionId = initialize(PROTOCOL_VERSION);
        // 握手第二步：服务端只接受 notifications/initialized
        JSONObject handshake = post(JSONObject.of("jsonrpc", "2.0", "method", "notifications/initialized"), sessionId);
        Assert.assertEquals(202, handshake.getIntValue("_status"));
    }

    @After
    public void destroy() {
        if (httpServer != null) {
            httpServer.shutdown();
        }
    }

    /**
     * 服务端不支持客户端声明的版本时，应回写自身支持的版本，由客户端决定是否断开。
     */
    @Test
    public void testVersionNegotiationFallback() throws Exception {
        JSONObject response = post(JSONObject.of("jsonrpc", "2.0", "id", 1, "method", "initialize",
                "params", JSONObject.of("protocolVersion", "1999-01-01",
                        "capabilities", new JSONObject(),
                        "clientInfo", JSONObject.of("name", "test", "version", "1.0"))), null);
        Assert.assertEquals(PROTOCOL_VERSION, response.getJSONObject("result").getString("protocolVersion"));
    }

    /**
     * initialize 缺少 protocolVersion 时应返回 -32602。
     */
    @Test
    public void testMissingProtocolVersion() throws Exception {
        JSONObject response = post(JSONObject.of("jsonrpc", "2.0", "id", 1, "method", "initialize",
                "params", JSONObject.of("capabilities", new JSONObject(),
                        "clientInfo", JSONObject.of("name", "test", "version", "1.0"))), null);
        Assert.assertEquals(-32602, response.getJSONObject("error").getIntValue("code"));
    }

    /**
     * 未注册的方法应返回 -32601，而不是抛异常或返回空结果。
     */
    @Test
    public void testMethodNotFound() throws Exception {
        JSONObject response = post(JSONObject.of("jsonrpc", "2.0", "id", 2, "method", "no/such/method"), sessionId);
        Assert.assertEquals(-32601, response.getJSONObject("error").getIntValue("code"));
    }

    /**
     * 调用不存在的工具属于参数错误，错误码必须是 -32602。
     */
    @Test
    public void testUnknownToolReturnsInvalidParams() throws Exception {
        JSONObject response = post(JSONObject.of("jsonrpc", "2.0", "id", 3, "method", "tools/call",
                "params", JSONObject.of("name", "not_exist", "arguments", new JSONObject())), sessionId);
        Assert.assertEquals(-32602, response.getJSONObject("error").getIntValue("code"));
    }

    /**
     * tools/list 必须支持 cursor 分页，且末页不再返回 nextCursor。
     */
    @Test
    public void testToolsListPagination() throws Exception {
        JSONObject firstPage = post(JSONObject.of("jsonrpc", "2.0", "id", 4, "method", "tools/list"), sessionId)
                .getJSONObject("result");
        Assert.assertEquals(2, firstPage.getJSONArray("tools").size());
        String cursor = firstPage.getString("nextCursor");
        Assert.assertNotNull(cursor);

        JSONObject secondPage = post(JSONObject.of("jsonrpc", "2.0", "id", 5, "method", "tools/list",
                "params", JSONObject.of("cursor", cursor)), sessionId).getJSONObject("result");
        Assert.assertEquals(1, secondPage.getJSONArray("tools").size());
        Assert.assertNull(secondPage.getString("nextCursor"));
    }

    /**
     * 非法 cursor 应返回 -32602。
     */
    @Test
    public void testInvalidCursor() throws Exception {
        JSONObject response = post(JSONObject.of("jsonrpc", "2.0", "id", 6, "method", "tools/list",
                "params", JSONObject.of("cursor", "not-a-cursor")), sessionId);
        Assert.assertEquals(-32602, response.getJSONObject("error").getIntValue("code"));
    }

    /**
     * 声明了 resources.subscribe 能力后，订阅请求必须得到应答。
     */
    @Test
    public void testResourceSubscribe() throws Exception {
        JSONObject response = post(JSONObject.of("jsonrpc", "2.0", "id", 7, "method", "resources/subscribe",
                "params", JSONObject.of("uri", "file:///a.txt")), sessionId);
        Assert.assertNull(response.getJSONObject("error"));
        Assert.assertNotNull(response.getJSONObject("result"));

        JSONObject unsubscribe = post(JSONObject.of("jsonrpc", "2.0", "id", 8, "method", "resources/unsubscribe",
                "params", JSONObject.of("uri", "file:///a.txt")), sessionId);
        Assert.assertNull(unsubscribe.getJSONObject("error"));
    }

    /**
     * 订阅不存在的资源应返回 -32602。
     */
    @Test
    public void testSubscribeUnknownResource() throws Exception {
        JSONObject response = post(JSONObject.of("jsonrpc", "2.0", "id", 9, "method", "resources/subscribe",
                "params", JSONObject.of("uri", "file:///missing.txt")), sessionId);
        Assert.assertEquals(-32602, response.getJSONObject("error").getIntValue("code"));
    }

    /**
     * 字符串 id 必须原样回传，不能被强制转成数字。
     */
    @Test
    public void testStringRequestId() throws Exception {
        JSONObject response = post(JSONObject.of("jsonrpc", "2.0", "id", "req-abc", "method", "ping"), sessionId);
        Assert.assertEquals("req-abc", response.getString("id"));
    }

    private String initialize(String protocolVersion) throws Exception {
        JSONObject request = JSONObject.of("jsonrpc", "2.0", "id", 0, "method", "initialize",
                "params", JSONObject.of("protocolVersion", protocolVersion,
                        "capabilities", new JSONObject(),
                        "clientInfo", JSONObject.of("name", "test", "version", "1.0")));
        JSONObject response = post(request, null);
        Assert.assertEquals(PROTOCOL_VERSION, response.getJSONObject("result").getString("protocolVersion"));
        String session = response.getString("_sessionId");
        Assert.assertNotNull(session);
        return session;
    }

    private JSONObject post(JSONObject body, String session) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json, text/event-stream");
        conn.setRequestProperty("Connection", "close");
        if (session != null) {
            conn.setRequestProperty("mcp-session-id", session);
            conn.setRequestProperty("MCP-Protocol-Version", PROTOCOL_VERSION);
        }
        byte[] payload = body.toJSONString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(payload.length);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(payload);
        }

        int status = conn.getResponseCode();
        String text = read(conn.getInputStream());
        JSONObject response = text.isEmpty() ? new JSONObject() : JSONObject.parseObject(text);
        response.put("_status", status);
        response.put("_sessionId", conn.getHeaderField("mcp-session-id"));
        conn.disconnect();
        return response;
    }

    private String read(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int len;
        while ((len = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, len);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
