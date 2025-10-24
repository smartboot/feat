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

import org.smartboot.socket.extension.multiplex.MultiplexClient;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.client.impl.HttpRequestImpl;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpProtocol;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public final class HttpClient {

    private final HttpOptions options;
    //消息处理器
    private final HttpMessageProcessor processor = new HttpMessageProcessor();
    private final MultiplexClient<AbstractResponse> multiplexClient = new MultiplexClient(processor, processor) {
        @Override
        protected void onReuseClient(AioQuickClient client) {
            AioSession session = client.getSession();
            DecoderUnit attachment = session.getAttachment();
            //重置附件，为下一个响应作准备
            attachment.setState(DecoderUnit.STATE_PROTOCOL_DECODE);
            attachment.setResponse(null);
        }
    };
    /**
     * Header: Host
     */
    private final String hostHeader;


    private final String uri;

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
        this.options = new HttpOptions(multiplexClient.getMultiplexOptions(), host, port);
        options.setHttps(https);
        hostHeader = options.getHost() + ":" + options.getPort();
        this.uri = uriIndex > 0 ? url.substring(uriIndex) : "/";
    }

    public HttpClient(String host, int port) {
        this.options = new HttpOptions(multiplexClient.getMultiplexOptions(), host, port);
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
        HttpRestImpl httpRestImpl;
        try {
            AioQuickClient client = multiplexClient.acquireClient();
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
                multiplexClient.releaseClient(client);
            } else {
                multiplexClient.reuseClient(client);
            }
        });
        httpRestImpl.getCompletableFuture().exceptionally(throwable -> {
            multiplexClient.releaseClient(client);
            return null;
        });
    }


    public HttpOptions options() {
        return options;
    }

    public void close() {
        multiplexClient.close();
    }
}
