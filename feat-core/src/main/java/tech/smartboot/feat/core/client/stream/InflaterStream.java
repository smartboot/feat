/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.stream;

import tech.smartboot.feat.core.client.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public abstract class InflaterStream implements Stream {
    private ByteBuffer buffer;
    private InputStream inflaterInputStream;
    private final Stream stream;

    public InflaterStream(Stream stream) {
        this.stream = stream;
    }

    @Override
    public void stream(HttpResponse response, byte[] data, boolean end) throws IOException {
        if (buffer != null && buffer.remaining() > 0) {
            ByteBuffer newBuffer = ByteBuffer.allocate(buffer.remaining() + data.length);
            newBuffer.put(buffer);
            newBuffer.put(data);
            newBuffer.flip();
            buffer = newBuffer;
        } else {
            buffer = ByteBuffer.wrap(data);
        }
        if (inflaterInputStream == null) {
            inflaterInputStream = inflaterInputStream(new InputStream() {
                @Override
                public int read() {
                    return (buffer == null ? -1 : buffer.get()) & 0xFF;
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    if (buffer == null) {
                        return -1;
                    }
                    int size = Math.min(buffer.remaining(), len);
                    buffer.get(b, off, size);
                    return size;
                }

                @Override
                public int available() {
                   throw new UnsupportedOperationException();
                }
            });
        }
        byte[] b = new byte[4096];
        int n;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (buffer.hasRemaining() && (n = inflaterInputStream.read(b)) > 0) {
            bos.write(b, 0, n);
        }
        stream.stream(response, bos.toByteArray(), end);
    }

    protected abstract InputStream inflaterInputStream(InputStream inputStream) throws IOException;
}