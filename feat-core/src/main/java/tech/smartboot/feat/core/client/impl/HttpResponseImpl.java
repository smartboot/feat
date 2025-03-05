/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.impl;

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.client.AbstractResponse;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.stream.GzipStream;
import tech.smartboot.feat.core.client.stream.Stream;
import tech.smartboot.feat.core.client.stream.StringStream;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.utils.Constant;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class HttpResponseImpl extends AbstractResponse implements HttpResponse {
    private static final int STATE_CHUNK_LENGTH = 1;
    private static final int STATE_CHUNK_CONTENT = 1 << 1;
    private static final int STATE_CHUNK_END = 1 << 2;
    private static final int STATE_CONTENT_LENGTH = 1 << 3;
    private static final int STATE_FINISH = 1 << 4;
    private static final int STATE_GZIP = 0x80;
    private Stream streaming = new StringStream();
    private int state;

    private long remaining;
    /**
     * body内容
     */
    private String body;
    private Consumer<HttpResponse> headerConsumer;

    public HttpResponseImpl(AioSession session, CompletableFuture future) {
        super(session, future);
    }

    public String body() {
        return body;
    }

    public void headerCompleted(Consumer<HttpResponse> resp) {
        this.headerConsumer = resp;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public void reset() {

    }

    @Override
    public void onHeaderComplete() {
        String transferEncoding = getHeader(HeaderNameEnum.TRANSFER_ENCODING.getName());
        if (StringUtils.equals(transferEncoding, HeaderValue.TransferEncoding.CHUNKED)) {
            state = STATE_CHUNK_LENGTH;
        } else if (getContentLength() > 0) {
            state = STATE_CONTENT_LENGTH;
            remaining = getContentLength();
        } else {
            state = STATE_FINISH;
        }
        if (StringUtils.equals(HeaderValue.ContentEncoding.GZIP, getHeader(HeaderNameEnum.CONTENT_ENCODING.getName()))) {
            state = STATE_GZIP | state;
            streaming = new GzipStream(streaming);
        }
        if (headerConsumer != null) {
            headerConsumer.accept(this);
        }
    }

    @Override
    public void onBodyStream(ByteBuffer buffer) {
        try {
            switch (state & ~STATE_GZIP) {
                case STATE_CHUNK_LENGTH: {
                    int length = StringUtils.scanUntilAndTrim(buffer, Constant.LF);
                    if (length < 0) {
                        return;
                    }
                    if (length == 1) {
                        state = STATE_FINISH;
                    } else {
                        remaining = StringUtils.convertHexString(buffer, buffer.position() - length - 1, length - 1);
                        if (remaining != 0) {
                            state = STATE_CHUNK_CONTENT;
                        }
                    }
                    onBodyStream(buffer);
                }
                break;
                case STATE_CHUNK_CONTENT: {
                    int length = Integer.min(buffer.remaining(), (int) remaining);
                    if (length == 0) {
                        return;
                    }
                    byte[] bytes = new byte[length];
                    buffer.get(bytes);
                    remaining -= length;
                    streaming.stream(this, bytes, false);
                    if (remaining == 0) {
                        state = STATE_CHUNK_END;
                    }
                    onBodyStream(buffer);
                }
                break;
                case STATE_CHUNK_END: {
                    if (buffer.remaining() < 2) {
                        return;
                    }
                    if (buffer.get() == Constant.CR && buffer.get() == Constant.LF) {
                        state = STATE_CHUNK_LENGTH;
                        onBodyStream(buffer);
                    } else {
                        throw new IllegalStateException();
                    }
                }
                break;
                case STATE_CONTENT_LENGTH: {
                    int length = Integer.min(buffer.remaining(), (int) remaining);
                    if (length == 0) {
                        return;
                    }
                    byte[] bytes = new byte[length];
                    buffer.get(bytes);
                    remaining -= length;
                    streaming.stream(this, bytes, false);
                    if (remaining == 0) {
                        state = STATE_FINISH;
                    }
                    onBodyStream(buffer);
                }
                break;
                case STATE_FINISH:
                    finishDecode();
                    break;
                default:
                    throw new IllegalStateException();
            }
        } catch (Exception e) {
            throw new FeatException(e);
        }
    }

    private void finishDecode() throws IOException {
        streaming.stream(this, new byte[0], true);
        getFuture().complete(this);
    }


    public void onStream(Stream streaming) {
        this.streaming = streaming;
    }

}
