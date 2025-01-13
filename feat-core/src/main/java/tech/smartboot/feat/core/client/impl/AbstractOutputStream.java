/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: AbstractOutputStream.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client.impl;

import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.common.utils.Constant;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
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
            writeString(HeaderNameEnum.CONTENT_TYPE.getName());
            writeBuffer.writeByte((byte) ':');
            writeBuffer.write(getBytes(String.valueOf(request.getContentType())));
            writeBuffer.write(Constant.CRLF_BYTES);
        }

        if (request.getContentLength() >= 0) {
            writeString(HeaderNameEnum.CONTENT_LENGTH.getName());
            writeBuffer.writeByte((byte) ':');
            writeBuffer.write(getBytes(String.valueOf(request.getContentLength())));
            writeBuffer.write(Constant.CRLF_BYTES);
        } else if (chunkedSupport && source == HeaderWriteSource.WRITE) {
            request.addHeader(HeaderNameEnum.TRANSFER_ENCODING.getName(), HeaderValueEnum.TransferEncoding.CHUNKED);
        }

        //输出Header部分
        if (request.getHeaders() != null) {
            for (Map.Entry<String, HeaderValue> entry : request.getHeaders().entrySet()) {
                HeaderValue headerValue = entry.getValue();
                while (headerValue != null) {
                    writeString(entry.getKey());
                    writeBuffer.writeByte((byte) ':');
                    writeString(headerValue.getValue());
                    writeBuffer.write(Constant.CRLF_BYTES);
                    headerValue = headerValue.getNextValue();
                }
            }
        }
        writeBuffer.write(Constant.CRLF_BYTES);
        committed = true;
    }


    private void convertCookieToHeader(AbstractRequest request) {
        List<Cookie> cookies = request.getCookies();
        if (cookies == null || cookies.size() == 0) {
            return;
        }
        cookies.forEach(cookie -> request.addHeader(HeaderNameEnum.SET_COOKIE.getName(), cookie.toString()));

    }
}
