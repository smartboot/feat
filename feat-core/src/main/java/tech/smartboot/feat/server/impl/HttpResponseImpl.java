/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpResponseImpl.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server.impl;

import tech.smartboot.feat.common.enums.HttpProtocolEnum;

import java.util.Map;
import java.util.function.Supplier;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
class HttpResponseImpl extends AbstractResponse {
    private final HttpRequestImpl request;

    public HttpResponseImpl(HttpRequestImpl request) {
        init(request.request.getAioSession(), new HttpOutputStream(request, this));
        this.request = request;
    }

    @Override
    public void setTrailerFields(Supplier<Map<String, String>> supplier) {
        if (outputStream.isCommitted()) {
            throw new IllegalStateException();
        }
        if (request.getProtocol() == HttpProtocolEnum.HTTP_10) {
            throw new IllegalStateException("HTTP/1.0 request");
        } else if (request.getProtocol() == HttpProtocolEnum.HTTP_11 && !outputStream.isChunkedSupport()) {
            throw new IllegalStateException("unSupport trailer");
        }
        outputStream.setTrailerFields(supplier);
    }
}
