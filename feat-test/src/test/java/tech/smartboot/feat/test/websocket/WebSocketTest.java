package tech.smartboot.feat.test.websocket;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.WebSocketClient;
import tech.smartboot.feat.core.client.WebSocketListener;
import tech.smartboot.feat.core.common.codec.websocket.CloseReason;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.upgrade.websocket.WebSocketUpgrade;
import tech.smartboot.feat.test.BastTest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class WebSocketTest extends BastTest {
    private HttpServer bootstrap;
    private final int port = 8080;
    private int idleTimeout = 2000;

    @Before
    public void init() {

        bootstrap = Feat.httpServer(opts -> opts.debug(true)).httpHandler(req -> req.upgrade(new WebSocketUpgrade(idleTimeout) {
            @Override
            public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String data) {
                System.out.println(data);
                response.sendTextMessage(data);
            }
        })).listen(port);
    }

    @Test
    public void testWebSocket() throws IOException, ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        String message = "hello world";
        WebSocketClient webSocketClient = Feat.websocket("ws://localhost:" + port, new WebSocketListener() {
            @Override
            public void onOpen(WebSocketClient client, tech.smartboot.feat.core.client.WebSocketResponse response) {
                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onMessage(WebSocketClient client, String message) {
                System.out.println(message);
                future.complete(message);
            }

        });
        Assert.assertEquals(message, future.get());
        webSocketClient.close();
    }

    @Test
    public void testWebSocket1() throws IOException, ExecutionException, InterruptedException {
        CompletableFuture<Long> future = new CompletableFuture<>();
        WebSocketClient webSocketClient = new WebSocketClient("ws://localhost:" + port);
        webSocketClient.options().debug(true);
        String message = "hello world";

        webSocketClient.connect(new WebSocketListener() {
            @Override
            public void onOpen(WebSocketClient client, tech.smartboot.feat.core.client.WebSocketResponse response) {
                try {
                    webSocketClient.sendMessage(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onClose(WebSocketClient client, tech.smartboot.feat.core.client.WebSocketResponse response, CloseReason reason) {
                future.complete(System.currentTimeMillis());
            }
        });
        long cost = System.currentTimeMillis() - future.get();
        Assert.assertTrue(-cost > idleTimeout);
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}
