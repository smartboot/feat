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
