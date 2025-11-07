/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.server.HttpServer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HttpProtocolTest {

    private static final int SERVER_PORT = 8082;
    private HttpServer bootstrap;

    @Before
    public void init() {
        bootstrap = new HttpServer();
        bootstrap.httpHandler(request -> {
            request.getResponse().write("Hello World");
        });
        bootstrap.options().debug(true);
        bootstrap.listen(SERVER_PORT);
    }

    /**
     * 测试不支持的HTTP协议版本
     * 覆盖HttpRequestProtocol中的异常分支
     */
    @Test
    public void testUnsupportedHttpVersion() throws Exception {
        // 使用原始Socket发送不支持的HTTP协议版本
        Socket socket = new Socket("127.0.0.1", SERVER_PORT);
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        // 发送不支持的HTTP协议版本请求
        String rawRequest = "GET / HTTP/1.2\r\nHost: localhost\r\n\r\n";
        out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
        out.flush();

        // 读取服务器响应
        byte[] responseBuffer = new byte[1024];
        int bytesRead = in.read(responseBuffer);

        // 验证服务器返回了错误响应
        String response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);
        Assert.assertTrue("Response should contain 400 Bad Request",
                response.contains("400") || response.contains("Bad Request"));
    }

    /**
     * 测试HTTP/1.1协议的正常解析
     */
    @Test
    public void testHttp11Protocol() throws Exception {
        HttpClient httpClient = new HttpClient("127.0.0.1", SERVER_PORT);
        HttpResponse response = httpClient.get("/").submit().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());
        Assert.assertEquals("Hello World", response.body());
    }

    /**
     * 测试HTTP/1.0协议的正常解析
     */
    @Test
    public void testHttpProtocol() throws Exception {
        // 使用原始Socket发送不支持的HTTP协议版本
        Socket socket = new Socket("127.0.0.1", SERVER_PORT);
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        // 发送不支持的HTTP协议版本请求
        String rawRequest = "GET / HTTP/1.0\r\nHost: localhost\r\n\r\n";
        out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
        out.flush();

        // 读取服务器响应
        byte[] responseBuffer = new byte[1024];
        int bytesRead = in.read(responseBuffer);

        // 验证服务器返回了错误响应
        String response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);
        Assert.assertTrue(response.contains("Hello World"));
        socket.close();
        out.close();
        in.close();


        socket = new Socket("127.0.0.1", SERVER_PORT);
        out = socket.getOutputStream();
        in = socket.getInputStream();

        // 发送不支持的HTTP协议版本请求
        rawRequest = "GET /   HTTP/1.0\r\nHost: localhost\r\n\r\n";
        out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
        out.flush();

        // 读取服务器响应
        responseBuffer = new byte[1024];
        bytesRead = in.read(responseBuffer);

        // 验证服务器返回了错误响应
        response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);
        Assert.assertTrue(response.contains("Hello World"));
        socket.close();
        out.close();
        in.close();


        socket = new Socket("127.0.0.1", SERVER_PORT);
        out = socket.getOutputStream();
        in = socket.getInputStream();

        // 发送不支持的HTTP协议版本请求
        rawRequest = "GET /   HTTP/1.1\r\nHost: localhost\r\n\r\n";
        out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
        out.flush();

        // 读取服务器响应
        responseBuffer = new byte[1024];
        bytesRead = in.read(responseBuffer);

        // 验证服务器返回了错误响应
        response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);
        Assert.assertTrue(response.contains("Hello World"));
        socket.close();
        out.close();
        in.close();


        socket = new Socket("127.0.0.1", SERVER_PORT);
        out = socket.getOutputStream();
        in = socket.getInputStream();

        // 发送不支持的HTTP协议版本请求
        rawRequest = "GET /   HTTP/2.0\r\nHost: localhost\r\n\r\n";
        out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
        out.flush();

        // 读取服务器响应
        responseBuffer = new byte[1024];
        bytesRead = in.read(responseBuffer);

        // 验证服务器返回了错误响应
        response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);
        Assert.assertTrue(response.contains("Hello World"));
        socket.close();
        out.close();
        in.close();
    }

    /**
     * 测试畸形HTTP协议版本
     * 覆盖HttpRequestProtocol中的异常分支
     */
    @Test
    public void testMalformedHttpVersion() throws Exception {
        // 使用原始Socket发送畸形HTTP协议版本
        Socket socket = new Socket("127.0.0.1", SERVER_PORT);
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        // 发送畸形的HTTP协议版本请求
        String rawRequest = "GET / HTTX/1.1\r\nHost: localhost\r\n\r\n";
        out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
        out.flush();

        // 读取服务器响应
        byte[] responseBuffer = new byte[1024];
        int bytesRead = in.read(responseBuffer);

        // 验证服务器返回了错误响应
        String response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);
        Assert.assertTrue("Response should contain 400 Bad Request",
                response.contains("400") || response.contains("Bad Request"));

    }

    @After
    public void destroy() {
        if (bootstrap != null) {
            bootstrap.shutdown();
        }
    }
}