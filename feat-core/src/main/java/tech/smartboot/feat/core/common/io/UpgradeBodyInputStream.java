package tech.smartboot.feat.core.common.io;

import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.ByteBuffer;

public class UpgradeBodyInputStream extends BodyInputStream {
    public UpgradeBodyInputStream(AioSession session) {
        super(session);
    }

    @Override
    public void setReadListener(ReadListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (this.readListener != null) {
            throw new IllegalStateException();
        }
        this.readListener = new ReadListener() {
            @Override
            public void onDataAvailable() throws IOException {
                setFlags(FLAG_READY);
                listener.onDataAvailable();
                clearFlags(FLAG_READY);
            }

            @Override
            public void onAllDataRead() throws IOException {
                listener.onAllDataRead();
            }

            @Override
            public void onError(Throwable t) {
                listener.onError(t);
            }
        };
    }

    @Override
    public int read(byte[] data, int off, int len) throws IOException {
        checkState();
        if (data == null) {
            throw new NullPointerException();
        }
        if (isFinished()) {
            return -1;
        }
        if (len == 0) {
            return 0;
        }

        ByteBuffer byteBuffer = session.readBuffer();

//        if (readListener != null) {
//            if (anyAreClear(state, FLAG_READY)) {
//                throw new IllegalStateException();
//            }
//            clearFlags(FLAG_IS_READY_CALLED);
//        }
        int readLength = Math.min(len, byteBuffer.remaining());

        byteBuffer.get(data, off, readLength);

        if (readListener == null) {
            return readLength + read(data, off + readLength, len - readLength);
        } else {
            if (!byteBuffer.hasRemaining()) {
                clearFlags(FLAG_READY);
            }
            return readLength;
        }
    }

}
