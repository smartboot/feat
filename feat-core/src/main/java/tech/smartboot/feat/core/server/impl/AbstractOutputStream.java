/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: AbstractOutputStream.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.io.BufferOutputStream;
import tech.smartboot.feat.core.common.utils.Constant;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
abstract class AbstractOutputStream extends BufferOutputStream {
    protected final AbstractResponse response;
    protected final CommonRequest request;

    public AbstractOutputStream(CommonRequest request, AbstractResponse response) {
        super(request.getAioSession().writeBuffer());
        this.response = response;
        this.request = request;
    }

    /**
     * 输出Http消息头
     */
    protected void writeHeader(HeaderWriteSource source) throws IOException {
        if (committed) {
            return;
        }

        //转换Cookie
        convertCookieToHeader();

        boolean hasHeader = hasHeader();
        //输出http状态行、contentType,contentLength、Transfer-Encoding、server等信息
        writeHeadPart(hasHeader);
        if (hasHeader) {
            //输出Header部分
            writeHeaders();
        }
        committed = true;
    }

    protected abstract void writeHeadPart(boolean hasHeader) throws IOException;

    protected void convertCookieToHeader() {
        List<Cookie> cookies = response.getCookies();
        if (cookies.size() > 0) {
            cookies.forEach(cookie -> response.addHeader(HeaderNameEnum.SET_COOKIE.getName(), cookie.toString()));
        }
    }

    protected boolean hasHeader() {
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
}
