/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client;

import io.github.smartboot.socket.extension.multiplex.MultiplexClient;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.client.impl.HttpRequestImpl;
import tech.smartboot.feat.core.common.DecodeContext;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.HttpUrl;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

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
        protected void onReuse(AioQuickClient client) {
            AioSession session = client.getSession();
            ClientDecodeContext attachment = session.getAttachment();
            //重置附件，为下一个响应作准备
            attachment.setState(DecodeContext.STATE_PROTOCOL_DECODE);
            attachment.setResponse(null);
        }
    };
    /**
     * Header: Host
     */
    private final String hostHeader;


    private final String uri;

    public HttpClient(final String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (!FeatUtils.SCHEMA_HTTP.equals(httpUrl.getScheme()) && !FeatUtils.SCHEMA_HTTPS.equals(httpUrl.getScheme())) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        this.options = new HttpOptions(multiplexClient.getMultiplexOptions(), httpUrl.getHost(), httpUrl.getPort());
        options.setHttps(httpUrl.isSecure());
        hostHeader = httpUrl.getHostHeader();
        this.uri = httpUrl.getPath();
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
            AioQuickClient client = multiplexClient.acquire();
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
            String connection = httpResponse.getHeader(HeaderName.CONNECTION);
            boolean close;
            if (FeatUtils.isNotBlank(connection) && !HeaderValue.Connection.KEEPALIVE.equalsIgnoreCase(connection)) {
                close = true;
            } else {
                close = !HeaderValue.Connection.KEEPALIVE.equalsIgnoreCase(request.getHeader(HeaderName.CONNECTION));
            }
            //非keep-alive,主动断开连接
            if (close) {
                multiplexClient.release(client);
            } else {
                multiplexClient.reuse(client);
            }
        });
        httpRestImpl.getCompletableFuture().exceptionally(throwable -> {
            multiplexClient.release(client);
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
