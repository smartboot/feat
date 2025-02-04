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

    Body<? extends HttpRest> body();

    Future<HttpResponse> done();

    HttpRest onSuccess(Consumer<HttpResponse> consumer);

    HttpRest onFailure(Consumer<Throwable> consumer);

    Header<? extends HttpRest> header();

    HttpRest addQueryParam(String name, String value);

    HttpResponse response();

}
