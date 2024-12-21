/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpOutputStream.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import tech.smartboot.feat.core.common.codec.h2.codec.ContinuationFrame;
import tech.smartboot.feat.core.common.codec.h2.codec.DataFrame;
import tech.smartboot.feat.core.common.codec.h2.codec.HeadersFrame;
import tech.smartboot.feat.core.common.codec.h2.codec.Http2Frame;
import tech.smartboot.feat.core.common.utils.HttpUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
final class Http2OutputStream extends AbstractOutputStream {
    private final int streamId;
    private final Http2Session http2Session;

    public Http2OutputStream(int streamId, Http2RequestImpl httpRequest, Http2ResponseImpl response, boolean push) {
        super(httpRequest, response);
        disableChunked();
        this.http2Session = httpRequest.getSession();
        this.streamId = streamId;
    }

    protected void writeHeader(HeaderWriteSource source) throws IOException {
        if (committed) {
            if (source == HeaderWriteSource.CLOSE && !closed) {
//                writeBuffer.flush();
                DataFrame dataFrame1 = new DataFrame(streamId, DataFrame.FLAG_END_STREAM, 0);
                dataFrame1.writeTo(writeBuffer, new byte[0], 0, 0);

//                writeBuffer.flush();
            }
            return;
        }
        //转换Cookie
        convertCookieToHeader();
        // Create HEADERS frame
        response.setHeader(":status", String.valueOf(response.getHttpStatus().value()));

        List<ByteBuffer> buffers = HttpUtils.HPackEncoder(http2Session.getHpackEncoder(), response.getHeaders());

        boolean multipleHeaders = buffers.size() > 1;

        HeadersFrame headersFrame = new HeadersFrame(streamId, multipleHeaders ? 0 : Http2Frame.FLAG_END_HEADERS, 0);
        headersFrame.setFragment(buffers.isEmpty() ? null : buffers.get(0));
        headersFrame.writeTo(writeBuffer);
        for (int i = 1; i < buffers.size() - 1; i++) {
            ContinuationFrame continuationFrame = new ContinuationFrame(streamId, 0, 0);
            continuationFrame.setFragment(buffers.get(i));
            continuationFrame.writeTo(writeBuffer);
        }
        if (multipleHeaders) {
            ContinuationFrame continuationFrame = new ContinuationFrame(streamId, Http2Frame.FLAG_END_HEADERS, 0);
            continuationFrame.setFragment(buffers.get(buffers.size() - 1));
            continuationFrame.writeTo(writeBuffer);
        }
//        writeBuffer.flush();
        System.err.println("StreamID: " + streamId + " Header已发送...");
        committed = true;
    }

    protected void writeHeadPart(boolean hasHeader) {
        //编码成http2
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeHeader(HeaderWriteSource.WRITE);
        if (len == 0) {
            return;
        }
        DataFrame dataFrame = new DataFrame(streamId, 0, len);
        dataFrame.writeTo(writeBuffer, b, off, len);
    }

}
