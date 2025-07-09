/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.server.impl;

import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpMethod;
import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.server.ServerOptions;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
final class HttpOutputStream extends FeatOutputStream {
    private static final String TEXT_PLAIN_FAST_WRITE = HttpProtocol.HTTP_11.getProtocol() + " 200 OK\r\n" + HeaderName.SERVER.getName() + ":feat/" + Feat.VERSION + "\r\nDate:" + FeatUtils.formatRFC1123(FeatUtils.currentTime()) + "\r\nContent-Type:" + HeaderValue.ContentType.TEXT_PLAIN_UTF8 + "\r\nContent-Length:";
    private static final int SERVER_INDEX = TEXT_PLAIN_FAST_WRITE.indexOf(HeaderName.SERVER.getName());
    private static final int DATE_INDEX = TEXT_PLAIN_FAST_WRITE.indexOf("Date:");
    private static final int SERVER_INDEX_LENGTH = TEXT_PLAIN_FAST_WRITE.indexOf(HeaderName.DATE.getName()) - SERVER_INDEX;
    private static final int PLAIN_CONTENT_TYPE_INDEX = TEXT_PLAIN_FAST_WRITE.indexOf(HeaderName.CONTENT_TYPE.getName()) - 2;
    private static final int PLAIN_CONTENT_LENGTH_INDEX = TEXT_PLAIN_FAST_WRITE.indexOf(HeaderName.CONTENT_LENGTH.getName()) - 2;
    private static final byte[] TEXT_PLAIN_FAST_WRITE_BYTES = TEXT_PLAIN_FAST_WRITE.getBytes();

    private static final String APPLICATION_JSON = HttpProtocol.HTTP_11.getProtocol() + " 200 OK\r\n" + HeaderName.SERVER.getName() + ":feat/" + Feat.VERSION + "\r\nDate:" + FeatUtils.formatRFC1123(FeatUtils.currentTime()) + "\r\nContent-Type:" + HeaderValue.ContentType.APPLICATION_JSON + "\r\nContent-Length:";
    private static final int JSON_CONTENT_LENGTH_INDEX = APPLICATION_JSON.indexOf(HeaderName.CONTENT_LENGTH.getName()) - 2;
    private static final byte[] APPLICATION_JSON_FAST_WRITE_BYTES = APPLICATION_JSON.getBytes();
    private static final byte[] CHUNKED = "\r\nTransfer-Encoding: chunked\r\n\r\n".getBytes();
    private final HttpEndpoint request;
    private final HttpResponseImpl response;

    static {

        HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
            byte[] bytes = FeatUtils.formatRFC1123(FeatUtils.currentTime()).getBytes();
            System.arraycopy(bytes, 0, TEXT_PLAIN_FAST_WRITE_BYTES, DATE_INDEX + 5, bytes.length);
            System.arraycopy(bytes, 0, APPLICATION_JSON_FAST_WRITE_BYTES, DATE_INDEX + 5, bytes.length);
        }, 800, TimeUnit.MILLISECONDS);
    }

    public HttpOutputStream(HttpEndpoint httpRequest, HttpResponseImpl response) {
        super(httpRequest.getAioSession().writeBuffer());
        this.request = httpRequest;
        this.response = response;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        request.setLatestIo(FeatUtils.currentTime().getTime());
    }

    /**
     * 输出Http消息头
     */
    protected void writeHeader(HeaderWriteSource source) throws IOException {
        if (committed) {
            return;
        }

        boolean hasHeader = hasHeader();
        //输出http状态行、contentType,contentLength、Transfer-Encoding、server等信息
        writeHeadPart(hasHeader);
        if (hasHeader) {
            //输出Header部分
            writeHeaders();
        }
        committed = true;
    }

    private boolean hasHeader() {
        return response.getHeaders().size() > 0;
    }

    private void writeHeaders() throws IOException {
        for (Map.Entry<String, HeaderValue> entry : response.getHeaders().entrySet()) {
            HeaderValue headerValue = entry.getValue();
            while (headerValue != null) {
                writeString(entry.getKey());
                writeBuffer.writeByte((byte) ':');
                writeString(headerValue.getValue());
                writeBuffer.write(FeatUtils.CRLF_BYTES);
                headerValue = headerValue.getNextValue();
            }
        }
        writeBuffer.write(FeatUtils.CRLF_BYTES);
    }

    private void writeHeadPart(boolean hasHeader) throws IOException {
        checkChunked();
        long contentLength = response.getContentLength();
        String contentType = response.getContentType();
        if (contentLength > 0) {
            remaining = contentLength;
        }
        boolean fastWrite = request.getProtocol() == HttpProtocol.HTTP_11 && response.isFastFlag() && contentType != null;
        if (fastWrite && HeaderValue.ContentType.TEXT_PLAIN_UTF8.equals(contentType)) {
            writeTextPlainHeadPart(hasHeader, contentLength);
        } else if (fastWrite && HeaderValue.ContentType.APPLICATION_JSON.equals(contentType)) {
            writeApplicationJsonHeadPart(hasHeader, contentLength);
        } else {
            writeCommonHeadPart(hasHeader, contentType, contentLength);
        }
    }

    private void writeCommonHeadPart(boolean hasHeader, String contentType, long contentLength) throws IOException {
        writeString(request.getProtocol().getProtocol());
        writeBuffer.writeByte(FeatUtils.SP);
        response.getHttpStatus().write(writeBuffer);
        // Server
        if (response.getHeader(HeaderName.SERVER) == null) {
            writeBuffer.write(TEXT_PLAIN_FAST_WRITE_BYTES, SERVER_INDEX, SERVER_INDEX_LENGTH);
        }
        // Date
        writeBuffer.write(TEXT_PLAIN_FAST_WRITE_BYTES, DATE_INDEX, 34);

        if (contentType != null) {
            writeBuffer.write(TEXT_PLAIN_FAST_WRITE_BYTES, PLAIN_CONTENT_TYPE_INDEX, 15);
            writeString(contentType);
        }

        if (contentLength >= 0) {
            writeBuffer.write(TEXT_PLAIN_FAST_WRITE_BYTES, PLAIN_CONTENT_LENGTH_INDEX, 17);
            writeLongString(contentLength);
            if (hasHeader) {
                writeBuffer.write(FeatUtils.CRLF_BYTES);
            } else {
                writeBuffer.write(FeatUtils.CRLF_CRLF_BYTES);
            }
        } else if (chunkedSupport) {
            if (hasHeader) {
                writeBuffer.write(CHUNKED, 0, CHUNKED.length - 2);
            } else {
                writeBuffer.write(CHUNKED);
            }
        } else if (hasHeader) {
            writeBuffer.write(FeatUtils.CRLF_BYTES);
        } else {
            writeBuffer.write(FeatUtils.CRLF_CRLF_BYTES);
        }
    }

    private void writeApplicationJsonHeadPart(boolean hasHeader, long contentLength) throws IOException {
        if (chunkedSupport) {
            writeBuffer.write(APPLICATION_JSON_FAST_WRITE_BYTES, 0, JSON_CONTENT_LENGTH_INDEX);
            if (hasHeader) {
                writeBuffer.write(CHUNKED, 0, CHUNKED.length - 2);
            } else {
                writeBuffer.write(CHUNKED);
            }
        } else {
            if (contentLength >= 0) {
                writeBuffer.write(APPLICATION_JSON_FAST_WRITE_BYTES);
                writeLongString(contentLength);
            } else {
                writeBuffer.write(APPLICATION_JSON_FAST_WRITE_BYTES, 0, JSON_CONTENT_LENGTH_INDEX);
            }
            if (hasHeader) {
                writeBuffer.write(FeatUtils.CRLF_BYTES);
            } else {
                writeBuffer.write(FeatUtils.CRLF_CRLF_BYTES);
            }
        }
    }

    private void writeTextPlainHeadPart(boolean hasHeader, long contentLength) throws IOException {
        if (chunkedSupport) {
            writeBuffer.write(TEXT_PLAIN_FAST_WRITE_BYTES, 0, PLAIN_CONTENT_LENGTH_INDEX);
            if (hasHeader) {
                writeBuffer.write(CHUNKED, 0, CHUNKED.length - 2);
            } else {
                writeBuffer.write(CHUNKED);
            }
            return;
        }

        if (contentLength >= 0) {
            writeBuffer.write(TEXT_PLAIN_FAST_WRITE_BYTES);
            writeLongString(contentLength);
        } else {
            writeBuffer.write(TEXT_PLAIN_FAST_WRITE_BYTES, 0, PLAIN_CONTENT_LENGTH_INDEX);
        }
        if (hasHeader) {
            writeBuffer.write(FeatUtils.CRLF_BYTES);
        } else {
            writeBuffer.write(FeatUtils.CRLF_CRLF_BYTES);
        }
    }

    private void checkChunked() {
        if (!chunkedSupport) {
            return;
        }
        if (response.getContentLength() >= 0) {
            disableChunked();
        } else if (response.getHttpStatus().value() == HttpStatus.CONTINUE.value() || response.getHttpStatus().value() == HttpStatus.SWITCHING_PROTOCOLS.value()) {
            disableChunked();
        } else if (HttpMethod.HEAD.equals(request.getMethod())) {
            disableChunked();
        } else if (HttpProtocol.HTTP_11 != request.getProtocol()) {
            disableChunked();
        }
    }
}
