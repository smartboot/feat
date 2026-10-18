/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 *
 */

package tech.smartboot.feat.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * 复现：服务端在响应体未传输完成时关闭连接，客户端 future 永久挂起。
 * <p>
 * HttpResponseImpl.onClose() 仅在 STATE_CONNECTION_CLOSE / STATE_STREAM 状态下才会
 * complete future。若断连发生在 STATE_CONTENT_LENGTH / STATE_CHUNK_* 等状态（body
 * 未收完），future 既不 complete 也不 completeExceptionally，调用方无限等待。
 * <p>
 * 断言的是期望行为：future 应在超时内异常完成。缺陷修复前该测试失败（超时挂死），
 * 修复后作为回归防线转绿。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HttpClientMidCloseTest {
    private static final int AWAIT_SECONDS = 5;

    private HttpClient client;
    private ServerSocket serverSocket;

    @After
    public void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    /**
     * 对照组：完整响应后关闭连接，future 正常完成。
     * 用于证明测试基建可用，挂死现象仅出现在"响应体中途断开"场景。
     */
    @Test
    public void completeResponseThenClose_completesNormally() throws Exception {
        startRawServer("HTTP/1.1 200 OK\r\nContent-Length: 10\r\n\r\n",
                "hello feat".getBytes(StandardCharsets.UTF_8));
        HttpResponse response = submit().get(AWAIT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals("hello feat", response.body());
    }

    /**
     * Content-Length 声明 100 字节，实际只发送 10 字节后服务端关闭连接。
     */
    @Test
    public void contentLengthTruncated_serverCloses_completesExceptionally() throws Exception {
        startRawServer("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n",
                "0123456789".getBytes(StandardCharsets.UTF_8));
        expectFailureWithHangReproduced(submit());
    }

    /**
     * Transfer-Encoding: chunked 响应，发送一个 chunk 后未发送终止块即关闭连接。
     */
    @Test
    public void chunkedTruncated_serverCloses_completesExceptionally() throws Exception {
        startRawServer("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nhello\r\n",
                new byte[0]);
        expectFailureWithHangReproduced(submit());
    }

    private CompletableFuture<HttpResponse> submit() throws IOException {
        client = new HttpClient("127.0.0.1", serverSocket.getLocalPort());
        client.options().debug(true);
        return client.get("/test").submit();
    }

    private void expectFailureWithHangReproduced(CompletableFuture<HttpResponse> future) throws Exception {
        try {
            HttpResponse response = future.get(AWAIT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            Assert.fail("服务端中断响应体后关闭连接，future 不应正常完成，实际得到: status="
                    + response.statusCode() + " body=" + response.body());
        } catch (TimeoutException e) {
            throw new AssertionError("复现缺陷：响应体未接收完成时服务端关闭连接，future 永久挂起。"
                    + "HttpResponseImpl.onClose() 未对未完成状态执行 completeExceptionally", e);
        } catch (java.util.concurrent.ExecutionException e) {
            // 期望行为：断连后 future 异常完成
            Assert.assertTrue("期望断连导致的 IOException，实际: " + e.getCause(),
                    e.getCause() instanceof IOException);
        }
    }

    /**
     * 启动原生 ServerSocket 模拟服务端：读取完整请求头 -> 写入原始响应 -> 关闭连接。
     */
    private void startRawServer(String rawResponseHead, byte[] partialBody) throws IOException {
        serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        Thread server = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                socket.setSoTimeout(5000);
                // 读完整请求，避免未消费数据触发 RST 而非 FIN
                readRequestHead(socket.getInputStream());
                OutputStream out = socket.getOutputStream();
                out.write(rawResponseHead.getBytes(StandardCharsets.ISO_8859_1));
                if (partialBody.length > 0) {
                    out.write(partialBody);
                }
                out.flush();
                // 模拟服务端异常：响应体未发完即关闭连接
                socket.close();
            } catch (IOException ignored) {
            }
        });
        server.setDaemon(true);
        server.start();
    }

    private void readRequestHead(InputStream in) throws IOException {
        // 读取直到出现 CRLFCRLF（请求头结束符）
        byte[] window = new byte[4];
        int b;
        while ((b = in.read()) != -1) {
            window[0] = window[1];
            window[1] = window[2];
            window[2] = window[3];
            window[3] = (byte) b;
            if (window[0] == '\r' && window[1] == '\n' && window[2] == '\r' && window[3] == '\n') {
                return;
            }
        }
    }
}
