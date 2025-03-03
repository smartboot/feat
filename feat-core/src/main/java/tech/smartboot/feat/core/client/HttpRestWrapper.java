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

import tech.smartboot.feat.core.client.stream.Stream;

import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
class HttpRestWrapper implements HttpRest {
    protected final HttpRestImpl rest;

    public HttpRestWrapper(HttpRestImpl rest) {
        this.rest = rest;
    }

    @Override
    public HttpRest body(Consumer<RequestBody> body) {
        return rest.body(body);
    }

    @Override
    public RequestBody body() {
        return rest.body();
    }

    @Override
    public Future<HttpResponse> submit() {
        return rest.submit();
    }

    @Override
    public HttpRest onResponseHeader(Consumer<HttpResponse> resp) {
        return rest.onResponseHeader(resp);
    }

    @Override
    public HttpRest onResponseBody(Stream streaming) {
        return rest.onResponseBody(streaming);
    }

    @Override
    public HttpRest onSuccess(Consumer<HttpResponse> consumer) {
        return rest.onSuccess(consumer);
    }

    @Override
    public HttpRest onFailure(Consumer<Throwable> consumer) {
        return rest.onFailure(consumer);
    }

    @Override
    public HttpRest header(Consumer<Header> header) {
        return rest.header(header);
    }

    @Override
    public Header header() {
        return rest.header();
    }

    @Override
    public HttpRest addQueryParam(String name, String value) {
        return rest.addQueryParam(name, value);
    }

    @Override
    public HttpRest addQueryParam(String name, int value) {
        return rest.addQueryParam(name, value);
    }

}
