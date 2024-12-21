package tech.smartboot.feat.core.common.io;

import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class BodyInputStream extends InputStream {
    public static final BodyInputStream EMPTY_INPUT_STREAM = new BodyInputStream(null) {
        @Override
        public void setReadListener(ReadListener listener) {
            throw new IllegalStateException();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return -1;
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public void close() {

        }

        @Override
        public boolean isFinished() {
            return true;
        }
    };
    protected final AioSession session;
    protected ReadListener readListener;
    protected volatile int state;
    protected static final int FLAG_READY = 1;
    protected static final int FLAG_FINISHED = 1 << 1;
    protected static final int FLAG_CLOSED = 1 << 2;
    protected static final int FLAG_IS_READY_CALLED = 1 << 3;
    protected static final int FLAG_CHUNKED_TRAILER = 1 << 4;
    protected static final int FLAG_EXPECT_CR_LF = 1 << 5;
    //需要解析chunked长度
    protected static final int FLAG_READ_CHUNKED_LENGTH = 1 << 6;
    protected static final AtomicIntegerFieldUpdater<BodyInputStream> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(BodyInputStream.class, "state");

    public BodyInputStream(AioSession session) {
        this.session = session;
    }


    @Override
    public void close() throws IOException {
        setFlags(FLAG_CLOSED | FLAG_FINISHED);
    }

    @Override
    public final int read() throws IOException {
        byte[] b = new byte[1];
        int read = read(b);
        if (read == -1) {
            return -1;
        }
        return b[0] & 0xff;
    }

    /**
     * listener#onAllDataRead方法需要触发futuren.complete
     *
     * @param listener
     */
    public abstract void setReadListener(ReadListener listener);

    public ReadListener getReadListener() {
        return readListener;
    }

    public boolean isFinished() {
        return anyAreSet(state, FLAG_FINISHED);
    }

    protected void checkState() throws IOException {
        if (anyAreSet(state, FLAG_CLOSED)) {
            throw new IOException("stream closed");
        }
    }

    public boolean isReady() {
        if (readListener == null) {
            return true;
        }
        return anyAreSet(state, FLAG_READY) && session.readBuffer().hasRemaining();
    }

    @Override
    public int available() throws IOException {
        checkState();
        return session.readBuffer().remaining();
    }

    protected static boolean anyAreClear(int var, int flags) {
        return (var & flags) != flags;
    }

    protected void clearFlags(int flags) {
        int old;
        do {
            old = state;
        } while (!stateUpdater.compareAndSet(this, old, old & ~flags));
    }

    protected void setFlags(int flags) {
        int old;
        do {
            old = state;
        } while (!stateUpdater.compareAndSet(this, old, old | flags));
    }

    protected boolean anyAreSet(int var, int flags) {
        return (var & flags) != 0;
    }
}
