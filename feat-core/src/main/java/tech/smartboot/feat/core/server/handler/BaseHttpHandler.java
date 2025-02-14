/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpServerHandle.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.handler;

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.common.io.ReadListener;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.AbstractResponse;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.core.server.impl.HttpMessageProcessor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Http消息处理器
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public abstract class BaseHttpHandler implements HttpHandler {

    public void onBodyStream(ByteBuffer buffer, HttpEndpoint request) {
        AbstractResponse response = request.getResponse();
        try {
            if (request.getInputStream().getReadListener() != null) {
                if (buffer.hasRemaining()) {
                    request.getInputStream().getReadListener().onDataAvailable();
                }
                return;
            }
            CompletableFuture<Object> future = new CompletableFuture<>();
            boolean keepAlive = isKeepAlive(request, response);
            request.setKeepAlive(keepAlive);
            request.getServerHandler().handle(request, future);
            if (request.getUpgrade() == null) {
                finishHttpHandle(request, future);
            }
        } catch (Throwable e) {
            HttpMessageProcessor.responseError(response, e);
        }
    }

    public void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
        try {
            handle(request);
        } finally {
            completableFuture.complete(null);
        }
    }

    @Override
    public void handle(HttpRequest request) throws Throwable {
    }

    /**
     * Http header 完成解析
     */
    public void onHeaderComplete(HttpEndpoint request) throws IOException {
    }

    /**
     * 断开 TCP 连接
     */
    public void onClose(HttpEndpoint request) {
    }

    private void finishHttpHandle(HttpEndpoint abstractRequest, CompletableFuture<Object> future) throws IOException {
        if (future.isDone()) {
            if (keepConnection(abstractRequest)) {
                finishResponse(abstractRequest);
            }
            return;
        }

        AioSession session = abstractRequest.getAioSession();
        ReadListener readListener = abstractRequest.getInputStream().getReadListener();
        if (readListener == null) {
            session.awaitRead();
        } else if (abstractRequest.getAioSession().readBuffer().hasRemaining()) {
            abstractRequest.getInputStream().getReadListener().onDataAvailable();
        }

        Thread thread = Thread.currentThread();
        AbstractResponse response = abstractRequest.getResponse();
        future.thenRun(() -> {
            try {
                if (keepConnection(abstractRequest)) {
                    finishResponse(abstractRequest);
                    if (thread != Thread.currentThread()) {
                        session.writeBuffer().flush();
                    }
                }
            } catch (Exception e) {
                HttpMessageProcessor.responseError(response, e);
            } finally {
                if (readListener == null) {
                    session.signalRead();
                }
            }
        }).exceptionally(throwable -> {
            try {
                HttpMessageProcessor.responseError(response, throwable);
            } finally {
                if (readListener == null) {
                    session.signalRead();
                }
            }
            return null;
        });
    }

    private void finishResponse(HttpEndpoint abstractRequest) throws IOException {
        AbstractResponse response = abstractRequest.getResponse();
        //关闭本次请求的输出流
        FeatOutputStream bufferOutputStream = response.getOutputStream();
        if (!bufferOutputStream.isClosed()) {
            bufferOutputStream.close();
        }
        abstractRequest.reset();
    }

    private boolean keepConnection(HttpEndpoint request) throws IOException {
        if (request.getResponse().isClosed()) {
            return false;
        }
        //非keepAlive或者 body部分未读取完毕,释放连接资源
        if (!request.isKeepAlive() || !request.getInputStream().isFinished()) {
            request.getResponse().close();
            return false;
        }
        return true;
    }

    private boolean isKeepAlive(HttpEndpoint abstractRequest, AbstractResponse response) {
        String connection = abstractRequest.getConnection();
        boolean keepAlive = !HeaderValue.Connection.CLOSE.equals(connection);
        // http/1.0默认短连接，http/1.1默认长连接。此处用 == 性能更高
        if (keepAlive && HttpProtocolEnum.HTTP_10 == abstractRequest.getProtocol()) {
            keepAlive = HeaderValue.Connection.KEEPALIVE.equalsIgnoreCase(connection);
            if (keepAlive) {
                response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValue.Connection.KEEPALIVE);
            }
        }
        return keepAlive;
    }

}
