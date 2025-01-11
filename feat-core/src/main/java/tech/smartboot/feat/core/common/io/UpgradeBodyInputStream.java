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
                setFlags(FLAG_LISTENER_READY);
                listener.onDataAvailable();
                clearFlags(FLAG_LISTENER_READY);
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

        int readLength = Math.min(len, byteBuffer.remaining());

        byteBuffer.get(data, off, readLength);

        if (readListener == null) {

            throw new UnsupportedOperationException();
//            return readLength + read(data, off + readLength, len - readLength);
        } else {
            return readLength;
        }
    }

}
