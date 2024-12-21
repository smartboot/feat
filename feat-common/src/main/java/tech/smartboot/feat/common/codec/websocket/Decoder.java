package tech.smartboot.feat.common.codec.websocket;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/24
 */
public interface Decoder {

    Decoder decode(ByteBuffer byteBuffer, WebSocket webSocket);
}
