/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: PostInputStream.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.common.io;

import tech.smartboot.feat.common.enums.HttpStatus;
import tech.smartboot.feat.common.exception.HttpException;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2019/12/1
 */
public class PostInputStream extends BodyInputStream {
    private final long maxPayload;
    private long remaining;

    public PostInputStream(AioSession session, long contentLength, long maxPayload) {
        super(session);
        this.remaining = contentLength;
        this.maxPayload = maxPayload;
    }

    @Override
    public int read(byte[] data, int off, int len) throws IOException {
        if (maxPayload > 0L && remaining > maxPayload) {
            throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
        }

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

        if (readListener != null) {
            if (anyAreClear(state, FLAG_READY | FLAG_IS_READY_CALLED)) {
                throw new IllegalStateException();
            }
            clearFlags(FLAG_IS_READY_CALLED);
        } else if (remaining > 0 && !byteBuffer.hasRemaining()) {
            try {
                session.read();
            } catch (IOException e) {
                if (readListener != null) {
                    readListener.onError(e);
                }
                throw e;
            }

        }
        int readLength = Math.min(len, byteBuffer.remaining());
        if (remaining < readLength) {
            readLength = (int) remaining;
        }
        byteBuffer.get(data, off, readLength);
        remaining = remaining - readLength;

        if (remaining > 0) {
            if (readListener == null) {
                return readLength + read(data, off + readLength, len - readLength);
            } else {
                if (!byteBuffer.hasRemaining()) {
                    clearFlags(FLAG_READY);
                }
                return readLength;
            }
        } else {
            setFlags(FLAG_FINISHED);
            return readLength;
        }
    }

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
                if (remaining == 0) {
                    listener.onAllDataRead();
                }
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
}
