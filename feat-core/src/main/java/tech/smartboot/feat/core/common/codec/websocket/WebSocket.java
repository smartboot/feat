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

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public interface WebSocket {
    Decoder PAYLOAD_FINISH = new Decoder() {
        @Override
        public Decoder decode(ByteBuffer byteBuffer, WebSocket request) {
            return this;
        }
    };

    boolean isFrameFinalFlag();

    void setFrameFinalFlag(boolean frameFinalFlag);

    boolean isFrameMasked();

    void setFrameMasked(boolean frameMasked);

    int getFrameRsv();

    void setFrameRsv(int frameRsv);

    int getFrameOpcode();

    void setFrameOpcode(int frameOpcode);

    byte[] getPayload();

    long getPayloadLength();

    void setPayloadLength(long payloadLength);

    byte[] getMaskingKey();

    void setMaskingKey(byte[] maskingKey);

    void setPayload(byte[] payload);

    SmartDecoder getPayloadDecoder();

    void setPayloadDecoder(SmartDecoder decoder);
}
