/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpOutputStream.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpMethodEnum;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.utils.Constant;
import tech.smartboot.feat.core.common.utils.DateUtils;
import tech.smartboot.feat.core.server.HttpServerConfiguration;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
final class HttpOutputStream extends AbstractOutputStream {
    private static final byte[] Content_Type_Bytes = "\r\nContent-Type:".getBytes();
    private static final byte[] Content_Length_Bytes = "\r\nContent-Length:".getBytes();
    private static final Date currentDate = new Date(0);
    private static final Semaphore flushDateSemaphore = new Semaphore(1);
    private static final byte[] CHUNKED = "\r\nTransfer-Encoding: chunked\r\n".getBytes();
    private static final byte[] CHUNKED_2 = "\r\nTransfer-Encoding: chunked\r\n\r\n".getBytes();
    private static byte[] SERVER_LINE = null;
    private static long expireTime;
    private static byte[] HEAD_PART_BYTES;
    private final Request request;
    private final HttpServerConfiguration configuration;


    public HttpOutputStream(HttpRequestImpl httpRequest, HttpResponseImpl response) {
        super(httpRequest.request, response);
        this.request = httpRequest.request;
        this.configuration = request.getConfiguration();
        if (SERVER_LINE == null) {
            String serverLine =
                    HeaderNameEnum.SERVER.getName() + ':' + configuration.serverName() + "\r\n";
            SERVER_LINE = serverLine.getBytes();
            HEAD_PART_BYTES = (HttpProtocolEnum.HTTP_11.getProtocol() + " 200 OK\r\n" + serverLine
                    + "Date:" + DateUtils.RFC1123_FORMAT).getBytes();
            flushDate();
        }
    }

    private void flushDate() {
        long currentTime = DateUtils.currentTimeMillis();
        if (currentTime > expireTime && flushDateSemaphore.tryAcquire()) {
            try {
                expireTime = currentTime + 1000;
                currentDate.setTime(currentTime);
                String date = DateUtils.formatRFC1123(currentDate);
                byte[] bytes = date.getBytes();
                System.arraycopy(bytes, 0, HEAD_PART_BYTES, HEAD_PART_BYTES.length - bytes.length, bytes.length);
            } finally {
                flushDateSemaphore.release();
            }
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        if (configuration.getWsIdleTimeout() > 0 || configuration.getHttpIdleTimeout() > 0) {
            request.setLatestIo(DateUtils.currentTimeMillis());
        }
    }

    protected void writeHeadPart(boolean hasHeader) throws IOException {
        checkChunked();

        long contentLength = response.getContentLength();
        String contentType = response.getContentType();
        if (contentLength > 0) {
            remaining = contentLength;
        }

        flushDate();


        HttpStatus httpStatus = response.getHttpStatus();
        boolean fastWrite =
                request.getProtocol() == HttpProtocolEnum.HTTP_11 && httpStatus == HttpStatus.OK && configuration.serverName() != null && response.getHeader(HeaderNameEnum.SERVER.getName()) == null;
        // HTTP/1.1
        if (fastWrite) {
            writeBuffer.write(HEAD_PART_BYTES);
        } else {
            writeString(request.getProtocol().getProtocol());
            writeBuffer.writeByte((byte) ' ');
            httpStatus.write(writeBuffer);
            if (configuration.serverName() != null && response.getHeader(HeaderNameEnum.SERVER.getName()) == null) {
                writeBuffer.write(SERVER_LINE);
            }
            // Date
            writeBuffer.write(HEAD_PART_BYTES, HEAD_PART_BYTES.length - 34, 34);
        }

        if (contentType != null) {
            writeBuffer.write(Content_Type_Bytes);
            writeString(contentType);
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
                writeBuffer.write(CHUNKED);
            } else {
                writeBuffer.write(CHUNKED_2);
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
        } else if (HttpMethodEnum.HEAD.name().equals(request.getMethod())) {
            disableChunked();
        } else if (HttpProtocolEnum.HTTP_11 != request.getProtocol()) {
            disableChunked();
        } else if (response.getContentType().startsWith(HeaderValueEnum.CONTENT_TYPE_EVENT_STREAM.getName())) {
            disableChunked();
        }
    }
}
