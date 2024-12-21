package tech.smartboot.feat.core.common.codec.h2.codec;

import java.nio.ByteBuffer;

public class ResetStreamFrame extends Http2Frame {


    private int errorCode;

    public ResetStreamFrame(int streamid, int flag, int remaining) {
        super(streamid, flag, remaining);
    }

    @Override
    public boolean decode(ByteBuffer buffer) {
        if (finishDecode()) {
            return true;
        }
        if (buffer.remaining() < 4) {
            return false;
        }
        errorCode = buffer.getInt();
        remaining -= 4;
        checkEndRemaining();
        return true;
    }

    @Override
    public int type() {
        return FRAME_TYPE_RST_STREAM;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
