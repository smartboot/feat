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
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.test.BastTest;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * chunked编码下write(ByteBuffer, callback)正确性测试：
 * 分别覆盖"复用buffer空间就地写入"与"无空闲空间独立写入"两条路径
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChunkedByteBufferWriteTest extends BastTest {
    private HttpServer bootstrap;

    @Before
    public void init() {
        bootstrap = new HttpServer();
        bootstrap.httpHandler(new HttpHandler() {
            @Override
            public void handle(HttpRequest request, CompletableFuture<Void> future) throws Throwable {
                FeatOutputStream outputStream = (FeatOutputStream) request.getResponse().getOutputStream();
                if (request.getRequestURI().equals("/inplace")) {
                    //场景1：数据区前部与尾部均有空闲空间，块大小行与CRLF就地复用buffer空间
                    ByteBuffer buffer = ByteBuffer.allocate(64);
                    buffer.position(10);
                    buffer.put("Hello".getBytes(StandardCharsets.UTF_8));
                    buffer.limit(buffer.position());
                    buffer.position(10);
                    outputStream.write(buffer, () -> future.complete(null));
                } else if (request.getRequestURI().equals("/flip")) {
                    //场景2：flip后的部分buffer（position=0），块大小行独立写入、CRLF就地复用尾部空间
                    //该形态是原始算法必然抛IndexOutOfBoundsException的现实场景（chunked流最后一个分块）
                    ByteBuffer buffer = ByteBuffer.allocate(64);
                    buffer.put("Hello".getBytes(StandardCharsets.UTF_8));
                    buffer.flip();
                    outputStream.write(buffer, () -> future.complete(null));
                } else {
                    //场景3：buffer无前置空间且已满，块大小行与CRLF走独立写入
                    ByteBuffer buffer = ByteBuffer.allocate(6);
                    buffer.put("World!".getBytes(StandardCharsets.UTF_8));
                    buffer.flip();
                    outputStream.write(buffer, () -> future.complete(null));
                }
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
    public void testInPlace() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Hello", getHttpClient().get("/inplace").submit().get().body());
    }

    @Test
    public void testFlippedPartial() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Hello", getHttpClient().get("/flip").submit().get().body());
    }

    @Test
    public void testFallback() throws ExecutionException, InterruptedException {
        Assert.assertEquals("World!", getHttpClient().get("/fallback").submit().get().body());
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
