/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */
package tech.smartboot.feat.test.client;

import tech.smartboot.feat.core.client.HttpClient;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class HttpClientUrlTest {
    @Test
    public void queryWithoutPathUsesRootRequestTarget() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            server.setSoTimeout(5000);
            CompletableFuture<String> requestLine = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    String line = reader.readLine();
                    for (String header = reader.readLine(); header != null && !header.isEmpty(); header = reader.readLine()) {
                        // Consume the remaining headers.
                    }
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    return line;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            HttpClient client = new HttpClient("http://127.0.0.1:" + server.getLocalPort() + "?next=/a:b");
            try {
                client.get().submit().get(5, TimeUnit.SECONDS);
                assertEquals("GET /?next=/a:b HTTP/1.1", requestLine.get(5, TimeUnit.SECONDS));
            } finally {
                client.close();
            }
        }
    }

    @Test
    public void queryWithoutPortDoesNotBecomePartOfHost() {
        HttpClient client = new HttpClient("http://example.com?x=1");
        try {
            assertEquals("example.com", client.options().getHost());
            assertEquals(80, client.options().getPort());
        } finally {
            client.close();
        }
    }
}
