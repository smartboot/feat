package tech.smartboot.feat.common.codec.h2.codec;

import java.nio.ByteBuffer;

public class PriorityFrame extends Http2Frame {

    private int streamDependency;
    private int weight;
    private boolean exclusive;

    public PriorityFrame(int streamId, int flags, int remaining, int streamDependency, int weight, boolean exclusive) {
        super(streamId, flags, remaining);
        this.streamDependency = streamDependency;
        this.weight = weight;
        this.exclusive = exclusive;
    }

    @Override
    public boolean decode(ByteBuffer buffer) {
        if (finishDecode()) {
            return true;
        }
        if (buffer.remaining() < 5) {
            return false;
        }
        int value = buffer.getInt();
        streamDependency = value & 0x7FFFFFFF;
        exclusive = (value & 0x80000000) != 0;
        weight = buffer.get() & 0xFF;
        remaining -= 5;
        checkEndRemaining();
        return true;
    }

    @Override
    public int type() {
        return FRAME_TYPE_PRIORITY;
    }


    public int streamDependency() {
        return streamDependency;
    }

    public int weight() {
        return weight;
    }

    public boolean exclusive() {
        return exclusive;
    }

}
