package tech.smartboot.feat.common.codec.h2.codec;

import java.nio.ByteBuffer;

public class WindowUpdateFrame extends Http2Frame {

    private int windowUpdate;

    public WindowUpdateFrame(int streamId, int flags, int remaining) {
        super(streamId, flags, remaining);
    }

    @Override
    public boolean decode(ByteBuffer buffer) {
        if (buffer.remaining() < 4) {
            return false;
        }
        windowUpdate = buffer.getInt();
        remaining = 0;
        return true;
    }

    @Override
    public int type() {
        return FRAME_TYPE_WINDOW_UPDATE;
    }

    public int getUpdate() {
        return this.windowUpdate;
    }

}
