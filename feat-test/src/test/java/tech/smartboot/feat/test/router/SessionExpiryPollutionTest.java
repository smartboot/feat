/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.router;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.session.LocalSessionManager;
import tech.smartboot.feat.test.BastTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 会话过期污染响应测试：会话过期由后台定时器线程触发，
 * 不得向已随连接复用的响应对象写入Set-Cookie，
 * 否则同连接上下一个无关请求的响应会被注入清空会话的Cookie
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SessionExpiryPollutionTest extends BastTest {
    private HttpServer bootstrap;

    @Before
    public void init() {
        Router router = new Router();
        //会话超时设为1秒，加速过期触发
        LocalSessionManager sessionManager = new LocalSessionManager();
        sessionManager.getOptions().setTimeout(1);
        router.setSessionManager(sessionManager);
        router.route("/create", ctx -> {
            ctx.session().put("flag", "aa");
            ctx.Response.write("created");
        });
        router.route("/plain", ctx -> ctx.Response.write("ok"));
        router.route("/check", ctx -> {
            String flag = ctx.session().get("flag");
            ctx.Response.write("flag:" + flag);
        });
        router.route("/logout", ctx -> {
            ctx.session().invalidate();
            ctx.Response.write("logged-out");
        });

        bootstrap = new HttpServer();
        bootstrap.httpHandler(router);
        bootstrap.options().debug(true);
        bootstrap.listen(SERVER_PORT);
    }

    @Test
    public void testExpiryDoesNotPolluteNextResponse() throws IOException, InterruptedException {
        try (Socket socket = new Socket("127.0.0.1", SERVER_PORT)) {
            socket.setSoTimeout(10000);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            //请求1：创建会话，响应正常携带会话Cookie
            out.write("GET /create HTTP/1.1\r\nHost: localhost\r\nConnection: keep-alive\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            String response1 = readResponse(in);
            Assert.assertTrue("响应1应携带会话Cookie, resp: " + response1, response1.contains("FEAT_SESSION="));

            //等待会话过期（后台定时器触发invalidate）
            Thread.sleep(3000);

            //请求2：同一连接上的无关请求，响应不得被注入过期的清空Cookie
            out.write("GET /plain HTTP/1.1\r\nHost: localhost\r\nConnection: keep-alive\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            String response2 = readResponse(in);
            String headSection = headerSection(response2);
            Assert.assertFalse("会话过期Cookie被注入到无关响应: " + response2, headSection.contains("FEAT_SESSION="));
            Assert.assertTrue("正常请求应成功: " + response2, response2.startsWith("HTTP/1.1 200"));

            //请求3：携带原会话Cookie，会话必须已在服务端过期（防止测试空转通过）
            String sessionCookie = extractSessionCookie(response1);
            out.write(("GET /check HTTP/1.1\r\nHost: localhost\r\nCookie: " + sessionCookie + "\r\nConnection: keep-alive\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            String response3 = readResponse(in);
            Assert.assertFalse("会话应当已过期（旧实现永不失效）, resp: " + response3, response3.contains("flag:aa"));
        }
    }

    /**
     * 从响应中提取FEAT_SESSION cookie串
     */
    private static String extractSessionCookie(String response) {
        for (String line : response.split("\r\n")) {
            if (line.startsWith("Set-Cookie:") && line.contains("FEAT_SESSION=")) {
                return line.substring("Set-Cookie:".length()).trim().split(";")[0];
            }
        }
        return "";
    }

    @Test
    public void testAppInvalidateDoesNotPolluteCreatingConnection() throws IOException {
        try (Socket socket1 = new Socket("127.0.0.1", SERVER_PORT);
             Socket socket2 = new Socket("127.0.0.1", SERVER_PORT)) {
            socket1.setSoTimeout(10000);
            socket2.setSoTimeout(10000);

            //连接1：创建会话（会话持有的创建请求属于连接1）
            socket1.getOutputStream().write("GET /create HTTP/1.1\r\nHost: localhost\r\nConnection: keep-alive\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            String response1 = readResponse(socket1.getInputStream());
            Assert.assertTrue("响应1应携带会话Cookie, resp: " + response1, response1.contains("FEAT_SESSION="));
            String sessionCookie = extractSessionCookie(response1);

            //连接2：携带会话Cookie调用invalidate
            //旧实现会把清空Cookie写进会话创建请求（连接1）的已复用响应对象
            socket2.getOutputStream().write(("GET /logout HTTP/1.1\r\nHost: localhost\r\nCookie: " + sessionCookie + "\r\nConnection: keep-alive\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            String logout = readResponse(socket2.getInputStream());
            Assert.assertTrue("logout应成功: " + logout, logout.startsWith("HTTP/1.1 200"));

            //连接1：后续请求的响应不得被注入清空Cookie
            socket1.getOutputStream().write("GET /plain HTTP/1.1\r\nHost: localhost\r\nConnection: keep-alive\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            String response2 = readResponse(socket1.getInputStream());
            Assert.assertFalse("创建连接的后续响应被注入清空Cookie: " + response2, headerSection(response2).contains("FEAT_SESSION="));
            Assert.assertTrue("连接1后续请求应正常: " + response2, response2.startsWith("HTTP/1.1 200"));
        }
    }

    private static String headerSection(String response) {
        int headEnd = response.indexOf("\r\n\r\n");
        return headEnd < 0 ? response : response.substring(0, headEnd);
    }

    /**
     * 持续读取直至chunked终止块（0\r\n[trailer行]*\r\n）结束
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
