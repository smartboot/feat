/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: WebSocketRequestImpl.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import org.smartboot.socket.util.Attachment;
import tech.smartboot.feat.core.common.Reset;
import tech.smartboot.feat.core.common.codec.websocket.WebSocket;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.common.multipart.MultipartConfig;
import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.common.utils.SmartDecoder;
import tech.smartboot.feat.core.common.utils.WebSocketUtil;
import tech.smartboot.feat.core.server.PushBuilder;
import tech.smartboot.feat.core.server.WebSocketRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
public class WebSocketRequestImpl extends AbstractRequest implements WebSocketRequest, WebSocket, Reset {
    private SmartDecoder payloadDecoder;
    private final ByteArrayOutputStream payload = new ByteArrayOutputStream();
    private final WebSocketResponseImpl response;
    private boolean frameFinalFlag;
    private boolean frameMasked;
    private int frameRsv;
    private int frameOpcode;
    /**
     * payload长度
     */
    private long payloadLength;

    private byte[] maskingKey;

    public WebSocketRequestImpl(Request baseHttpRequest) {
        super(baseHttpRequest);
        this.response = new WebSocketResponseImpl(this);
    }

    public final WebSocketResponseImpl getResponse() {
        return response;
    }

    public BodyInputStream getInputStream() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void reset() {
        if (frameOpcode != WebSocketUtil.OPCODE_CONTINUE) {
            payload.reset();
        }
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

    @Override
    public Collection<Part> getParts() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Part> getParts(MultipartConfig configElement) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getTrailerFields() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTrailerFieldsReady() {
        throw new UnsupportedOperationException();
    }


    @Override
    public Attachment getAttachment() {
        return request.getAttachment();
    }

    @Override
    public void setAttachment(Attachment attachment) {
        request.setAttachment(attachment);
    }

    @Override
    public PushBuilder newPushBuilder() {
        throw new UnsupportedOperationException();
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

    @Override
    public SmartDecoder getPayloadDecoder() {
        return payloadDecoder;
    }

    @Override
    public void setPayloadDecoder(SmartDecoder payloadDecoder) {
        this.payloadDecoder = payloadDecoder;
    }
}
