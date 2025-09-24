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

import org.smartboot.socket.extension.plugins.IdleStatePlugin;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.client.impl.HttpRequestImpl;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public final class HttpClient {

    private final HttpOptions options;

    /**
     * Header: Host
     */
    private final String hostHeader;

    private boolean closed;

    private boolean firstConnected = true;


    /**
     * 可链路复用的连接
     */
    private final ConcurrentLinkedQueue<AioQuickClient> resuingClients = new ConcurrentLinkedQueue<>();
    /**
     * 所有连接
     */
    private final ConcurrentHashMap<AioQuickClient, AioQuickClient> clients = new ConcurrentHashMap<>();
    /**
     * 消息处理器
     */
    private final HttpMessageProcessor processor = new HttpMessageProcessor();
    private final String uri;
    private long latestTime = System.currentTimeMillis();
    private volatile TimerTask timerTask;

    public HttpClient(String url) {
        int schemaIndex = url.indexOf("://");
        if (schemaIndex == -1) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        String schema = url.substring(0, schemaIndex);
        int uriIndex = url.indexOf("/", schemaIndex + 3);
        int portIndex = url.indexOf(":", schemaIndex + 3);
        boolean http = FeatUtils.SCHEMA_HTTP.equals(schema);
        boolean https = !http && FeatUtils.SCHEMA_HTTPS.equals(schema);

        if (!http && !https) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        String host;
        int port;
        if (portIndex > 0) {
            host = url.substring(schemaIndex + 3, portIndex);
            port = FeatUtils.toInt(uriIndex > 0 ? url.substring(portIndex + 1, uriIndex) : url.substring(portIndex + 1), -1);
        } else if (uriIndex > 0) {
            host = url.substring(schemaIndex + 3, uriIndex);
            port = https ? 443 : 80;
        } else {
            host = url.substring(schemaIndex + 3);
            port = https ? 443 : 80;
        }
        if (port == -1) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        this.options = new HttpOptions(host, port);
        options.setHttps(https);
        hostHeader = options.getHost() + ":" + options.getPort();
        this.uri = uriIndex > 0 ? url.substring(uriIndex) : "/";
    }

    public HttpClient(String host, int port) {
        this.options = new HttpOptions(host, port);
        hostHeader = options.getHost() + ":" + options.getPort();
        this.uri = null;
    }

    public HttpGet get() {
        if (uri == null) {
            throw new UnsupportedOperationException("this method only support on constructor: HttpClient(String url)");
        }
        HttpRestImpl rest = rest0(uri);
        return new HttpGet(rest);
    }

    public HttpGet get(String uri) {
        HttpRestImpl rest = rest0(uri);
        return new HttpGet(rest);
    }

    public HttpRest rest(String method) {
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        if (uri == null) {
            throw new UnsupportedOperationException("this method only support on constructor: HttpClient(String url)");
        }
        return rest0(uri).setMethod(method);
    }

    public HttpRest rest(String method, String uri) {
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        return rest0(uri).setMethod(method);
    }

    private HttpRestImpl rest0(String uri) {
        if (closed) {
            throw new FeatException("client closed");
        }
        latestTime = System.currentTimeMillis();
        HttpRestImpl httpRestImpl;
        try {
            AioQuickClient client = acquireConnection();
            httpRestImpl = new HttpRestImpl(client.getSession());
            initRest(httpRestImpl, uri, client);
        } catch (Throwable e) {
            httpRestImpl = new HttpRestImpl(null) {
                @Override
                public CompletableFuture<HttpResponse> submit() {
                    CompletableFuture future = super.getCompletableFuture();
                    future.completeExceptionally(e);
                    return future;
                }

                @Override
                public RequestBody body() {
                    return new RequestBody() {
                        @Override
                        public RequestBody write(byte[] bytes, int offset, int len) {
                            return this;
                        }

                        @Override
                        public void transferFrom(ByteBuffer buffer, Consumer<RequestBody> consumer) {

                        }

                        @Override
                        public RequestBody flush() {
                            return this;
                        }
                    };
                }
            };
        }

        return httpRestImpl;
    }

    public HttpPost post(String uri) {
        HttpRestImpl rest = rest0(uri);
        return new HttpPost(rest);
    }

    public HttpPost post() {
        if (uri == null) {
            throw new UnsupportedOperationException("this method only support on constructor: HttpClient(String url)");
        }
        return post(uri);
    }

    int i = 0;

    private void initRest(HttpRestImpl httpRestImpl, String uri, AioQuickClient client) {
        HttpRequestImpl request = httpRestImpl.getRequest();
        if (options.getProxy() != null && FeatUtils.isNotBlank(options.getProxy().getProxyUserName())) {
            request.addHeader(HeaderName.PROXY_AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((options.getProxy().getProxyUserName() + ":" + options.getProxy().getProxyPassword()).getBytes()));
        }
        request.setUri(uri);
        request.addHeader(HeaderName.HOST, hostHeader);
        request.setProtocol(HttpProtocol.HTTP_11.getProtocol());
        options.getHeaders().forEach(request::addHeader);

        httpRestImpl.getCompletableFuture().thenAccept(httpResponse -> {
            boolean close = !HeaderValue.Connection.KEEPALIVE.equalsIgnoreCase(httpResponse.getHeader(HeaderName.CONNECTION));
            if (!close) {
                close = !HeaderValue.Connection.KEEPALIVE.equalsIgnoreCase(request.getHeader(HeaderName.CONNECTION));
            }
            //非keep-alive,主动断开连接
            if (close) {
                releaseConnection(client);
            } else {
                resuingClients.offer(client);
            }
        });
        httpRestImpl.getCompletableFuture().exceptionally(throwable -> {
            releaseConnection(client);
            return null;
        });
    }


    public HttpOptions options() {
        return options;
    }

    private AioQuickClient acquireConnection() throws Throwable {
        AioQuickClient client;
        while (true) {
            client = resuingClients.poll();
            if (client == null) {
                break;
            }
            AioSession session = client.getSession();
            if (session.isInvalid()) {
                releaseConnection(client);
                continue;
            }
            DecoderUnit attachment = session.getAttachment();
            //重置附件，为下一个响应作准备
            attachment.setState(DecoderUnit.STATE_PROTOCOL_DECODE);
            attachment.setResponse(null);
            return client;
        }

        if (firstConnected) {
            boolean noneSslPlugin = true;
            for (Plugin responsePlugin : options.getPlugins()) {
                processor.addPlugin(responsePlugin);
                if (responsePlugin instanceof SslPlugin) {
                    noneSslPlugin = false;
                }
            }
            if (noneSslPlugin && options.isHttps()) {
                processor.addPlugin(new SslPlugin<>(new ClientSSLContextFactory()));
            }
            if (options.isDebug()) {
                processor.addPlugin(new StreamMonitorPlugin<>(StreamMonitorPlugin.BLUE_TEXT_INPUT_STREAM, StreamMonitorPlugin.RED_TEXT_OUTPUT_STREAM));
            }
            if (options.idleTimeout() > 0) {
                processor.addPlugin(new IdleStatePlugin<>(options.idleTimeout()));
            }

            firstConnected = false;
        }
        client = options.getProxy() == null ? new AioQuickClient(options.getHost(), options.getPort(), processor, processor) : new AioQuickClient(options.getProxy().getProxyHost(), options.getProxy().getProxyPort(), processor, processor);
        client.setWriteBuffer(options.getWriteBufferSize(), 2).setReadBufferSize(options.readBufferSize());
        if (options.getConnectTimeout() > 0) {
            client.connectTimeout(options.getConnectTimeout());
        }
        if (options.group() == null) {
            client.start();
        } else {
            client.start(options.group());
        }
        clients.put(client, client);
        startConnectionMonitor();
        return client;
    }

    /**
     * 启动连接监控任务，用于清理无效连接和空闲连接
     *
     * <p>该方法会启动一个定时任务，每隔1分钟执行一次检查：
     * <ul>
     *   <li>如果连接超过30秒没有被使用，则将其关闭</li>
     *   <li>如果所有连接都已关闭，则取消监控任务</li>
     *   <li>如果任务取消后又有新连接创建，则重新启动监控任务</li>
     * </ul>
     *
     * @see #releaseConnection(AioQuickClient)
     * @see #acquireConnection()
     */
    private void startConnectionMonitor() {
        // 使用双重检查锁定确保只有一个监控任务在运行
        if (timerTask != null) {
            return;
        }
        synchronized (this) {
            if (timerTask != null) {
                return;
            }
            timerTask = HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
                long time = latestTime;
                // 如果超过30秒没有使用连接，则清理可复用连接队列中的连接
                if (System.currentTimeMillis() - time > 30 * 1000) {
                    AioQuickClient c;
                    // 当latestTime没有更新且队列中还有连接时，持续清理
                    while (time == latestTime && (c = resuingClients.poll()) != null) {
                        System.out.println("release...");
                        releaseConnection(c);
                    }
                }
                // 如果没有活动连接，则取消监控任务
                if (clients.isEmpty()) {
                    TimerTask oldTask = timerTask;
                    timerTask = null;
                    oldTask.cancel();
                    // 取消任务后再次检查是否有新连接加入，如果有则重新启动监控任务
                    if (!clients.isEmpty()) {
                        startConnectionMonitor();
                    }
                }
            }, 1, TimeUnit.MINUTES);
        }
    }

    private void releaseConnection(AioQuickClient client) {
        client.shutdownNow();
        clients.remove(client);
    }

    public void close() {
        closed = true;
        clients.forEach((client, aioQuickClient) -> releaseConnection(client));
    }

}
