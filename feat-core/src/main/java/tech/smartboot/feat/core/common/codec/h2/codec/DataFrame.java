package tech.smartboot.feat.core.common.codec.h2.codec;


import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DataFrame extends Http2Frame {

    private static final int STATE_PAD_LENGTH = 0;
    private static final int STATE_DATA = 1;
    private static final int STATE_PADDING = 2;
    private int state = STATE_PAD_LENGTH;


    private int padLength = 0;
    private ByteBuffer dataBuffer = ByteBuffer.allocate(0);
    private byte[] padding = EMPTY_PADDING;


    public DataFrame(int streamId, int flags, int remaining) {
        super(streamId, flags, remaining);
    }

    @Override
    public boolean decode(ByteBuffer buffer) {
        if (finishDecode()) {
            return true;
        }
        switch (state) {
            case STATE_PAD_LENGTH:
                if (getFlag(FLAG_PADDED)) {
                    if (!buffer.hasRemaining()) {
                        return false;
                    }
                    padLength = buffer.get();
                    if (padLength < 0) {
                        throw new IllegalStateException();
                    }
                    remaining -= 1;
                }
                dataBuffer = ByteBuffer.allocate(remaining - padLength);
                state = STATE_DATA;
            case STATE_DATA:
                int min = Math.min(buffer.remaining(), dataBuffer.remaining());
                int limit = buffer.limit();
                buffer.limit(buffer.position() + min);
                dataBuffer.put(buffer);
                buffer.limit(limit);
                remaining -= min;
                if (dataBuffer.hasRemaining()) {
                    return false;
                }
                state = STATE_PADDING;
            case STATE_PADDING:
                if (buffer.remaining() < padLength) {
                    return false;
                }
                if (padLength > 0) {
                    padding = new byte[padLength];
                    buffer.get(padding);
                    remaining -= padLength;
                }
        }

        checkEndRemaining();
        dataBuffer.flip();
        return true;
    }

    public void writeTo(WriteBuffer writeBuffer, byte[] data, int offset, int length) throws IOException {
        System.err.println("write data frame");
        int payloadLength = length;
        byte flags = (byte) this.flags;

        // Check if padding is needed
        boolean padded = padding != null && padding.length > 0;
        if (padded) {
            payloadLength += 1 + padding.length;
            flags |= FLAG_PADDED;
        }

        // Write frame header
        writeBuffer.writeInt(payloadLength << 8 | FRAME_TYPE_DATA);
        writeBuffer.writeByte(flags);
        writeBuffer.writeInt(streamId);

        // Write pad length if padded
        if (padded) {
            writeBuffer.writeByte((byte) padding.length);
        }

        // Write data
        writeBuffer.write(data, offset, length);

        // Write padding if padded
        if (padded) {
            writeBuffer.write(padding);
        }
//        writeBuffer.flush();
    }

    public byte[] getPadding() {
        return padding;
    }

    public byte[] getData() {
        return dataBuffer.array();
    }

    @Override
    public int type() {
        return FRAME_TYPE_DATA;
    }

}
