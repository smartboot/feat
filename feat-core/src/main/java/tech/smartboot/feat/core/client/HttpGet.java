/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpGet.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.enums.HttpMethodEnum;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/3
 */
public class HttpGet extends HttpRestWrapper {

    HttpGet(HttpRestImpl rest) {
        super(rest);
        rest.setMethod(HttpMethodEnum.GET.getMethod());
    }


    @Override
    public HttpGet onSuccess(Consumer<HttpResponse> consumer) {
        super.onSuccess(consumer);
        return this;
    }

    @Override
    public HttpGet onFailure(Consumer<Throwable> consumer) {
        super.onFailure(consumer);
        return this;
    }

    @Override
    public HttpGet header(Consumer<Header> header) {
        super.header(header);
        return this;
    }

    @Override
    public HttpRest body(Consumer<RequestBody> body) {
        throw new UnsupportedOperationException("GET method does not support body");
    }

    @Override
    public RequestBody body() {
        throw new UnsupportedOperationException("GET method does not support body");
    }
}
