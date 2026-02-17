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

import tech.smartboot.feat.core.client.stream.ServerSentEventStream;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * SSE客户端实现类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public final class SseClient {

    private final Map<String, Consumer<SseEvent>> eventHandlers = new ConcurrentHashMap<>();

    private volatile Consumer<HttpResponse> onOpen = client -> {
    };

    private final HttpRestImpl httpRest;

    SseClient(Predicate<HttpResponse> predicate, HttpRestImpl rest) {
        this.httpRest = rest;
        // 设置事件流处理器
        httpRest.onResponseHeader(resp -> {
            if (predicate.test(resp)) {
                httpRest.sseUpgrade();
                httpRest.onResponseBody(new ServerSentEventStream() {
                    public void onEvent(HttpResponse httpResponse, Map<String, String> event) throws IOException {
                        // 创建事件对象
                        SseEvent sseEvent = new SseEvent(event);

                        // 查找对应的事件处理器
                        String eventType = sseEvent.getType() != null ? sseEvent.getType() : ServerSentEventStream.DEFAULT_EVENT;
                        Consumer<SseEvent> handler = eventHandlers.get(eventType);

                        // 如果没有找到特定类型的处理器，尝试使用默认处理器
                        if (handler == null) {
                            handler = eventHandlers.get(ServerSentEventStream.DEFAULT_EVENT);
                        }

                        // 执行事件处理器
                        if (handler != null) {
                            handler.accept(sseEvent);
                        }
                    }
                });
                // 通知连接成功
                onOpen.accept(resp);
            }
        });
    }


    public SseClient onEvent(String eventType, Consumer<SseEvent> handler) {
        eventHandlers.put(eventType != null ? eventType : ServerSentEventStream.DEFAULT_EVENT, handler);
        return this;
    }


    public SseClient onData(Consumer<SseEvent> handler) {
        return onEvent(ServerSentEventStream.DEFAULT_EVENT, handler);
    }

    /**
     * 连接打开时触发
     *
     */
    public SseClient onOpen(Consumer<HttpResponse> consumer) {
        this.onOpen = consumer;
        return this;
    }
}