package tech.smartboot.feat.core.client;

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

    HttpRest body(Consumer<Body> body);

    Body body();

    Future<HttpResponse> submit();

    HttpRest onSuccess(Consumer<HttpResponse> consumer);

    HttpRest onFailure(Consumer<Throwable> consumer);

    HttpRest header(Consumer<Header> header);

    Header header();

    HttpRest addQueryParam(String name, String value);

    HttpResponse response();

}
