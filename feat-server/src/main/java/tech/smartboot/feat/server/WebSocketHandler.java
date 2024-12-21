/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: WebSocketHandle.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server;

import tech.smartboot.feat.common.codec.websocket.BasicFrameDecoder;
import tech.smartboot.feat.common.codec.websocket.Decoder;
import tech.smartboot.feat.common.codec.websocket.WebSocket;
import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HeaderValueEnum;
import tech.smartboot.feat.common.enums.HttpStatus;
import tech.smartboot.feat.common.utils.SHA1;
import tech.smartboot.feat.server.impl.AbstractResponse;
import tech.smartboot.feat.server.impl.Request;
import tech.smartboot.feat.server.impl.WebSocketRequestImpl;
import tech.smartboot.feat.server.impl.WebSocketResponseImpl;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * WebSocket消息处理器
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public abstract class WebSocketHandler implements ServerHandler<WebSocketRequest, WebSocketResponse> {
    public static final String WEBSOCKET_13_ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private static final AttachKey<Decoder> FRAME_DECODER_KEY = AttachKey.valueOf("ws_frame_decoder");

    private final Decoder basicFrameDecoder = new BasicFrameDecoder();


    public void willHeaderComplete(WebSocketRequestImpl request, WebSocketResponseImpl response) {

    }

    @Override
    public final void onHeaderComplete(Request request) throws IOException {
        WebSocketRequestImpl webSocketRequest = request.newWebsocketRequest();
        WebSocketResponseImpl response = webSocketRequest.getResponse();
        willHeaderComplete(webSocketRequest, response);
        String key = request.getHeader(HeaderNameEnum.Sec_WebSocket_Key);
        String acceptSeed = key + WEBSOCKET_13_ACCEPT_GUID;
        byte[] sha1 = SHA1.encode(acceptSeed);
        String accept = Base64.getEncoder().encodeToString(sha1);
        response.setHttpStatus(HttpStatus.SWITCHING_PROTOCOLS);
        response.setHeader(HeaderNameEnum.UPGRADE.getName(), HeaderValueEnum.WEBSOCKET.getName());
        response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValueEnum.UPGRADE.getName());
        response.setHeader(HeaderNameEnum.Sec_WebSocket_Accept.getName(), accept);
        OutputStream outputStream = response.getOutputStream();
        outputStream.flush();

        Attachment attachment = request.getAttachment();
        if (attachment == null) {
            attachment = new Attachment();
            request.setAttachment(attachment);
        }
        attachment.put(FRAME_DECODER_KEY, basicFrameDecoder);
        request.setAttachment(attachment);
        whenHeaderComplete(webSocketRequest, response);
    }

    public void whenHeaderComplete(WebSocketRequestImpl request, WebSocketResponseImpl response) {

    }

    @Override
    public final void onBodyStream(ByteBuffer byteBuffer, Request request) {
        Attachment attachment = request.getAttachment();
        Decoder decoder = attachment.get(FRAME_DECODER_KEY).decode(byteBuffer, request.newWebsocketRequest());
        if (decoder == WebSocket.PAYLOAD_FINISH) {
            attachment.put(FRAME_DECODER_KEY, basicFrameDecoder);
            try {
                handleWebSocketRequest(request.newWebsocketRequest(), request.getAioSession());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            attachment.put(FRAME_DECODER_KEY, decoder);
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

    private void finishResponse(WebSocketRequestImpl abstractRequest) throws IOException {
        AbstractResponse response = abstractRequest.getResponse();
        //关闭本次请求的输出流
        if (!response.getOutputStream().isClosed()) {
            response.getOutputStream().close();
        }
        abstractRequest.reset();
    }
}
