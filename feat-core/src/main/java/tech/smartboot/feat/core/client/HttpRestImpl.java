/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpRest.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.client.impl.HttpRequestImpl;
import tech.smartboot.feat.core.client.impl.HttpResponseImpl;
import tech.smartboot.feat.core.client.stream.Stream;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/3
 */
class HttpRestImpl implements HttpRest {
    private final static String DEFAULT_USER_AGENT = "feat";
    private final HttpRequestImpl request;
    private final CompletableFuture<HttpResponseImpl> completableFuture = new CompletableFuture<>();
    private final AbstractQueue<AbstractResponse> queue;
    private Map<String, String> queryParams = null;
    private boolean commit = false;
    private RequestBody body;

    private final HttpResponseImpl response;

    HttpRestImpl(AioSession session, AbstractQueue<AbstractResponse> queue) {
        this.request = new HttpRequestImpl(session);
        this.queue = queue;
        this.response = new HttpResponseImpl(session, completableFuture);
    }

    protected final void willSendRequest() {
        if (commit) {
            return;
        }
        commit = true;
        resetUri();
        Collection<String> headers = request.getHeaderNames();
        if (!headers.contains(HeaderNameEnum.USER_AGENT.getName())) {
            request.addHeader(HeaderNameEnum.USER_AGENT.getName(), DEFAULT_USER_AGENT);
        }
        AioSession session = response.getSession();
        DecoderUnit attachment = session.getAttachment();
        synchronized (session) {
            if (attachment.getResponse() == null) {
                attachment.setResponse(response);
            } else {
                queue.offer(response);
            }
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
                    } catch (IOException e) {
                        System.out.println("body stream write error! " + e.getMessage());
                        completableFuture.completeExceptionally(e);
                    }
                    return this;
                }

                @Override
                public void transferFrom(ByteBuffer buffer, Consumer<RequestBody> consumer) {
                    try {
                        willSendRequest();
                        request.getOutputStream().transferFrom(buffer, bufferOutputStream -> consumer.accept(HttpRestImpl.this.body));
                    } catch (IOException e) {
                        System.out.println("body stream write error! " + e.getMessage());
                        completableFuture.completeExceptionally(e);
                    }
                }

                @Override
                public RequestBody flush() {
                    try {
                        willSendRequest();
                        request.getOutputStream().flush();
                    } catch (IOException e) {
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

    public Future<HttpResponse> submit() {
        try {
            willSendRequest();
            request.getOutputStream().close();
            request.getOutputStream().flush();
        } catch (Throwable e) {
//            completableFuture.completeExceptionally(e);
        }
        return new Future<HttpResponse>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return completableFuture.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return completableFuture.isCancelled();
            }

            @Override
            public boolean isDone() {
                return completableFuture.isDone();
            }

            @Override
            public HttpResponse get() throws InterruptedException, ExecutionException {
                return completableFuture.get();
            }

            @Override
            public HttpResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return completableFuture.get(timeout, unit);
            }

        };

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
        completableFuture.thenAccept(consumer);
        return this;
    }

    public HttpRestImpl onFailure(Consumer<Throwable> consumer) {
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
