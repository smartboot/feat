/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.test.BastTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * HTTP响应拆分防护测试：header名/值中的CR/LF注入必须被拒绝，
 * 不得在响应中伪造出额外的header
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HeaderCrlfInjectionTest extends BastTest {
    private HttpServer bootstrap;

    @Before
    public void init() {
        bootstrap = new HttpServer();
        bootstrap.httpHandler(request -> {
            String uri = request.getRequestURI();
            if (uri.equals("/inject-value")) {
                //header值注入：尝试伪造Set-Cookie
                request.getResponse().setHeader("X-Custom", "v1\r\nSet-Cookie: forged=1");
            } else if (uri.equals("/inject-name")) {
                //header名注入
                request.getResponse().setHeader("X-Bad\r\nSet-Cookie: forged=1", "v1");
            } else {
                request.getResponse().setHeader("X-Custom", "v1");
                request.getResponse().write("ok");
            }
        });
        bootstrap.options().debug(true);
        bootstrap.listen(SERVER_PORT);
    }

    @Test
    public void testNormalHeaderUnaffected() throws IOException {
        String response = request("/normal");
        Assert.assertTrue(response.startsWith("HTTP/1.1 200"));
        Assert.assertTrue(response.contains("X-Custom:v1"));
        Assert.assertFalse(response.contains("forged"));
    }

    @Test
    public void testHeaderValueInjectionRejected() throws IOException {
        //旧实现会把\r\n原样写入响应头区，伪造出Set-Cookie: forged=1
        String response = request("/inject-value");
        Assert.assertTrue("应返回500错误而非注入, resp: " + response, response.startsWith("HTTP/1.1 500"));
        Assert.assertFalse("响应头区发生注入: " + response, headerSection(response).contains("Set-Cookie: forged"));
    }

    @Test
    public void testHeaderNameInjectionRejected() throws IOException {
        String response = request("/inject-name");
        Assert.assertTrue("应返回500错误而非注入, resp: " + response, response.startsWith("HTTP/1.1 500"));
        Assert.assertFalse("响应头区发生注入: " + response, headerSection(response).contains("Set-Cookie: forged"));
    }

    /**
     * 提取响应头区（错误页body会包含异常消息文本，不作为注入判定依据）
     */
    private static String headerSection(String response) {
        int headEnd = response.indexOf("\r\n\r\n");
        return headEnd < 0 ? response : response.substring(0, headEnd);
    }

    private static String request(String uri) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", BastTest.SERVER_PORT)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(("GET " + uri + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            return readResponse(socket.getInputStream());
        }
    }

    /**
     * 读取完整响应（chunked或content-length）
     */
    private static String readResponse(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (true) {
            int n = in.read(buf);
            if (n < 0) {
                break;
            }
            bos.write(buf, 0, n);
            String response = new String(bos.toByteArray(), StandardCharsets.ISO_8859_1);
            if (response.contains("\r\n0\r\n") && response.endsWith("\r\n\r\n")) {
                break;
            }
        }
        return new String(bos.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
