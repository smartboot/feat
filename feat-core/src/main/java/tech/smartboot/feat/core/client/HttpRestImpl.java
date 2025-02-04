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
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;

import java.io.ByteArrayOutputStream;
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
    private Body<HttpRestImpl> body;

    private final HttpResponseImpl response;
    private ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

    private BodyStreaming defaultSteaming = new BodyStreaming() {
        @Override
        public void stream(HttpResponse r, byte[] bytes, boolean end) {
            try {
                bodyStream.write(bytes);
                if (end) {
                    response.setBody(bodyStream.toString());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

    HttpRestImpl(AioSession session, AbstractQueue<AbstractResponse> queue) {
        this.request = new HttpRequestImpl(session);
        this.queue = queue;
        this.response = new HttpResponseImpl(session, completableFuture);
        response.setStreaming(defaultSteaming);
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

    public Body<? extends HttpRestImpl> body() {
        if (body == null) {
            body = new Body<HttpRestImpl>() {

                @Override
                public Body<HttpRestImpl> write(byte[] bytes, int offset, int len) {
                    try {
                        willSendRequest();
                        request.getOutputStream().write(bytes, offset, len);
                    } catch (IOException e) {
                        System.out.println("body stream write error! " + e.getMessage());
                        completableFuture.completeExceptionally(e);
                    }
                    return this;
                }

//                @Override
//                public void write(byte[] bytes, int offset, int len, Consumer<Body<HttpRestImpl>> consumer) {
//                    try {
//                        willSendRequest();
//                        request.getOutputStream().write(bytes, offset, len, bufferOutputStream -> consumer.accept(HttpRestImpl.this.body));
//                    } catch (IOException e) {
//                        System.out.println("body stream write error! " + e.getMessage());
//                        completableFuture.completeExceptionally(e);
//                    }
//                }

                @Override
                public void transferFrom(ByteBuffer buffer, Consumer<Body<HttpRestImpl>> consumer) {
                    try {
                        willSendRequest();
                        request.getOutputStream().transferFrom(buffer, bufferOutputStream -> consumer.accept(HttpRestImpl.this.body));
                    } catch (IOException e) {
                        System.out.println("body stream write error! " + e.getMessage());
                        completableFuture.completeExceptionally(e);
                    }
                }

                @Override
                public Body<HttpRestImpl> flush() {
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

                @Override
                public HttpRestImpl done() {
                    return HttpRestImpl.this;
                }
            };
        }
        return body;
    }

    public Future<HttpResponse> done() {
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

    @Override
    public HttpRest onStream(BodyStreaming streaming) {
        response.setStreaming(streaming);
        return this;
    }

    public HttpRest setMethod(String method) {
        request.setMethod(method);
        return this;
    }


    public Header<? extends HttpRestImpl> header() {
        return new Header<HttpRestImpl>() {
            @Override
            public Header<HttpRestImpl> add(String headerName, String headerValue) {
                request.addHeader(headerName, headerValue);
                return this;
            }

            @Override
            public Header<HttpRestImpl> set(String headerName, String headerValue) {
                request.setHeader(headerName, headerValue);
                return this;
            }

            @Override
            public Header<HttpRestImpl> setContentType(String contentType) {
                request.setContentType(contentType);
                return this;
            }

            @Override
            public Header<HttpRestImpl> setContentLength(int contentLength) {
                request.setContentLength(contentLength);
                return this;
            }

            @Override
            public HttpRestImpl done() {
                return HttpRestImpl.this;
            }
        };
    }

    /**
     * 在 uri 后面添加请求参数
     *
     * @param name  参数名
     * @param value 参数值
     */
    public final HttpRestImpl addQueryParam(String name, String value) {
        if (queryParams == null) {
            queryParams = new HashMap<>();
        }
        queryParams.put(name, value);
        return this;
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
