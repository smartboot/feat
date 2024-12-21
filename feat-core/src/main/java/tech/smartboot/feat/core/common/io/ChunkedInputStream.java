package tech.smartboot.feat.core.common.io;

import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.utils.Constant;
import org.smartboot.socket.transport.AioSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/6
 */
public class ChunkedInputStream extends BodyInputStream {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(8);

    private Map<String, String> trailerFields;
    /**
     * 剩余可读字节数
     */
    private long remainingThreshold;
    private final Consumer<Map<String, String>> consumer;
    private String trailerName;
    private int chunkedRemaining;

    public ChunkedInputStream(AioSession session, long maxPayload, Consumer<Map<String, String>> consumer) {
        super(session);
        this.remainingThreshold = maxPayload;
        this.consumer = consumer;
        setFlags(FLAG_READ_CHUNKED_LENGTH);
    }


    @Override
    public int read(byte[] data, int off, int len) throws IOException {
        checkState();
        if (data == null) {
            throw new NullPointerException();
        }
        if (len == 0) {
            return 0;
        }
        readChunkedLength();
        //仅在readListener情况下会存在true
        if (anyAreSet(state, FLAG_READ_CHUNKED_LENGTH)) {
            return 0;
        }
        if (isFinished()) {
            return -1;
        }


        ByteBuffer byteBuffer = session.readBuffer();
        if (chunkedRemaining > 0 && !byteBuffer.hasRemaining() && readListener == null) {
            session.read();
        }
        int readLength = Math.min(len, byteBuffer.remaining());
        readLength = Math.min(readLength, chunkedRemaining);
        byteBuffer.get(data, off, readLength);
        chunkedRemaining = chunkedRemaining - readLength;

        if (chunkedRemaining > 0) {
            return readLength + read(data, off + readLength, len - readLength);
        }
        setFlags(FLAG_EXPECT_CR_LF | FLAG_READ_CHUNKED_LENGTH);
        if (len == readLength) {
            return readLength;
        }
        int size = read(data, off + readLength, len - readLength);
        if (size <= 0) {
            return readLength;
        } else {
            return readLength + size;
        }
    }

    private void readChunkedLength() throws IOException {
        if (!anyAreSet(state, FLAG_READ_CHUNKED_LENGTH)) {
            return;
        }
        ByteBuffer byteBuffer = session.readBuffer();
        //前一个chunked解析完成，需要处理 CRLF
        if (anyAreSet(state, FLAG_EXPECT_CR_LF)) {
            if (byteBuffer.remaining() >= 2) {
                if (byteBuffer.get() == Constant.CR && byteBuffer.get() == Constant.LF) {
                    clearFlags(FLAG_EXPECT_CR_LF);
                } else {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
            } else {
                return;
            }
        }
        byteBuffer.mark();
        while (byteBuffer.hasRemaining()) {
            byte b = byteBuffer.get();
            if (b != Constant.LF) {
                continue;
            }
            if (byteBuffer.get(byteBuffer.position() - 2) != Constant.CR) {
                throw new HttpException(HttpStatus.BAD_REQUEST);
            }
            int p = byteBuffer.position() - 2;
            byteBuffer.reset();
            byte[] buffer = new byte[p - byteBuffer.position()];
            byteBuffer.get(buffer);
            chunkedRemaining = Integer.parseInt(new String(buffer), 16);
            remainingThreshold = remainingThreshold - 2 - buffer.length - chunkedRemaining;
            if (remainingThreshold < 0) {
                throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
            }
            if (byteBuffer.get() != Constant.CR || byteBuffer.get() != Constant.LF) {
                throw new HttpException(HttpStatus.BAD_REQUEST);
            }
            clearFlags(FLAG_READ_CHUNKED_LENGTH);
            if (chunkedRemaining > 0) {
                return;
            }
            setFlags(FLAG_CHUNKED_TRAILER);
            //trailerFields
            if (readListener == null) {
                while (true) {
                    parseTrailerFields();
                    if (anyAreSet(state, FLAG_CHUNKED_TRAILER)) {
                        session.read();
                    } else {
                        break;
                    }
                }
            } else {
                parseTrailerFields();
            }
            return;
        }

        byteBuffer.reset();
        if (readListener == null) {
            int i = session.read();
            if (i == -1) {
                throw new IOException("inputStream is closed");
            } else {
                readChunkedLength();
            }
        }
    }


    private void parseTrailerFields() throws IOException {
        ByteBuffer byteBuffer = session.readBuffer();
        byteBuffer.mark();
        while (byteBuffer.hasRemaining()) {
            byte b = byteBuffer.get();
            if (b == Constant.LF) {
                byteBuffer.mark();
                if (buffer.size() == 0) {
                    consumer.accept(trailerFields);
                    clearFlags(FLAG_CHUNKED_TRAILER);
                    setFlags(FLAG_FINISHED);
                    return;
                }
                trailerFields.put(trailerName, buffer.toString());
                buffer.reset();
            } else if (b == ':') {
                trailerName = buffer.toString();
                byteBuffer.mark();
                buffer.reset();
            } else if (b != Constant.CR) {
                if (trailerFields == null) {
                    trailerFields = new HashMap<>();
                }
                buffer.write(b);
            }
        }
        byteBuffer.reset();
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
                if (anyAreSet(state, FLAG_CHUNKED_TRAILER)) {
                    parseTrailerFields();
                }
                if (anyAreSet(state, FLAG_FINISHED)) {
                    return;
                }
                readChunkedLength();
                //不足chunkedLength解码
                if (anyAreSet(state, FLAG_READ_CHUNKED_LENGTH)) {
                    return;
                }
                if (chunkedRemaining > 0 && session.readBuffer().hasRemaining()) {
                    setFlags(FLAG_READY);
                    listener.onDataAvailable();
                    clearFlags(FLAG_READY);
                }
                if (anyAreSet(state, FLAG_FINISHED)) {
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
