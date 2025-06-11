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
import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.io.FeatOutputStream;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
abstract class AbstractOutputStream extends FeatOutputStream {

    protected final AbstractRequest request;

    public AbstractOutputStream(AbstractRequest request, AioSession aioSession) {
        super(aioSession.writeBuffer());
        this.request = request;
    }


    /**
     * 输出Http消息头
     *
     * @throws IOException
     */
    protected final void writeHeader(HeaderWriteSource source) throws IOException {
        if (committed) {
            return;
        }

        //输出http状态行、contentType,contentLength、Transfer-Encoding、server等信息
        String headLine = request.getMethod() + " " + request.getUri() + " " + request.getProtocol() + "\r\n";
        writeBuffer.write(getBytes(headLine));
        //转换Cookie
        convertCookieToHeader(request);

        if (request.getContentType() != null) {
            writeString(HeaderName.CONTENT_TYPE.getName());
            writeBuffer.writeByte((byte) ':');
            writeBuffer.write(getBytes(String.valueOf(request.getContentType())));
            writeBuffer.write(FeatUtils.CRLF_BYTES);
        }

        if (request.getContentLength() >= 0) {
            writeString(HeaderName.CONTENT_LENGTH.getName());
            writeBuffer.writeByte((byte) ':');
            writeBuffer.write(getBytes(String.valueOf(request.getContentLength())));
            writeBuffer.write(FeatUtils.CRLF_BYTES);
        } else if (chunkedSupport) {
            request.addHeader(HeaderName.TRANSFER_ENCODING, HeaderValue.TransferEncoding.CHUNKED);
        }

        //输出Header部分
        if (request.getHeaders() != null) {
            for (Map.Entry<String, HeaderValue> entry : request.getHeaders().entrySet()) {
                HeaderValue headerValue = entry.getValue();
                while (headerValue != null) {
                    writeString(entry.getKey());
                    writeBuffer.writeByte((byte) ':');
                    writeString(headerValue.getValue());
                    writeBuffer.write(FeatUtils.CRLF_BYTES);
                    headerValue = headerValue.getNextValue();
                }
            }
        }
        writeBuffer.write(FeatUtils.CRLF_BYTES);
        committed = true;
    }


    private void convertCookieToHeader(AbstractRequest request) {
        List<Cookie> cookies = request.getCookies();
        if (cookies == null || cookies.size() == 0) {
            return;
        }
        cookies.forEach(cookie -> request.addHeader(HeaderName.SET_COOKIE, cookie.toString()));

    }
}
