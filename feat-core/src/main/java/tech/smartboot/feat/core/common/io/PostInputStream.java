/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.common.io;

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.HttpException;

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
            if (anyAreClear(state, FLAG_LISTENER_READY)) {
                throw new IllegalStateException();
            }
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

        if (remaining == 0) {
            setFlags(FLAG_FINISHED);
            return readLength;
        }
        if (readListener == null) {
            return readLength + read(data, off + readLength, len - readLength);
        } else {
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
                setFlags(FLAG_LISTENER_READY);
                listener.onDataAvailable();
                clearFlags(FLAG_LISTENER_READY);
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
