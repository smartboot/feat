/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.common.codec.websocket;

import tech.smartboot.feat.core.common.utils.SmartDecoder;

import java.nio.ByteBuffer;

public interface WebSocket {
    public static final Decoder PAYLOAD_FINISH = new Decoder() {
        @Override
        public Decoder decode(ByteBuffer byteBuffer, WebSocket request) {
            return this;
        }
    };

    public boolean isFrameFinalFlag();

    public void setFrameFinalFlag(boolean frameFinalFlag);

    public boolean isFrameMasked();

    public void setFrameMasked(boolean frameMasked);

    public int getFrameRsv();

    public void setFrameRsv(int frameRsv);

    public int getFrameOpcode();

    public void setFrameOpcode(int frameOpcode);

    public byte[] getPayload();

    public long getPayloadLength();

    public void setPayloadLength(long payloadLength);

    public byte[] getMaskingKey();

    public void setMaskingKey(byte[] maskingKey);

    public void setPayload(byte[] payload);

    SmartDecoder getPayloadDecoder();

    void setPayloadDecoder(SmartDecoder decoder);
}
