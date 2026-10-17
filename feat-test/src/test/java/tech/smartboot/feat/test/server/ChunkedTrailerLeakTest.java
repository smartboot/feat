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
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.test.BastTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * 验证keep-alive连接上响应间的状态隔离：上一响应设置的chunked trailer
 * 不得泄漏到同连接的下一响应（reset()需清理trailerSupplier）
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChunkedTrailerLeakTest extends BastTest {
    private static final String TRAILER_NAME = "X-Session-Trailer";

    private HttpServer bootstrap;

    @Before
    public void init() {
        bootstrap = new HttpServer();
        bootstrap.httpHandler(new HttpHandler() {
            @Override
            public void handle(HttpRequest request, CompletableFuture<Void> future) throws Throwable {
                if (request.getRequestURI().equals("/trailer")) {
                    //响应1：chunked + trailer
                    FeatOutputStream outputStream = (FeatOutputStream) request.getResponse().getOutputStream();
                    outputStream.setTrailerFields(() -> Collections.singletonMap(TRAILER_NAME, "v1"));
                    outputStream.write("Hello".getBytes(StandardCharsets.UTF_8));
                } else {
                    //响应2：普通chunked响应，无trailer
                    request.getResponse().getOutputStream().write("World!".getBytes(StandardCharsets.UTF_8));
                }
                future.complete(null);
            }

            @Override
            public void handle(HttpRequest request) throws Throwable {
                throw new UnsupportedOperationException();
            }
        });
        bootstrap.options().debug(true);
        bootstrap.listen(SERVER_PORT);
    }

    @Test
    public void testTrailerNotLeakedToNextResponse() throws IOException {
        try (Socket socket = new Socket("127.0.0.1", SERVER_PORT)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            //响应1：携带trailer
            out.write("GET /trailer HTTP/1.1\r\nHost: localhost\r\nConnection: keep-alive\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            String response1 = readResponse(in);
            Assert.assertTrue("响应1应携带trailer, resp: " + response1, response1.contains(TRAILER_NAME + ":v1"));
            Assert.assertEquals("Hello", extractBody(response1));

            //响应2：同连接请求，不得携带响应1的trailer
            out.write("GET /plain HTTP/1.1\r\nHost: localhost\r\nConnection: keep-alive\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            String response2 = readResponse(in);
            Assert.assertFalse("上一响应的trailer泄漏到当前响应: " + response2, response2.contains(TRAILER_NAME));
            Assert.assertEquals("World!", extractBody(response2));
        }
    }

    /**
     * 持续读取直至chunked终止块（0\r\n[trailer行]*\r\n）结束
     */
    private static String readResponse(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        String response = "";
        while (true) {
            int n = in.read(buf);
            if (n < 0) {
                break;
            }
            bos.write(buf, 0, n);
            response = new String(bos.toByteArray(), StandardCharsets.ISO_8859_1);
            if (response.contains("\r\n0\r\n") && response.endsWith("\r\n\r\n")) {
                break;
            }
        }
        return response;
    }

    private static String extractBody(String response) {
        int headEnd = response.indexOf("\r\n\r\n");
        int terminal = response.indexOf("\r\n0\r\n", headEnd);
        String chunked = response.substring(headEnd + 4, terminal);
        //格式：<size>\r\n<data>（终止块匹配已含尾部CRLF）
        int crlf = chunked.indexOf("\r\n");
        return chunked.substring(crlf + 2);
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
