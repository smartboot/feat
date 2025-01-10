/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpMessageProcessor.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.common.DecodeState;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpServerConfiguration;
import tech.smartboot.feat.core.server.HttpServerHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * http消息处理器
 *
 * @author 三刀
 * @version V1.0 , 2018/6/10
 */
public class HttpMessageProcessor extends AbstractMessageProcessor<Request> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpMessageProcessor.class);
    private static final int MAX_LENGTH = 255 * 1024;
    private HttpServerConfiguration configuration;


    @Override
    public void process0(AioSession session, Request request) {
        DecodeState decodeState = request.getDecodeState();
        try {
            switch (decodeState.getState()) {
                case DecodeState.STATE_HEADER_CALLBACK: {
                    doHttpHeader(request);
                    decodeState.setState(DecodeState.STATE_BODY_READING_CALLBACK);
                }
                case DecodeState.STATE_BODY_READING_CALLBACK: {
                    decodeState.setState(DecodeState.STATE_BODY_READING_MONITOR);
                    HttpUpgradeHandler httpUpgradeHandler = request.getUpgradeHandler();
                    if (httpUpgradeHandler != null) {
                        httpUpgradeHandler.onBodyStream(session.readBuffer());
                    } else {
                        request.getServerHandler().onBodyStream(session.readBuffer(), request);
                    }
                    break;
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    public static void responseError(AbstractResponse response, Throwable throwable) {
        if (throwable instanceof HttpException) {
            HttpStatus httpStatus = ((HttpException) throwable).getHttpStatus();
            responseError(response, httpStatus, httpStatus.getReasonPhrase());
        } else if (throwable.getCause() != null) {
            responseError(response, throwable.getCause());
        } else {
            responseError(response, HttpStatus.INTERNAL_SERVER_ERROR, throwable.fillInStackTrace().toString());
        }
    }

    private static void responseError(AbstractResponse response, HttpStatus httpStatus, String desc) {
        try {
            if (response.outputStream.request.getAioSession().isInvalid()) {
                return;
            }
            response.setHttpStatus(httpStatus);
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(("<center><h1>" + httpStatus.value() + " " + httpStatus.getReasonPhrase() + "</h1>" + desc + "<hr/><a target='_blank' href='https://smartboot.tech/'>feat</a>/" + HttpServerConfiguration.VERSION + "&nbsp;|&nbsp; <a target='_blank' href='https://gitee.com/smartboot/feat'>Gitee</a></center>").getBytes());
        } catch (IOException e) {
            LOGGER.warn("HttpError response exception", e);
        } finally {
            response.close();
        }
    }

    private void doHttpHeader(Request request) throws IOException {
        methodCheck(request);
        uriCheck(request);
        request.getServerHandler().onHeaderComplete(request);
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
        }
        switch (stateMachineEnum) {
            case NEW_SESSION: {
                session.setAttachment(new Request(configuration, session));
                break;
            }
            case PROCESS_EXCEPTION:
                LOGGER.error("process exception", throwable);
                session.close();
                break;
            case SESSION_CLOSED: {
                Request request = session.getAttachment();
                try {
                    if (request.getServerHandler() != null) {
                        request.getServerHandler().onClose(request);
                    }
                } finally {
                    request.cancelWsIdleTask();
                    request.cancelHttpIdleTask();
                }
                break;
            }
            case DECODE_EXCEPTION: {
                LOGGER.warn("http decode exception,", throwable);
                Request request = session.getAttachment();
                responseError(request.newHttpRequest().getResponse(), throwable);
                break;
            }
        }
    }

    public void httpServerHandler(HttpServerHandler httpServerHandler) {
        this.configuration.setHttpServerHandler(Objects.requireNonNull(httpServerHandler));
    }


    /**
     * RFC2616 5.1.1 方法标记指明了在被 Request-URI 指定的资源上执行的方法。
     * 这种方法是大小写敏感的。 资源所允许的方法由 Allow 头域指定(14.7 节)。
     * 响应的返回码总是通知客户某个方法对当前资源是否是被允许的，因为被允许的方法能被动态的改变。
     * 如果服务器能理解某方法但此方法对请求资源不被允许的，
     * 那么源服务器应该返回 405 状态码(方法不允许);
     * 如果源服务器不能识别或没有实现某个方法，那么服务器应返回 501 状态码(没有实现)。
     * 方法 GET 和 HEAD 必须被所有一般的服务器支持。 所有其它的方法是可选的;
     * 然而，如果上面的方法都被实现， 这些方法遵循的语意必须和第 9 章指定的相同
     */
    private void methodCheck(Request request) {
        if (request.getMethod() == null) {
            throw new HttpException(HttpStatus.NOT_IMPLEMENTED);
        }
    }

    /**
     * 1、客户端和服务器都必须支持 Host 请求头域。
     * 2、发送 HTTP/1.1 请求的客户端必须发送 Host 头域。
     * 3、如果 HTTP/1.1 请求不包括 Host 请求头域，服务器必须报告错误 400(Bad Request)。 --服务器必须接受绝对 URIs(absolute URIs)。
     */
    private void hostCheck(Request request) {
        if (request.getHost() == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * RFC2616 3.2.1
     * HTTP 协议不对 URI 的长度作事先的限制，服务器必须能够处理任何他们提供资源的 URI，并 且应该能够处理无限长度的 URIs，这种无效长度的 URL 可能会在客户端以基于 GET 方式的 请求时产生。如果服务器不能处理太长的 URI 的时候，服务器应该返回 414 状态码(此状态码 代表 Request-URI 太长)。
     * 注:服务器在依赖大于 255 字节的 URI 时应谨慎，因为一些旧的客户或代理实现可能不支持这 些长度。
     */
    private void uriCheck(Request request) {
        String originalUri = request.getUri();
        if (StringUtils.length(originalUri) > MAX_LENGTH) {
            throw new HttpException(HttpStatus.URI_TOO_LONG);
        }
        /**
         *http_URL = "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]
         *1. 如果 Request-URI 是绝对地址(absoluteURI)，那么主机(host)是 Request-URI 的 一部分。任何出现在请求里 Host 头域的值应当被忽略。
         *2. 假如 Request-URI 不是绝对地址(absoluteURI)，并且请求包括一个 Host 头域，则主 机(host)由该 Host 头域的值决定.
         *3. 假如由规则1或规则2定义的主机(host)对服务器来说是一个无效的主机(host)， 则应当以一个 400(坏请求)错误消息返回。
         */
        if (originalUri.charAt(0) == '/') {
            request.setRequestURI(originalUri);
            return;
        }
        int schemeIndex = originalUri.indexOf("://");
        if (schemeIndex > 0) {//绝对路径
            int uriIndex = originalUri.indexOf('/', schemeIndex + 3);
            if (uriIndex == StringUtils.INDEX_NOT_FOUND) {
                request.setRequestURI("/");
            } else {
                request.setRequestURI(StringUtils.substring(originalUri, uriIndex));
            }
        } else {
            request.setRequestURI(originalUri);
        }
    }

    public void setConfiguration(HttpServerConfiguration configuration) {
        this.configuration = configuration;
    }

}
