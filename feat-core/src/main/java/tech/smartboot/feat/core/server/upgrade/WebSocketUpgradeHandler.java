package tech.smartboot.feat.core.server.upgrade;

import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.StringUtils;
import tech.smartboot.feat.core.common.codec.websocket.BasicFrameDecoder;
import tech.smartboot.feat.core.common.codec.websocket.CloseReason;
import tech.smartboot.feat.core.common.codec.websocket.Decoder;
import tech.smartboot.feat.core.common.codec.websocket.WebSocket;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.utils.SHA1;
import tech.smartboot.feat.core.common.utils.WebSocketUtil;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.impl.AbstractResponse;
import tech.smartboot.feat.core.server.impl.HttpUpgradeHandler;
import tech.smartboot.feat.core.server.impl.WebSocketRequestImpl;
import tech.smartboot.feat.core.server.impl.WebSocketResponseImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WebSocketUpgradeHandler extends HttpUpgradeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketUpgradeHandler.class);
    private static final String WEBSOCKET_13_ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Decoder basicFrameDecoder = new BasicFrameDecoder();
    private Decoder decoder = basicFrameDecoder;
    private WebSocketRequestImpl webSocketRequest;
    private TimerTask wsIdleTask;

    @Override
    public final void init() throws IOException {
        webSocketRequest = new WebSocketRequestImpl(request);
        String key = request.getHeader(HeaderNameEnum.Sec_WebSocket_Key);
        String acceptSeed = key + WEBSOCKET_13_ACCEPT_GUID;
        byte[] sha1 = SHA1.encode(acceptSeed);
        String accept = Base64.getEncoder().encodeToString(sha1);
        HttpResponse response = request.newHttpRequest().getResponse();
        response.setHttpStatus(HttpStatus.SWITCHING_PROTOCOLS);
        response.setHeader(HeaderNameEnum.UPGRADE.getName(), HeaderValueEnum.Upgrade.WEBSOCKET);
        response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValueEnum.Connection.UPGRADE);
        response.setHeader(HeaderNameEnum.Sec_WebSocket_Accept.getName(), accept);
        OutputStream outputStream = response.getOutputStream();
        outputStream.flush();

        if (request.getConfiguration().getWsIdleTimeout() > 0) {
            wsIdleTask = HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
                LOGGER.debug("check wsIdle monitor");
                if (System.currentTimeMillis() - request.getLatestIo() > request.getConfiguration().getWsIdleTimeout() && webSocketRequest != null) {
                    LOGGER.debug("close ws connection by idle monitor");
                    response.close();
                }
            }, request.getConfiguration().getWsIdleTimeout(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public final void onBodyStream(ByteBuffer buffer) {
        decoder = decoder.decode(buffer, webSocketRequest);
        if (decoder != WebSocket.PAYLOAD_FINISH) {
            return;
        }
        decoder = basicFrameDecoder;
        try {
            handleWebSocketRequest(webSocketRequest, request.getAioSession());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        if (buffer.hasRemaining()) {
            onBodyStream(buffer);
        }
    }

    private void handleWebSocketRequest(WebSocketRequestImpl abstractRequest, AioSession session) throws Throwable {
        CompletableFuture<Object> future = new CompletableFuture<>();
        handle(abstractRequest, abstractRequest.getResponse(), future);
        if (future.isDone()) {
            finishResponse(abstractRequest);
        } else {
            Thread thread = Thread.currentThread();
            session.awaitRead();
            future.thenRun(() -> {
                try {
                    finishResponse(abstractRequest);
                    if (thread != Thread.currentThread()) {
                        session.writeBuffer().flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    abstractRequest.getResponse().close();
                } finally {
                    session.signalRead();
                }
            });
        }
    }

    /**
     * 执行当前处理器逻辑。
     * <p>
     * 当前handle运行完后若还有后续的处理器，需要调用doNext
     * </p>
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void handle(WebSocketRequest request, WebSocketResponse response) throws Throwable {
        try {
            switch (request.getFrameOpcode()) {
                case WebSocketUtil.OPCODE_TEXT:
                    handleTextMessage(request, response, new String(request.getPayload(), StandardCharsets.UTF_8));
                    break;
                case WebSocketUtil.OPCODE_BINARY:
                    handleBinaryMessage(request, response, request.getPayload());
                    break;
                case WebSocketUtil.OPCODE_CLOSE:
                    try {
                        onClose(request, response, new CloseReason(request.getPayload()));
                    } finally {
                        response.close();
                    }
                    break;
                case WebSocketUtil.OPCODE_PING:
                    handlePing(request, response);
                    break;
                case WebSocketUtil.OPCODE_PONG:
                    handlePong(request, response);
                    break;
                case WebSocketUtil.OPCODE_CONTINUE:
                    LOGGER.warn("unSupport OPCODE_CONTINUE now,ignore payload: {}", StringUtils.toHexString(request.getPayload()));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        } catch (Throwable throwable) {
            onError(request, throwable);
            throw throwable;
        }
    }

    public void handle(WebSocketRequest request, WebSocketResponse response, CompletableFuture<Object> completableFuture) throws Throwable {
        try {
            handle(request, response);
        } finally {
            completableFuture.complete(null);
        }
    }

    private void finishResponse(WebSocketRequestImpl abstractRequest) throws IOException {
        AbstractResponse response = abstractRequest.getResponse();
        //关闭本次请求的输出流
        if (!response.getOutputStream().isClosed()) {
            response.getOutputStream().close();
        }
        abstractRequest.reset();
    }

    public void handlePing(WebSocketRequest request, WebSocketResponse response) {
        response.pong(request.getPayload());
    }

    public void handlePong(WebSocketRequest request, WebSocketResponse response) {
        LOGGER.warn("receive pong...");
    }

    /**
     * 握手成功
     *
     * @param request
     * @param response
     */
    public void onHandShake(WebSocketRequest request, WebSocketResponse response) {
        LOGGER.warn("handShake success");
    }

    /**
     * 连接关闭
     *
     * @param request
     * @param response
     */
    public void onClose(WebSocketRequest request, WebSocketResponse response, CloseReason closeReason) {
        LOGGER.warn("close connection");
    }

    /**
     * 处理字符串请求消息
     *
     * @param request
     * @param response
     * @param data
     */
    public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String data) {
        System.out.println(data);
    }

    /**
     * 处理二进制请求消息
     *
     * @param request
     * @param response
     * @param data
     */
    public void handleBinaryMessage(WebSocketRequest request, WebSocketResponse response, byte[] data) {
        System.out.println(data);
    }

    /**
     * 连接异常
     *
     * @param request
     * @param throwable
     */
    public void onError(WebSocketRequest request, Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void destroy() {
        if (wsIdleTask != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("cancel websocket idle monitor, request:{}", this);
            }
            wsIdleTask.cancel();
            wsIdleTask = null;
        }
    }
}
