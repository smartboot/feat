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

import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.client.stream.ServerSentEventStream;
import tech.smartboot.feat.core.common.HeaderName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * SSE客户端实现类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseClient {

    private final SseOptions options;
    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final Map<String, Consumer<SseEvent>> eventHandlers = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastEventId = new AtomicReference<>();
    private final AtomicInteger retryCount = new AtomicInteger(0);

    private volatile Consumer<Throwable> onError = Throwable::printStackTrace;
    private volatile Consumer<SseClient> onOpen = client -> {
    };
    private volatile Consumer<SseClient> onClose = client -> {
    };
    private final HttpRest httpRest;

    private static HashedWheelTimer timer;
    private final static AtomicInteger count = new AtomicInteger(0);
    private boolean connectExecuted = false;

    public SseClient(HttpRest rest) {
        this.httpRest = rest;
        this.options = new SseOptions();


        // 设置初始的lastEventId
        if (options.getLastEventId() != null) {
            this.lastEventId.set(options.getLastEventId());
        }
    }


    public void submit() {
        if (connectExecuted) {
            throw new IllegalStateException("The connect() method can only be called once per client instance.");
        }
        if (count.getAndIncrement() == 0) {
            timer = new HashedWheelTimer(r -> {
                Thread thread = new Thread(r, "SSE-Client-Scheduler");
                thread.setDaemon(true);
                return thread;
            });
        }
        connectExecuted = true;
        doConnect();
    }

    private void doConnect() {
        try {
            // 创建GET请求

            // 设置SSE相关头部
            httpRest.header(h -> {
                h.set(HeaderName.ACCEPT, "text/event-stream");
                h.set(HeaderName.CACHE_CONTROL, "no-cache");
                h.set(HeaderName.CONNECTION, "keep-alive");

                // 添加Last-Event-ID头部（如果有）
                String eventId = lastEventId.get();
                if (eventId != null) {
                    h.add("Last-Event-ID", eventId);
                }
            });

            // 设置事件流处理器
            httpRest.onResponseHeader(resp -> {
                if (resp.statusCode() == 200) {
                    httpRest.onResponseBody(new SseEventStreamImpl());
                    changeState(ConnectionState.CONNECTED);
                    retryCount.set(0);

                    // 通知连接成功
                    onOpen.accept(this);
                } else {
                    String error = "HTTP " + resp.statusCode() + ": " + resp.getReasonPhrase();
                    handleConnectionError(new RuntimeException(error));
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
        if (state.get() == ConnectionState.DISCONNECTED) {
            return;
        }
        changeState(ConnectionState.FAILED);
        // 尝试重连
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        int currentRetryCount = retryCount.incrementAndGet();
        if (!options.getRetryPolicy().shouldRetry(currentRetryCount)) {
            changeState(ConnectionState.FAILED);
            return;
        }

        long delay = options.getRetryPolicy().calculateDelay(currentRetryCount);
        changeState(ConnectionState.RECONNECTING);

        timer.schedule(() -> {
            if (state.get() == ConnectionState.RECONNECTING) {
                state.set(ConnectionState.CONNECTING);
                doConnect();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }


    public void close() {
        changeState(ConnectionState.DISCONNECTED);

        try {
            httpRest.close();
        } finally {
            if (count.decrementAndGet() == 0) {
                timer.shutdown();
                timer = null;
            }
        }
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

    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }


    public ConnectionState getConnectionState() {
        return state.get();
    }


    public String getLastEventId() {
        return lastEventId.get();
    }


    public SseOptions getOptions() {
        return options;
    }

    private void changeState(ConnectionState newState) {
        state.getAndSet(newState);
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

                // 更新最后事件ID
                if (id != null) {
                    lastEventId.set(id);
                }

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

                // 应用事件过滤器
                if (!options.getEventFilter().test(sseEvent)) {
                    return;
                }

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