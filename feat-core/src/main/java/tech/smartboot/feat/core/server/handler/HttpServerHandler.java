/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpServerHandle.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.handler;

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.common.io.BufferOutputStream;
import tech.smartboot.feat.core.common.io.ReadListener;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.impl.AbstractResponse;
import tech.smartboot.feat.core.server.impl.HttpMessageProcessor;
import tech.smartboot.feat.core.server.impl.HttpRequestImpl;
import tech.smartboot.feat.core.server.impl.Request;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Http消息处理器
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public abstract class HttpServerHandler implements HttpHandler {

    public void onBodyStream(ByteBuffer buffer, Request request) {
        HttpRequestImpl httpRequest = request.newHttpRequest();
        AbstractResponse response = httpRequest.getResponse();
        try {
            if (httpRequest.getInputStream().getReadListener() != null) {
                if (buffer.hasRemaining()) {
                    httpRequest.getInputStream().getReadListener().onDataAvailable();
                }
                return;
            }
            CompletableFuture<Object> future = new CompletableFuture<>();
            boolean keepAlive = isKeepAlive(httpRequest, response);
            httpRequest.setKeepAlive(keepAlive);
            httpRequest.request.getServerHandler().handle(httpRequest, response, future);
            if (request.getUpgradeHandler() == null) {
                finishHttpHandle(httpRequest, future);
            }
        } catch (Throwable e) {
            HttpMessageProcessor.responseError(response, e);
        }
    }

    public void handle(HttpRequest request, HttpResponse response, CompletableFuture<Object> completableFuture) throws Throwable {
        try {
            handle(request, response);
        } finally {
            completableFuture.complete(null);
        }
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Throwable {
    }

    /**
     * Http header 完成解析
     */
    public void onHeaderComplete(Request request) throws IOException {
    }

    /**
     * 断开 TCP 连接
     */
    public void onClose(Request request) {
    }

    private void finishHttpHandle(HttpRequestImpl abstractRequest, CompletableFuture<Object> future) throws IOException {
        if (future.isDone()) {
            if (keepConnection(abstractRequest)) {
                finishResponse(abstractRequest);
            }
            return;
        }

        AioSession session = abstractRequest.request.getAioSession();
        ReadListener readListener = abstractRequest.getInputStream().getReadListener();
        if (readListener == null) {
            session.awaitRead();
        } else {
            //todo
//            abstractRequest.request.setDecoder(session.readBuffer().hasRemaining() ? HttpRequestProtocol.BODY_READY_DECODER : HttpRequestProtocol.BODY_CONTINUE_DECODER);
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

    private void finishResponse(HttpRequestImpl abstractRequest) throws IOException {
        AbstractResponse response = abstractRequest.getResponse();
        //关闭本次请求的输出流
        BufferOutputStream bufferOutputStream = response.getOutputStream();
        if (!bufferOutputStream.isClosed()) {
            bufferOutputStream.close();
        }
        abstractRequest.reset();
    }

    private boolean keepConnection(HttpRequestImpl request) throws IOException {
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

    private boolean isKeepAlive(HttpRequestImpl abstractRequest, AbstractResponse response) {
        String connection = abstractRequest.getRequest().getConnection();
        boolean keepAlive = !HeaderValueEnum.Connection.CLOSE.equals(connection);
        // http/1.0默认短连接，http/1.1默认长连接。此处用 == 性能更高
        if (keepAlive && HttpProtocolEnum.HTTP_10 == abstractRequest.getProtocol()) {
            keepAlive = HeaderValueEnum.Connection.KEEPALIVE.equalsIgnoreCase(connection);
            if (keepAlive) {
                response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValueEnum.Connection.KEEPALIVE);
            }
        }
        return keepAlive;
    }

}
