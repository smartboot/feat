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

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.stream.ServerSentEventStream;
import tech.smartboot.feat.core.common.HeaderName;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SSE客户端实现类
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseClientImpl implements SseClient {
    
    private final String url;
    private final SseOptions options;
    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final Map<String, EventHandler> eventHandlers = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastEventId = new AtomicReference<>();
    private final AtomicInteger retryCount = new AtomicInteger(0);
    
    private volatile ErrorHandler errorHandler;
    private volatile ConnectionListener connectionListener;
    private volatile HttpClient httpClient;
    private volatile ScheduledExecutorService scheduler;
    private volatile CompletableFuture<Void> connectionFuture;

    public SseClientImpl(String url, SseOptions options) {
        this.url = url;
        this.options = options;
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "SSE-Client-Scheduler");
            thread.setDaemon(true);
            return thread;
        });
        
        // 设置初始的lastEventId
        if (options.getLastEventId() != null) {
            this.lastEventId.set(options.getLastEventId());
        }
    }

    @Override
    public CompletableFuture<Void> connect() {
        if (!state.compareAndSet(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING)) {
            if (state.get() == ConnectionState.CONNECTED) {
                return CompletableFuture.completedFuture(null);
            }
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Already connecting or connected"));
            return future;
        }
        
        try {
            return doConnect();
        } catch (Exception e) {
            state.set(ConnectionState.FAILED);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    private CompletableFuture<Void> doConnect() {
        this.connectionFuture = new CompletableFuture<>();
        
        try {
            // 创建HTTP客户端
            this.httpClient = new HttpClient(url);
            
            // 配置连接超时（使用现有的HttpOptions API）
            // httpClient.options().setTimeout(options.getConnectionTimeout());
            
            // 创建GET请求
            HttpGet request = httpClient.get();
            
            // 设置SSE相关头部
            request.header(h -> {
                h.add(HeaderName.ACCEPT, "text/event-stream");
                h.add(HeaderName.CACHE_CONTROL, "no-cache");
                
                // 添加自定义头部
                options.getHeaders().forEach(h::add);
                
                // 添加Last-Event-ID头部（如果有）
                String eventId = lastEventId.get();
                if (eventId != null) {
                    h.add("Last-Event-ID", eventId);
                }
            });
            
            // 设置事件流处理器
            request.onResponseBody(new SseEventStreamImpl());
            
            // 发送请求
            request.submit().thenAccept(response -> {
                if (response.statusCode() == 200) {
                    changeState(ConnectionState.CONNECTED);
                    retryCount.set(0);
                    
                    // 通知连接成功
                    if (connectionListener != null) {
                        connectionListener.onOpen(this);
                    }
                    
                    connectionFuture.complete(null);
                    
                    // 启动心跳检测
                    startHeartbeat();
                } else {
                    String error = "HTTP " + response.statusCode() + ": " + response.getReasonPhrase();
                    handleConnectionError(new RuntimeException(error));
                }
            }).exceptionally(throwable -> {
                handleConnectionError(throwable);
                return null;
            });
            
        } catch (Exception e) {
            handleConnectionError(e);
        }
        
        return connectionFuture;
    }
    
    private void handleConnectionError(Throwable error) {
        changeState(ConnectionState.FAILED);
        
        if (connectionListener != null) {
            connectionListener.onError(this, error);
        }
        
        if (errorHandler != null) {
            errorHandler.onError(error);
        }
        
        if (connectionFuture != null && !connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(error);
        }
        
        // 尝试重连
        scheduleReconnect();
    }
    
    private void scheduleReconnect() {
        if (!options.isAutoReconnect() || !options.getRetryPolicy().isRetryOnError()) {
            return;
        }
        
        int currentRetryCount = retryCount.incrementAndGet();
        if (!options.getRetryPolicy().shouldRetry(currentRetryCount)) {
            changeState(ConnectionState.FAILED);
            return;
        }
        
        long delay = options.getRetryPolicy().calculateDelay(currentRetryCount);
        changeState(ConnectionState.RECONNECTING);
        
        scheduler.schedule(() -> {
            if (state.get() == ConnectionState.RECONNECTING) {
                state.set(ConnectionState.DISCONNECTED);
                connect();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    private void startHeartbeat() {
        HeartbeatConfig heartbeat = options.getHeartbeatConfig();
        if (!heartbeat.isEnabled()) {
            return;
        }
        
        // 实现心跳逻辑（这里简化处理）
        scheduler.scheduleAtFixedRate(() -> {
            // 心跳检测逻辑
            // 可以通过检查最后接收事件的时间来判断连接是否正常
        }, heartbeat.getInterval(), heartbeat.getInterval(), TimeUnit.MILLISECONDS);
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        changeState(ConnectionState.DISCONNECTED);
        
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                // 忽略关闭异常
            }
        }
        
        if (connectionListener != null) {
            connectionListener.onClose(this, "Manual disconnect");
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public SseClient onEvent(String eventType, EventHandler handler) {
        eventHandlers.put(eventType != null ? eventType : "message", handler);
        return this;
    }

    @Override
    public SseClient onData(EventHandler handler) {
        return onEvent("message", handler);
    }

    @Override
    public SseClient onError(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public SseClient onConnection(ConnectionListener listener) {
        this.connectionListener = listener;
        return this;
    }

    @Override
    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }

    @Override
    public ConnectionState getConnectionState() {
        return state.get();
    }

    @Override
    public String getLastEventId() {
        return lastEventId.get();
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public SseOptions getOptions() {
        return options;
    }
    
    private void changeState(ConnectionState newState) {
        ConnectionState oldState = state.getAndSet(newState);
        if (oldState != newState && connectionListener != null) {
            connectionListener.onStateChange(this, oldState, newState);
        }
    }
    
    /**
     * SSE事件流实现
     */
    private class SseEventStreamImpl extends ServerSentEventStream {
        
        @Override
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
                if (!options.getEventFilter().accept(sseEvent)) {
                    return;
                }
                
                // 查找对应的事件处理器
                String eventType = type != null ? type : "message";
                EventHandler handler = eventHandlers.get(eventType);
                
                // 如果没有找到特定类型的处理器，尝试使用默认处理器
                if (handler == null) {
                    handler = eventHandlers.get("message");
                }
                
                // 执行事件处理器
                if (handler != null) {
                    try {
                        handler.onEvent(sseEvent);
                    } catch (Exception e) {
                        if (errorHandler != null) {
                            errorHandler.onError(e);
                        }
                    }
                }
                
            } catch (Exception e) {
                if (errorHandler != null) {
                    errorHandler.onError(e);
                }
            }
        }
    }
}