package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.client.stream.Stream;

import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface HttpRest {
    /**
     * 设置请求方法
     *
     * @param method 请求方法
     * @return
     */
    HttpRest setMethod(String method);

    HttpRest body(Consumer<RequestBody> body);

    RequestBody body();

    Future<HttpResponse> submit();

    HttpRest onResponseHeader(Consumer<HttpResponse> resp);

    HttpRest onResponseBody(Stream streaming);

    HttpRest onSuccess(Consumer<HttpResponse> consumer);

    HttpRest onFailure(Consumer<Throwable> consumer);

    HttpRest header(Consumer<Header> header);

    Header header();

    HttpRest addQueryParam(String name, String value);

}
