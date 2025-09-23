/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import tech.smartboot.feat.core.common.DecodeState;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.core.server.impl.Upgrade;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version v1.0 9/23/25
 */
public class AsyncBodyReadUpgrade extends Upgrade {
    private final HttpEndpoint request;
    private final ByteBuffer buffer;

    public AsyncBodyReadUpgrade(HttpEndpoint request, int length) {
        this.request = request;
        buffer = ByteBuffer.allocate(length);
    }

    @Override
    public void init(HttpRequest request, HttpResponse response) throws IOException {
        throw new IllegalStateException("AsyncBodyReadUpgrade not support");
    }

    @Override
    public void onBodyStream(ByteBuffer readBuffer) {
        if (readBuffer.remaining() <= buffer.remaining()) {
            buffer.put(readBuffer);
        } else {
            int limit = readBuffer.limit();
            readBuffer.limit(readBuffer.position() + buffer.remaining());
            buffer.put(readBuffer);
            readBuffer.limit(limit);
        }
        if (buffer.hasRemaining()) {
            return;
        }
        request.setInputStream(new AsyncBodyInputStream(request));
        request.getDecodeState().setState(DecodeState.STATE_BODY_ASYNC_READING_DONE);
        request.setUpgrade(null);
        buffer.flip();
    }

    private class AsyncBodyInputStream extends BodyInputStream {
        public AsyncBodyInputStream(HttpEndpoint session) {
            super(session);
        }


        @Override
        public int available() {
            return buffer.remaining();
        }

        @Override
        public boolean isFinished() {
            return true;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (len > buffer.remaining()) {
                len = buffer.remaining();
            }
            if (len == 0) {
                return -1;
            }
            buffer.get(b, off, len);
            return len;
        }
    }
}
