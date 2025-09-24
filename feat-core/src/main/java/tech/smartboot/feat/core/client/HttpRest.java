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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
     * <p>若响应非SSE，会当做普通Http处理</p>
     * <p>注意：该方法和onResponseHeader会相互覆盖，后注册的会覆盖先注册的。
     * 同时如果服务器响应为有效的SSE格式，系统将自动切换到SSE处理模式，
     * 此时onResponseBody注册的Stream处理器将不会被触发。</p>
     *
     * @param consumer SseClient消费者，用于配置SSE事件处理器
     * @return HttpRest
     */
    default HttpRest onSSE(Consumer<SseClient> consumer) {
        return onSSE(resp -> resp.statusCode() == 200 && resp.getContentType().startsWith("text/event-stream"), consumer);
    }

    /**
     * 转换为SseClient以处理Server-Sent Events
     * <p>只有当响应满足predicate条件时才会切换到SSE处理模式。
     * 若响应不满足条件，会当做普通Http处理</p>
     * <p>注意：该方法和onResponseHeader会相互覆盖，后注册的会覆盖先注册的。
     * 同时如果服务器响应为有效的SSE格式，系统将自动切换到SSE处理模式，
     * 此时onResponseBody注册的Stream处理器将不会被触发。</p>
     *
     * @param predicate 判断是否为SSE响应的条件，只有满足条件时才会启用SSE处理模式
     * @param consumer  SseClient消费者，用于配置各类SSE事件处理器
     * @return HttpRest
     */
    HttpRest onSSE(Predicate<HttpResponse> predicate, Consumer<SseClient> consumer);


    /**
     * 当响应头接收完毕时触发
     * <p>注意：该方法和onSSE会相互覆盖，后注册的会覆盖先注册的</p>
     *
     * @param resp 响应头
     * @return HttpRest
     */
    HttpRest onResponseHeader(Consumer<HttpResponse> resp);

    /**
     * 流式接收响应体
     * <p>注意：如果同时注册了onSSE相关监听器，且服务器响应为有效的SSE格式(text/event-stream)，
     * 系统将自动切换到SSE处理模式，此时onResponseBody注册的Stream处理器将不会被触发。</p>
     *
     * @param streaming 响应体流处理器
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

    HttpRest onClose(Runnable runnable);
}
