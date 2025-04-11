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
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpMethod;
import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.common.utils.Constant;
import tech.smartboot.feat.core.common.utils.DateUtils;
import tech.smartboot.feat.core.server.ServerOptions;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
final class HttpOutputStream extends FeatOutputStream {
    private static final byte[] Content_Type_TEXT_Bytes = ("\r\nContent-Type:" + HeaderValue.ContentType.TEXT_PLAIN_UTF8).getBytes();
    private static final byte[] Content_Type_JSON_Bytes = ("\r\nContent-Type:" + HeaderValue.ContentType.APPLICATION_JSON).getBytes();
    private static final byte[] Content_Length_Bytes = "\r\nContent-Length:".getBytes();
    private static final byte[] CHUNKED = "\r\nTransfer-Encoding: chunked\r\n\r\n".getBytes();
    private static byte[] SERVER_LINE = null;
    private static byte[] HEAD_PART_BYTES;
    private final HttpEndpoint request;
    private final ServerOptions options;
    private final AbstractResponse response;

    public HttpOutputStream(HttpEndpoint httpRequest, AbstractResponse response) {
        super(httpRequest.getAioSession().writeBuffer());
        this.request = httpRequest;
        this.response = response;
        this.options = request.getOptions();
        if (SERVER_LINE == null) {
            String serverLine = HeaderName.SERVER.getName() + ':' + options.serverName() + "\r\n";
            SERVER_LINE = serverLine.getBytes();
            HEAD_PART_BYTES = (HttpProtocol.HTTP_11.getProtocol() + " 200 OK\r\n" + serverLine + "Date:" + DateUtils.RFC1123_FORMAT).getBytes();
            HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
                byte[] bytes = DateUtils.currentTimeFormatRFC1123();
                System.arraycopy(bytes, 0, HEAD_PART_BYTES, HEAD_PART_BYTES.length - bytes.length, bytes.length);
            }, 800, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        request.setLatestIo(DateUtils.currentTime().getTime());
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
                writeBuffer.write(Constant.CRLF_BYTES);
                headerValue = headerValue.getNextValue();
            }
        }
        writeBuffer.write(Constant.CRLF_BYTES);
    }

    private void writeHeadPart(boolean hasHeader) throws IOException {
        checkChunked();

        long contentLength = response.getContentLength();
        String contentType = response.getContentType();
        if (contentLength > 0) {
            remaining = contentLength;
        }

        HttpStatus httpStatus = response.getHttpStatus();
        boolean fastWrite =
                request.getProtocol() == HttpProtocol.HTTP_11 && httpStatus == HttpStatus.OK && options.serverName() != null && response.getHeader(HeaderName.SERVER) == null;
        // HTTP/1.1
        if (fastWrite) {
            writeBuffer.write(HEAD_PART_BYTES);
        } else {
            writeString(request.getProtocol().getProtocol());
            writeBuffer.writeByte(Constant.SP);
            httpStatus.write(writeBuffer);
            if (options.serverName() != null && response.getHeader(HeaderName.SERVER) == null) {
                writeBuffer.write(SERVER_LINE);
            }
            // Date
            writeBuffer.write(HEAD_PART_BYTES, HEAD_PART_BYTES.length - 34, 34);
        }

        if (contentType != null) {
            if (contentType.equals(HeaderValue.ContentType.TEXT_PLAIN_UTF8)) {
                writeBuffer.write(Content_Type_TEXT_Bytes);
            } else if (contentType.equals(HeaderValue.ContentType.APPLICATION_JSON)) {
                writeBuffer.write(Content_Type_JSON_Bytes);
            } else {
                writeBuffer.write(Content_Type_TEXT_Bytes, 0, 15);
                writeString(contentType);
            }
        }

        if (contentLength >= 0) {
            writeBuffer.write(Content_Length_Bytes);
            writeLongString(contentLength);
            if (hasHeader) {
                writeBuffer.write(Constant.CRLF_BYTES);
            } else {
                writeBuffer.write(Constant.CRLF_CRLF_BYTES);
            }
        } else if (chunkedSupport) {
            if (hasHeader) {
                writeBuffer.write(CHUNKED, 0, CHUNKED.length - 2);
            } else {
                writeBuffer.write(CHUNKED);
            }
        } else if (hasHeader) {
            writeBuffer.write(Constant.CRLF_BYTES);
        } else {
            writeBuffer.write(Constant.CRLF_CRLF_BYTES);
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
        } else if (response.getContentType().startsWith(HeaderValue.ContentType.EVENT_STREAM)) {
            disableChunked();
        }
    }
}
