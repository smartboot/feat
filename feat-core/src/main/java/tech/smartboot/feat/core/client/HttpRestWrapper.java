package tech.smartboot.feat.core.client;

import java.util.concurrent.Future;
import java.util.function.Consumer;

class HttpRestWrapper implements HttpRest {
    protected final HttpRestImpl rest;

    public HttpRestWrapper(HttpRestImpl rest) {
        this.rest = rest;
    }

    @Override
    public HttpRest setMethod(String method) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpRest body(Consumer<Body> body) {
        return rest.body(body);
    }

    @Override
    public Body body() {
        return rest.body();
    }

    @Override
    public Future<HttpResponse> submit() {
        return rest.submit();
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
    public HttpResponse response() {
        return rest.response();
    }
}
