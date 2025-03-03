/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.HttpMethod;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/3
 */
public class HttpGet extends HttpRestWrapper {

    HttpGet(HttpRestImpl rest) {
        super(rest);
        rest.setMethod(HttpMethod.GET);
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
    public HttpGet body(Consumer<RequestBody> body) {
        throw new UnsupportedOperationException("GET method does not support body");
    }

    @Override
    public RequestBody body() {
        throw new UnsupportedOperationException("GET method does not support body");
    }
}
