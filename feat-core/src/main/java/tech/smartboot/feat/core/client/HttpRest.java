package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.client.stream.Stream;

import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface HttpRest {

    /**
     * 设置请求体
     *
     * @param body 请求体
     * @return HttpRest
     */
    HttpRest body(Consumer<RequestBody> body);

    RequestBody body();

    /**
     * 提交请求
     *
     * @return Future
     */
    Future<HttpResponse> submit();

    /**
     * 当响应头接收完毕时触发
     *
     * @param resp 响应头
     * @return HttpRest
     */
    HttpRest onResponseHeader(Consumer<HttpResponse> resp);

    /**
     * 流式接收响应体
     *
     * @param streaming 响应体
     * @return HttpRest
     */
    HttpRest onResponseBody(Stream streaming);

    /**
     * 当响应体接收完毕时触发
     *
     * @param consumer
     * @return
     */
    HttpRest onSuccess(Consumer<HttpResponse> consumer);

    HttpRest onFailure(Consumer<Throwable> consumer);

    HttpRest header(Consumer<Header> header);

    Header header();

    HttpRest addQueryParam(String name, String value);

    HttpRest addQueryParam(String name, int value);

}
