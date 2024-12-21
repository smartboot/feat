package tech.smartboot.feat.core.common.codec.websocket;

import tech.smartboot.feat.core.common.utils.Constant;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/24
 */
class PayloadLengthDecoder implements Decoder {

    private final Decoder maskingKeyDecoder = new MaskingKeyDecoder();

    @Override
    public Decoder decode(ByteBuffer byteBuffer, WebSocket request) {
        long length = request.getPayloadLength();

        if (length == Constant.WS_PLAY_LOAD_126) {
            if (byteBuffer.remaining() < Short.BYTES) {
                return this;
            }
            request.setPayloadLength(Short.toUnsignedInt(byteBuffer.getShort()));
        }

        if (length == Constant.WS_PLAY_LOAD_127) {
            if (byteBuffer.remaining() < Long.BYTES) {
                return this;
            } else {
                request.setPayloadLength(byteBuffer.getLong());
            }

        }
        return maskingKeyDecoder.decode(byteBuffer, request);
    }
}
