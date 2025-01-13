/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpResponseImpl.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
class HttpResponseImpl extends AbstractResponse {
    private final HttpEndpoint request;

    public HttpResponseImpl(HttpEndpoint request) {
        this.outputStream = new HttpOutputStream(request, this);
        this.request = request;
    }

    @Override
    public void setTrailerFields(Supplier<Map<String, String>> supplier) {
        if (getOutputStream().isCommitted()) {
            throw new IllegalStateException();
        }
        if (request.getProtocol() == HttpProtocolEnum.HTTP_10) {
            throw new IllegalStateException("HTTP/1.0 request");
        } else if (request.getProtocol() == HttpProtocolEnum.HTTP_11 && !getOutputStream().isChunkedSupport()) {
            throw new IllegalStateException("unSupport trailer");
        }
        getOutputStream().setTrailerFields(supplier);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            if (getOutputStream() != null && !getOutputStream().isClosed()) {
                getOutputStream().close();
            }
        } catch (IOException ignored) {
        } finally {
            request.getAioSession().close(false);
        }
        closed = true;
    }

}
