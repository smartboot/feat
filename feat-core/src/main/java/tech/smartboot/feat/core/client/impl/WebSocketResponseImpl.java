/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.impl;

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.client.AbstractResponse;
import tech.smartboot.feat.core.client.WebSocketResponse;
import tech.smartboot.feat.core.common.codec.websocket.BasicFrameDecoder;
import tech.smartboot.feat.core.common.codec.websocket.Decoder;
import tech.smartboot.feat.core.common.codec.websocket.WebSocket;
import tech.smartboot.feat.core.common.utils.SmartDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
public abstract class WebSocketResponseImpl extends AbstractResponse implements WebSocket, WebSocketResponse {
    public final static Decoder basicFrameDecoder = new BasicFrameDecoder();
    private Decoder decoder = basicFrameDecoder;
    private final ByteArrayOutputStream payload = new ByteArrayOutputStream();
    private boolean frameFinalFlag;
    private boolean frameMasked;
    private int frameRsv;
    private int frameOpcode;

    /**
     * payload长度
     */
    private long payloadLength;

    private byte[] maskingKey;
    private SmartDecoder payloadDecoder;
    private CompletableFuture<AbstractResponse> future;

    public WebSocketResponseImpl(AioSession session, CompletableFuture future) {
        super(session, future);
        this.future = future;
    }


    public void reset() {
        if (frameOpcode != WebSocket.OPCODE_CONTINUE) {
            payload.reset();
        }
        decoder = basicFrameDecoder;
    }

    public boolean isFrameFinalFlag() {
        return frameFinalFlag;
    }

    public void setFrameFinalFlag(boolean frameFinalFlag) {
        this.frameFinalFlag = frameFinalFlag;
    }

    public boolean isFrameMasked() {
        return frameMasked;
    }

    public void setFrameMasked(boolean frameMasked) {
        this.frameMasked = frameMasked;
    }

    public int getFrameRsv() {
        return frameRsv;
    }

    public void setFrameRsv(int frameRsv) {
        this.frameRsv = frameRsv;
    }

    public int getFrameOpcode() {
        return frameOpcode;
    }

    public void setFrameOpcode(int frameOpcode) {
        this.frameOpcode = frameOpcode;
    }

    public byte[] getPayload() {
        return payload.toByteArray();
    }

    public long getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(long payloadLength) {
        this.payloadLength = payloadLength;
    }

    public byte[] getMaskingKey() {
        return maskingKey;
    }

    public void setMaskingKey(byte[] maskingKey) {
        this.maskingKey = maskingKey;
    }

    public void setPayload(byte[] payload) {
        try {
            this.payload.write(payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public SmartDecoder getPayloadDecoder() {
        return payloadDecoder;
    }

    @Override
    public void setPayloadDecoder(SmartDecoder payloadDecoder) {
        this.payloadDecoder = payloadDecoder;
    }

    public CompletableFuture<AbstractResponse> getFuture() {
        return future;
    }

    public void setFuture(CompletableFuture future) {
        this.future = future;
    }
}
