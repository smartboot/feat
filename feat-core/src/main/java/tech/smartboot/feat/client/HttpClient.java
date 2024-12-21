/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpClient.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.client;

import tech.smartboot.feat.client.impl.HttpRequestImpl;
import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HeaderValueEnum;
import tech.smartboot.feat.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.common.utils.Constant;
import tech.smartboot.feat.common.utils.NumberUtils;
import tech.smartboot.feat.common.utils.StringUtils;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.Base64;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/2
 */
public final class HttpClient implements AutoCloseable {

    private final HttpClientConfiguration configuration;

    /**
     * Header: Host
     */
    private final String hostHeader;
    /**
     * 客户端Client
     */
    private AioQuickClient client;

    /**
     * 绑定线程池资源组
     */
    private AsynchronousChannelGroup asynchronousChannelGroup;

    private boolean connected;

    private boolean firstConnected = true;

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
        boolean http = Constant.SCHEMA_HTTP.equals(schema);
        boolean https = !http && Constant.SCHEMA_HTTPS.equals(schema);

        if (!http && !https) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        String host;
        int port;
        if (portIndex > 0) {
            host = url.substring(schemaIndex + 3, portIndex);
            port = NumberUtils.toInt(uriIndex > 0 ? url.substring(portIndex + 1, uriIndex) : url.substring(portIndex + 1), -1);
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
        this.configuration = new HttpClientConfiguration(host, port);
        configuration.setHttps(https);
        hostHeader = configuration.getHost() + ":" + configuration.getPort();
        this.uri = uriIndex > 0 ? url.substring(uriIndex) : "/";
    }

    public HttpClient(String host, int port) {
        this.configuration = new HttpClientConfiguration(host, port);
        hostHeader = configuration.getHost() + ":" + configuration.getPort();
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


    public HttpRest rest(String uri) {
        return rest0(uri);
    }

    private HttpRestImpl rest0(String uri) {
        connect();
        HttpRestImpl httpRestImpl = new HttpRestImpl(client.getSession(), queue) {
            @Override
            public Future<HttpResponse> done() {
                try {
                    return super.done();
                } finally {
                    if (HeaderValueEnum.KEEPALIVE.getName().equals(getRequest().getHeader(HeaderNameEnum.CONNECTION.getName()))) {
                        semaphore.release();
                    }
                }
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
        if (configuration.getProxy() != null && StringUtils.isNotBlank(configuration.getProxy().getProxyUserName())) {
            request.addHeader(HeaderNameEnum.PROXY_AUTHORIZATION.getName(), "Basic " + Base64.getEncoder().encodeToString((configuration.getProxy().getProxyUserName() + ":" + configuration.getProxy().getProxyPassword()).getBytes()));
        }
        request.setUri(uri);
        request.addHeader(HeaderNameEnum.HOST.getName(), hostHeader);
        request.setProtocol(HttpProtocolEnum.HTTP_11.getProtocol());

        httpRestImpl.getCompletableFuture().thenAccept(httpResponse -> {
            AioSession session = client.getSession();
            DecoderUnit attachment = session.getAttachment();
            //重置附件，为下一个响应作准备
            synchronized (session) {
                attachment.setState(DecoderUnit.STATE_PROTOCOL_DECODE);
                attachment.setResponse(queue.poll());
            }
            //request标注为keep-alive，response不包含该header,默认保持连接.
            if (HeaderValueEnum.KEEPALIVE.getName().equalsIgnoreCase(request.getHeader(HeaderNameEnum.CONNECTION.getName())) && httpResponse.getHeader(HeaderNameEnum.CONNECTION.getName()) == null) {
                return;
            }
            //存在链路复用情况
            if (attachment.getResponse() != null || !queue.isEmpty()) {
                return;
            }
            //非keep-alive,主动断开连接
            if (!HeaderValueEnum.KEEPALIVE.getName().equalsIgnoreCase(httpResponse.getHeader(HeaderNameEnum.CONNECTION.getName()))) {
                close();
            } else if (!HeaderValueEnum.KEEPALIVE.getName().equalsIgnoreCase(request.getHeader(HeaderNameEnum.CONNECTION.getName()))) {
                close();
            }
        });
        httpRestImpl.getCompletableFuture().exceptionally(throwable -> {
            close();
            return null;
        });
    }


    public HttpClientConfiguration configuration() {
        return configuration;
    }

    private void connect() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
                for (Plugin responsePlugin : configuration.getPlugins()) {
                    processor.addPlugin(responsePlugin);
                    if (responsePlugin instanceof SslPlugin) {
                        noneSslPlugin = false;
                    }
                }
                if (noneSslPlugin && configuration.isHttps()) {
                    processor.addPlugin(new SslPlugin<>(new ClientSSLContextFactory()));
                }
                if (configuration.isDebug()) {
                    processor.addPlugin(new StreamMonitorPlugin<>(StreamMonitorPlugin.BLUE_TEXT_INPUT_STREAM, StreamMonitorPlugin.RED_TEXT_OUTPUT_STREAM));
                }

                firstConnected = false;
            }
            connected = true;
            client = configuration.getProxy() == null ? new AioQuickClient(configuration.getHost(), configuration.getPort(), processor, processor) : new AioQuickClient(configuration.getProxy().getProxyHost(), configuration.getProxy().getProxyPort(), processor, processor);
            client.setBufferPagePool(configuration.getReadBufferPool(), configuration.getWriteBufferPool()).setWriteBuffer(configuration.getWriteBufferSize(), 2).setReadBufferSize(configuration.readBufferSize());
            if (configuration.getConnectTimeout() > 0) {
                client.connectTimeout(configuration.getConnectTimeout());
            }
            if (asynchronousChannelGroup == null) {
                client.start();
            } else {
                client.start(asynchronousChannelGroup);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void setAsynchronousChannelGroup(AsynchronousChannelGroup asynchronousChannelGroup) {
        this.asynchronousChannelGroup = asynchronousChannelGroup;
    }

    public void close() {
        connected = false;
        client.shutdownNow();
        if (semaphore.availablePermits() == 0) {
            semaphore.release();
        }
    }

}
