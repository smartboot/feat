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

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.client.impl.HttpRequestImpl;
import tech.smartboot.feat.core.client.impl.HttpResponseImpl;
import tech.smartboot.feat.core.client.sse.SseClient;
import tech.smartboot.feat.core.client.stream.Stream;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
class HttpRestImpl implements HttpRest {
    private static final Logger logger = LoggerFactory.getLogger(HttpRestImpl.class);
    private final static String DEFAULT_USER_AGENT = "feat";
    private final HttpRequestImpl request;
    private final CompletableFuture<HttpResponseImpl> completableFuture = new CompletableFuture<>();
    private Map<String, String> queryParams = null;
    private boolean commit = false;
    private RequestBody body;

    private final HttpResponseImpl response;

    HttpRestImpl(AioSession session) {
        this.request = new HttpRequestImpl(session);
        this.response = new HttpResponseImpl(session, completableFuture);
        if (session != null) {
            DecoderUnit attachment = session.getAttachment();
            if (attachment.getResponse() != null) {
                throw new FeatException("HttpRestImpl can not be reused");
            }
            attachment.setResponse(response);
        }
    }

    protected final void willSendRequest() {
        if (commit) {
            return;
        }
        commit = true;
        resetUri();
        Collection<String> headers = request.getHeaderNames();
        if (!headers.contains(HeaderName.USER_AGENT.getName())) {
            request.addHeader(HeaderName.USER_AGENT, DEFAULT_USER_AGENT);
        }
    }

    private void resetUri() {
        if (queryParams == null) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder(request.getUri());
        int index = request.getUri().indexOf("#");
        if (index > 0) {
            stringBuilder.setLength(index);
        }
        index = request.getUri().indexOf("?");
        if (index == -1) {
            stringBuilder.append('?');
        } else if (index < stringBuilder.length() - 1) {
            stringBuilder.append('&');
        }
        queryParams.forEach((key, value) -> {
            try {
                stringBuilder.append(key).append('=').append(URLEncoder.encode(value, "utf8")).append('&');
            } catch (UnsupportedEncodingException e) {
                stringBuilder.append(key).append('=').append(value).append('&');
            }
        });
        if (stringBuilder.length() > 0) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        request.setUri(stringBuilder.toString());
    }

    public RequestBody body() {
        if (body == null) {
            body = new RequestBody() {

                @Override
                public RequestBody write(byte[] bytes, int offset, int len) {
                    try {
                        willSendRequest();
                        request.getOutputStream().write(bytes, offset, len);
                    } catch (Throwable e) {
                        logger.error("body stream write error! ", e);
                        completableFuture.completeExceptionally(e);
                    }
                    return this;
                }

                @Override
                public void transferFrom(ByteBuffer buffer, Consumer<RequestBody> consumer) {
                    try {
                        willSendRequest();
                        request.getOutputStream().transferFrom(buffer, bufferOutputStream -> consumer.accept(HttpRestImpl.this.body));
                    } catch (Throwable e) {
                        logger.error("body stream write error! ", e);
                        completableFuture.completeExceptionally(e);
                    }
                }

                @Override
                public RequestBody flush() {
                    try {
                        willSendRequest();
                        request.getOutputStream().flush();
                    } catch (Throwable e) {
                        System.out.println("body stream flush error! " + e.getMessage());
                        e.printStackTrace();
                        completableFuture.completeExceptionally(e);
                    }
                    return this;
                }

            };
        }
        return body;
    }

    public CompletableFuture<HttpResponse> submit() {
        try {
            willSendRequest();
            request.getOutputStream().close();
            request.getOutputStream().flush();
        } catch (Throwable e) {
            e.printStackTrace();
//            completableFuture.completeExceptionally(e);
        }
        CompletableFuture future = completableFuture;
        return future;

    }

    @Override
    public SseClient toSseClient() {
        return new SseClient(this);
    }


    @Override
    public HttpRest onResponseHeader(Consumer<HttpResponse> resp) {
        response.headerCompleted(resp);
        return this;
    }

    @Override
    public HttpRest onResponseBody(Stream streaming) {
        response.onStream(streaming);
        return this;
    }

    public HttpRestImpl onSuccess(Consumer<HttpResponse> consumer) {
        completableFuture.thenAccept(httpResponse -> {
            try {
                consumer.accept(httpResponse);
            } catch (Throwable e) {
                if (throwableConsumer != null) {
                    throwableConsumer.accept(e);
                }
            }
        });
        return this;
    }

    private Consumer<Throwable> throwableConsumer;

    @Override
    public HttpRestImpl onFailure(Consumer<Throwable> consumer) {
        throwableConsumer = consumer;
        completableFuture.exceptionally(throwable -> {
            consumer.accept(throwable);
            return null;
        });
        return this;
    }


    public HttpRest setMethod(String method) {
        request.setMethod(method);
        return this;
    }

    @Override
    public HttpRest body(Consumer<RequestBody> body) {
        RequestBody b = body();
        body.accept(b);
        return this;
    }

    public Header header() {
        return new Header() {
            @Override
            public Header add(String headerName, String headerValue) {
                commitCheck();
                request.addHeader(headerName, headerValue);
                return this;
            }

            @Override
            public Header set(String headerName, String headerValue) {
                commitCheck();
                request.setHeader(headerName, headerValue);
                return this;
            }

            @Override
            public Header setContentType(String contentType) {
                commitCheck();
                request.setContentType(contentType);
                return this;
            }

            @Override
            public Header setContentLength(int contentLength) {
                commitCheck();
                request.setContentLength(contentLength);
                return this;
            }
        };
    }

    @Override
    public HttpRest header(Consumer<Header> header) {
        header.accept(header());
        return this;
    }

    /**
     * 在 uri 后面添加请求参数
     *
     * @param name  参数名
     * @param value 参数值
     */
    public final HttpRestImpl addQueryParam(String name, String value) {
        commitCheck();
        if (queryParams == null) {
            queryParams = new HashMap<>();
        }
        queryParams.put(name, value);
        return this;
    }

    @Override
    public HttpRest addQueryParam(String name, int value) {
        return addQueryParam(name, String.valueOf(value));
    }

    @Override
    public void close() {
        response.getSession().close();
    }

    private void commitCheck() {
        if (commit) {
            throw new IllegalStateException("http request has been commit!");
        }
    }

    /**
     * Http 响应事件
     */
//    public HttpRestImpl onResponse(ResponseHandler responseHandler) {
//        this.responseHandler = Objects.requireNonNull(responseHandler);
//        return this;
//    }
    public HttpRequestImpl getRequest() {
        return request;
    }

    public CompletableFuture<HttpResponseImpl> getCompletableFuture() {
        return completableFuture;
    }
}
