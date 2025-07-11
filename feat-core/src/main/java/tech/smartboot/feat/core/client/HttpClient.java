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

import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.client.impl.HttpRequestImpl;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpProtocol;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public final class HttpClient {

    private final HttpOptions options;

    /**
     * Header: Host
     */
    private final String hostHeader;
    /**
     * 客户端Client
     */
    private AioQuickClient client;

    private boolean connected;

    private boolean firstConnected = true;

    private Throwable connectThrowable;
    /**
     * 消息处理器
     */
    private final HttpMessageProcessor processor = new HttpMessageProcessor();
    private final ConcurrentLinkedQueue<AbstractResponse> queue = new ConcurrentLinkedQueue<>();
    private final String uri;
    private final Semaphore semaphore = new Semaphore(1);

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


    public HttpRest rest(String method, String uri) {
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        return rest0(uri).setMethod(method);
    }

    private HttpRestImpl rest0(String uri) {
        connect();
        HttpRestImpl httpRestImpl = new HttpRestImpl(client.getSession(), queue, connectThrowable) {
            @Override
            public CompletableFuture<HttpResponse> submit() {
                if (connectThrowable != null) {
                    CompletableFuture future = getCompletableFuture();
                    future.completeExceptionally(connectThrowable);
                    connectThrowable = null;
                    return future;
                }
                return super.submit();
            }

            @Override
            public HttpRestImpl onSuccess(Consumer<HttpResponse> consumer) {
                semaphore.release();
                return super.onSuccess(consumer);
            }

            @Override
            public HttpRestImpl onFailure(Consumer<Throwable> consumer) {
                semaphore.release();
                return super.onFailure(consumer);
            }
        };
        initRest(httpRestImpl, uri);
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

    private void initRest(HttpRestImpl httpRestImpl, String uri) {
        HttpRequestImpl request = httpRestImpl.getRequest();
        if (options.getProxy() != null && FeatUtils.isNotBlank(options.getProxy().getProxyUserName())) {
            request.addHeader(HeaderName.PROXY_AUTHORIZATION,
                    "Basic " + Base64.getEncoder().encodeToString((options.getProxy().getProxyUserName() + ":" + options.getProxy().getProxyPassword()).getBytes()));
        }
        request.setUri(uri);
        request.addHeader(HeaderName.HOST, hostHeader);
        request.setProtocol(HttpProtocol.HTTP_11.getProtocol());

        httpRestImpl.getCompletableFuture().thenAccept(httpResponse -> {
            AioSession session = client.getSession();
            DecoderUnit attachment = session.getAttachment();
            //重置附件，为下一个响应作准备
            synchronized (session) {
                attachment.setState(DecoderUnit.STATE_PROTOCOL_DECODE);
                attachment.setResponse(queue.poll());
            }
            //request标注为keep-alive，response不包含该header,默认保持连接.
            if (HeaderValue.Connection.KEEPALIVE.equalsIgnoreCase(request.getHeader(HeaderName.CONNECTION)) && httpResponse.getHeader(HeaderName.CONNECTION) == null) {
                return;
            }
            //存在链路复用情况
            if (attachment.getResponse() != null || !queue.isEmpty()) {
                return;
            }

            //非keep-alive,主动断开连接
            if (!HeaderValue.Connection.KEEPALIVE.equalsIgnoreCase(httpResponse.getHeader(HeaderName.CONNECTION))) {
                close();
            } else if (!HeaderValue.Connection.KEEPALIVE.equalsIgnoreCase(request.getHeader(HeaderName.CONNECTION))) {
                close();
            }
        });
        httpRestImpl.getCompletableFuture().exceptionally(throwable -> {
            close();
            return null;
        });
    }


    public HttpOptions options() {
        return options;
    }

    private void connect() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            connectThrowable = e;
            return;
        }
        if (connected) {
            AioSession session = client.getSession();
            if (session == null || session.isInvalid()) {
                close();
                connect();
            }
            return;
        }

        try {
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

                firstConnected = false;
            }
            connected = true;
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
        } catch (Exception e) {
            connectThrowable = e;
        }
    }


    public void close() {
        connected = false;
        client.shutdown();
        if (semaphore.availablePermits() == 0) {
            semaphore.release();
        }
    }

}
