/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.sse;

import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.client.stream.ServerSentEventStream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * SSE客户端实现类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseClient {

    private final Map<String, Consumer<SseEvent>> eventHandlers = new ConcurrentHashMap<>();

    private volatile Consumer<Throwable> onError = Throwable::printStackTrace;
    private volatile Consumer<SseClient> onOpen = client -> {
    };
    private volatile Consumer<SseClient> onClose = client -> {
    };

    private final HttpRest httpRest;

    private boolean connectExecuted = false;

    public SseClient(HttpRest rest) {
        this.httpRest = rest;
    }


    public void submit() {
        if (connectExecuted) {
            throw new IllegalStateException("The submit() method can only be called once per client instance.");
        }
        connectExecuted = true;
        doConnect();
    }

    private void doConnect() {
        try {
            // 设置事件流处理器
            httpRest.onResponseHeader(resp -> {
                if (resp.statusCode() == 200 && resp.getContentType().startsWith("text/event-stream")) {
                    httpRest.onResponseBody(new SseEventStreamImpl());
                    // 通知连接成功
                    onOpen.accept(this);
                }
            });

            // 发送请求
            httpRest.onFailure(this::handleConnectionError).submit();

        } catch (Exception e) {
            handleConnectionError(e);
        }
    }


    private void handleConnectionError(Throwable error) {
        onError.accept(error);
    }

    public void close() {
        httpRest.close();
        onClose.accept(this);
    }


    public SseClient onEvent(String eventType, Consumer<SseEvent> handler) {
        eventHandlers.put(eventType != null ? eventType : "message", handler);
        return this;
    }


    public SseClient onData(Consumer<SseEvent> handler) {
        return onEvent("message", handler);
    }

    /**
     * 连接打开时触发
     *
     */
    public SseClient onOpen(Consumer<SseClient> consumer) {
        this.onOpen = consumer;
        return this;
    }

    public SseClient onError(Consumer<Throwable> onError) {
        this.onError = onError;
        return this;
    }

    public SseClient onClose(Consumer<SseClient> consumer) {
        this.onClose = consumer;
        return this;
    }

    /**
     * SSE事件流实现
     */
    private class SseEventStreamImpl extends ServerSentEventStream {


        public void onEvent(HttpResponse httpResponse, Map<String, String> event) {
            try {
                // 解析事件字段
                String id = event.get("id");
                String type = event.get("event");
                String data = event.get("data");
                String retryStr = event.get("retry");

                // 解析重连间隔
                Long retry = null;
                if (retryStr != null) {
                    try {
                        retry = Long.parseLong(retryStr);
                    } catch (NumberFormatException e) {
                        // 忽略无效的重连间隔
                    }
                }

                // 创建事件对象
                SseEvent sseEvent = new SseEvent(id, type, data, retry, event);

                // 查找对应的事件处理器
                String eventType = type != null ? type : "message";
                Consumer<SseEvent> handler = eventHandlers.get(eventType);

                // 如果没有找到特定类型的处理器，尝试使用默认处理器
                if (handler == null) {
                    handler = eventHandlers.get("message");
                }

                // 执行事件处理器
                if (handler != null) {
                    try {
                        handler.accept(sseEvent);
                    } catch (Exception e) {
                        if (onError != null) {
                            onError.accept(e);
                        }
                    }
                }

            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(e);
                }
            }
        }
    }
}