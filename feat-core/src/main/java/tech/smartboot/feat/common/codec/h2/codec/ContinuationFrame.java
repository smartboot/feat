package tech.smartboot.feat.common.codec.h2.codec;

import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ContinuationFrame extends Http2Frame {
    private ByteBuffer fragment = EMPTY_BUFFER;

    public ContinuationFrame(int streamId, int flags, int remaining) {
        super(streamId, flags, remaining);
    }


    @Override
    public boolean decode(ByteBuffer buffer) {
        return false;
    }

    @Override
    public void writeTo(WriteBuffer writeBuffer) throws IOException {
        int payloadLength = 0;
        byte flags = (byte) this.flags;

        payloadLength += fragment.remaining();

        // Write frame header
        writeBuffer.writeInt(payloadLength << 8 | FRAME_TYPE_CONTINUATION);
        writeBuffer.writeByte(flags);
        System.out.println("write continuation ,streamId:" + streamId);
        writeBuffer.writeInt(streamId);

        // Write fragment

        writeBuffer.write(fragment.array(), 0, fragment.remaining());

    }

    public ByteBuffer getFragment() {
        return fragment;
    }

    public void setFragment(ByteBuffer fragment) {
        this.fragment = fragment;
    }

    @Override
    public int type() {
        return FRAME_TYPE_CONTINUATION;
    }

}
