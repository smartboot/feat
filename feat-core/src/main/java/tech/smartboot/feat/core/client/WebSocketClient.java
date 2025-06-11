/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client;

import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.StringUtils;
import tech.smartboot.feat.core.client.impl.WebSocketRequestImpl;
import tech.smartboot.feat.core.client.impl.WebSocketResponseImpl;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpMethod;
import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.codec.websocket.CloseReason;
import tech.smartboot.feat.core.common.codec.websocket.Decoder;
import tech.smartboot.feat.core.common.codec.websocket.WebSocket;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
public class WebSocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class);
    private static final byte[] maskKey = new byte[4];
    private static final SecureRandom secureRandom = new SecureRandom();

    static {
        secureRandom.nextBytes(maskKey);
    }

    private final WebSocketOptions options;

    /**
     * Header: Host
     */
    private final String hostHeader;
    /**
     * 客户端Client
     */
    private AioQuickClient client;

    private boolean connected;

    private boolean firstConnected = true;

    /**
     * 消息处理器
     */
    private final HttpMessageProcessor processor = new HttpMessageProcessor();

    private final String uri;
    private WebSocketRequestImpl request;


    public static void main(String[] args) throws IOException {
        WebSocketClient client = new WebSocketClient("ws://localhost:8080");
        client.options().debug(true);
        client.connect(new WebSocketListener() {
            @Override
            public void onOpen(WebSocketClient client, WebSocketResponse session) {
                try {
                    client.sendMessage("hello");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onMessage(WebSocketClient client, String message) {
                System.out.println(message);
            }
        });
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    client.sendMessage("aaa" + System.currentTimeMillis());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public WebSocketClient(String url) {
        int schemaIndex = url.indexOf("://");
        if (schemaIndex == -1) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        String schema = url.substring(0, schemaIndex);
        int uriIndex = url.indexOf("/", schemaIndex + 3);
        int portIndex = url.indexOf(":", schemaIndex + 3);
        boolean ws = FeatUtils.SCHEMA_WS.equals(schema);
        boolean wss = !ws && FeatUtils.SCHEMA_WSS.equals(schema);

        if (!ws && !wss) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        String host;
        int port;
        if (portIndex > 0) {
            host = url.substring(schemaIndex + 3, portIndex);
            port = FeatUtils.toInt(uriIndex > 0 ? url.substring(portIndex + 1, uriIndex) : url.substring(portIndex + 1), -1);
        } else if (uriIndex > 0) {
            host = url.substring(schemaIndex + 3, uriIndex);
            port = wss ? 443 : 80;
        } else {
            host = url.substring(schemaIndex + 3);
            port = wss ? 443 : 80;
        }
        if (port == -1) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        this.options = new WebSocketOptions(host, port);
        options.setWss(wss);
        hostHeader = options.getHost() + ":" + options.getPort();
        this.uri = uriIndex > 0 ? url.substring(uriIndex) : "/";

    }

    public WebSocketOptions options() {
        return options;
    }

    public void connect(WebSocketListener listener) throws IOException {
        if (connected) {
            AioSession session = client.getSession();
            if (session == null || session.isInvalid()) {
                close();
                connect(listener);
            }
            return;
        }

        try {
            if (firstConnected) {
                boolean noneSslPlugin = true;
                for (Plugin responsePlugin : options.getPlugins()) {
                    processor.addPlugin(responsePlugin);
                    if (responsePlugin instanceof SslPlugin) {
                        noneSslPlugin = false;
                    }
                }
                if (noneSslPlugin && options.isWss()) {
                    processor.addPlugin(new SslPlugin<>(new ClientSSLContextFactory()));
                }

                firstConnected = false;
            }
            connected = true;
            client = options.getProxy() == null ? new AioQuickClient(options.getHost(), options.getPort(), processor, processor) : new AioQuickClient(options.getProxy().getProxyHost(), options.getProxy().getProxyPort(), processor, processor);
            client.setReadBufferSize(options.readBufferSize());
            if (options.getConnectTimeout() > 0) {
                client.connectTimeout(options.getConnectTimeout());
            }
            if (options.group() == null) {
                client.start();
            } else {
                client.start(options.group());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        AioSession session = client.getSession();
        DecoderUnit attachment = session.getAttachment();
        CompletableFuture<WebSocketResponseImpl> completableFuture = new CompletableFuture<>();
        completableFuture.thenAccept(new Consumer<WebSocketResponseImpl>() {
            @Override
            public void accept(WebSocketResponseImpl webSocketResponse) {
                try {
                    switch (webSocketResponse.getFrameOpcode()) {
                        case WebSocket.OPCODE_TEXT:
                            listener.onMessage(WebSocketClient.this, new String(webSocketResponse.getPayload(), StandardCharsets.UTF_8));
                            break;
                        case WebSocket.OPCODE_BINARY:
                            listener.onMessage(WebSocketClient.this, webSocketResponse.getPayload());
                            break;
                        case WebSocket.OPCODE_CLOSE:
                            try {
                                listener.onClose(WebSocketClient.this, webSocketResponse, new CloseReason(webSocketResponse.getPayload()));
                            } finally {
                                WebSocketClient.this.close();
                            }
                            break;
                        case WebSocket.OPCODE_PING:
                            System.out.println("ping...");
                            WebSocket.send(request.getOutputStream(), WebSocket.OPCODE_PONG, webSocketResponse.getPayload(), 0, webSocketResponse.getPayload().length);
//                        webSocketResponse.pong(webSocketResponse.getPayload());
                            break;
                        case WebSocket.OPCODE_PONG:
//                                handlePong(request, response);
                            break;
                        case WebSocket.OPCODE_CONTINUE:
                            LOGGER.warn("unSupport OPCODE_CONTINUE now,ignore payload: {}", StringUtils.toHexString(webSocketResponse.getPayload()));
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                } catch (Throwable throwable) {
                    listener.onError(WebSocketClient.this, webSocketResponse, throwable);
//                throw throwable;
                } finally {
                    CompletableFuture<WebSocketResponseImpl> completableFuture = new CompletableFuture<>();
                    completableFuture.thenAccept(this);
                    webSocketResponse.setFuture(completableFuture);
                    webSocketResponse.reset();
                    attachment.setState(DecoderUnit.STATE_BODY);
                }
            }
        });
        WebSocketResponseImpl webSocketResponse = new WebSocketResponseImpl(session, completableFuture) {
            @Override
            public void onHeaderComplete() {
                if (statusCode() != HttpStatus.SWITCHING_PROTOCOLS.value()) {
                    listener.onClose(WebSocketClient.this, this, new CloseReason(CloseReason.WRONG_CODE, ""));
                    return;
                }
                listener.onOpen(WebSocketClient.this, this);
            }

            @Override
            public void onBodyStream(ByteBuffer buffer) {
                Decoder decoder = getDecoder().decode(buffer, this);
                setDecoder(decoder);
                if (decoder == WebSocket.PAYLOAD_FINISH) {
                    getFuture().complete(this);
                }
            }
        };

        attachment.setResponse(webSocketResponse);
        initRest();
    }

    private void initRest() throws IOException {
        request = new WebSocketRequestImpl(client.getSession());
        request.setUri(uri);
        request.setMethod(HttpMethod.GET);
        request.setProtocol(HttpProtocol.HTTP_11.getProtocol());
        request.addHeader(HeaderName.HOST, hostHeader);
        request.addHeader(HeaderName.UPGRADE, HeaderValue.Upgrade.WEBSOCKET);
        request.setHeader(HeaderName.CONNECTION, HeaderValue.Connection.UPGRADE);
        request.setHeader(HeaderName.Sec_WebSocket_Key, generateSecWebSocketKey());
        request.setHeader(HeaderName.Sec_WebSocket_Version, "13");
//        request.setHeader(HeaderNameEnum.Sec_WebSocket_Protocol.getName(), HeaderValue.PERMESSAGE_DEFLATE.getName());
        request.getOutputStream().flush();
    }

    /**
     * 在客户端握手中的|Sec-WebSocket-Key|头字段包括一个 base64 编码的值，如果解码，长度是 16 字节。
     */
    private String generateSecWebSocketKey() {
        byte[] keyBytes = new byte[16];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }


    /**
     * 发送消息
     *
     * @param message 消息内容
     * @throws IOException 如果在发送消息过程中发生I/O错误
     */
    public void sendMessage(String message) throws IOException {
        // 发送消息到服务器
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        sendMask(request.getOutputStream(), WebSocket.OPCODE_TEXT, bytes, 0, bytes.length);
        request.getOutputStream().flush();
    }

    /**
     * 发送二进制消息
     *
     * @param bytes 二进制消息内容
     * @throws IOException 如果在发送过程中发生I/O错误
     */
    public void sendBinary(byte[] bytes) throws IOException {
        // 发送二进制消息到服务器
        sendMask(request.getOutputStream(), WebSocket.OPCODE_BINARY, bytes, 0, bytes.length);
        request.getOutputStream().flush();
    }

    public void close() {
        if (!connected) {
            return;
        }
        try {
            sendMask(request.getOutputStream(), WebSocket.OPCODE_CLOSE, new CloseReason(CloseReason.NORMAL_CLOSURE, "").toBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connected = false;
            client.shutdown();
        }
    }


    private void sendMask(OutputStream outputStream, byte opCode, byte[] bytes) throws IOException {
        sendMask(outputStream, opCode, bytes, 0, bytes.length);
    }

    private void sendMask(OutputStream outputStream, byte opCode, byte[] bytes, int offset, int len) throws IOException {
//        // 生成掩码密钥值
//        byte[] maskKey = new byte[4];
//        Random random = new Random();
//        random.nextBytes(maskKey);

        int maxlength;
        if (len < FeatUtils.WS_PLAY_LOAD_126) {
            maxlength = 6 + len;
        } else {
            maxlength = 8 + Math.min(FeatUtils.WS_DEFAULT_MAX_FRAME_SIZE, len);
        }
        byte[] writBytes = new byte[maxlength];
        do {
            int payloadLength = len - offset;
            if (payloadLength > FeatUtils.WS_DEFAULT_MAX_FRAME_SIZE) {
                payloadLength = FeatUtils.WS_DEFAULT_MAX_FRAME_SIZE;
            }
            byte firstByte = offset + payloadLength < len ? (byte) 0x00 : (byte) 0x80;
            if (offset == 0) {
                firstByte |= opCode;
            } else {
                firstByte |= WebSocket.OPCODE_CONTINUE;
            }
            byte secondByte = payloadLength < FeatUtils.WS_PLAY_LOAD_126 ? (byte) payloadLength : FeatUtils.WS_PLAY_LOAD_126;
            writBytes[0] = firstByte;
            writBytes[1] = (byte) (secondByte | 0x80);
            if (secondByte == FeatUtils.WS_PLAY_LOAD_126) {
                writBytes[2] = (byte) (payloadLength >> 8 & 0xff);
                writBytes[3] = (byte) (payloadLength & 0xff);
                System.arraycopy(maskKey, 0, writBytes, 4, maskKey.length);
                // 对消息进行掩码处理
                for (int i = 0; i < payloadLength; i++) {
                    writBytes[8 + i] = (byte) (bytes[i] ^ maskKey[i % 4]);
                }
            } else {
                System.arraycopy(maskKey, 0, writBytes, 2, maskKey.length);
                // 对消息进行掩码处理
                for (int i = 0; i < payloadLength; i++) {
                    writBytes[6 + i] = (byte) (bytes[i] ^ maskKey[i % 4]);
                }
            }
            outputStream.write(writBytes, 0, payloadLength < FeatUtils.WS_PLAY_LOAD_126 ? 6 + payloadLength : 8 + payloadLength);
            offset += payloadLength;
        } while (offset < len);
    }
}
