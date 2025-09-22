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

import tech.smartboot.feat.core.client.sse.SseClient;
import tech.smartboot.feat.core.client.stream.Stream;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
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
    CompletableFuture<HttpResponse> submit();


    /**
     * 转换为SseClient以处理Server-Sent Events
     * 若响应非SSE，会当做普通Http处理
     *
     * @return SseClient
     */
    SseClient toSseClient();


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

    /**
     * 关闭HttpRest
     */
    void close();
}
