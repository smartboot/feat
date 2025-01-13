/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpRequestImpl.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import org.smartboot.socket.util.Attachment;
import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.Reset;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.common.multipart.MultipartConfig;
import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.server.HttpRequest;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
public class HttpRequestImpl implements HttpRequest, Reset {
    public final HttpEndpoint request;
    /**
     * 释放维持长连接
     */
    private boolean keepAlive;
    private List<Part> parts;
    private boolean multipartParsed;

    private final HttpResponseImpl response;

    HttpRequestImpl(HttpEndpoint request) {
        this.request = request;
        this.response = new HttpResponseImpl(this);
    }

    public final AbstractResponse getResponse() {
        return response;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    @Override
    public BodyInputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    @Override
    public Map<String, String> getTrailerFields() {
        return request.getTrailerFields() == null ? Collections.emptyMap() : request.getTrailerFields();
    }

    @Override
    public boolean isTrailerFieldsReady() {
        return !HeaderValueEnum.TransferEncoding.CHUNKED.equals(getHeader(HeaderNameEnum.TRANSFER_ENCODING)) || request.getTrailerFields() != null;
    }

    @Override
    public void upgrade(HttpUpgradeHandler upgradeHandler) throws IOException {
        request.setUpgradeHandler(upgradeHandler);
        response.getOutputStream().disableChunked();
        //升级后取消http空闲监听
        request.cancelHttpIdleTask();
        upgradeHandler.setRequest(request);
        upgradeHandler.init(this, response);
        upgradeHandler.onBodyStream(request.getAioSession().readBuffer());

    }

    public void reset() {
        request.reset();
        response.reset();

        if (parts != null) {
            for (Part part : parts) {
                try {
                    part.delete();
                } catch (IOException ignore) {
                }
            }
            parts = null;
        }
        multipartParsed = false;
    }

    public Collection<Part> getParts(MultipartConfig configElement) throws IOException {
        if (!multipartParsed) {
            MultipartFormDecoder multipartFormDecoder = new MultipartFormDecoder(this, configElement);
            long remaining = getContentLength();
            if (configElement.getMaxRequestSize() > 0 && configElement.getMaxRequestSize() < remaining) {
                throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
            }
            int p = request.getAioSession().readBuffer().position();
            while (!multipartFormDecoder.decode(request.getAioSession().readBuffer(), this)) {
                remaining -= request.getAioSession().readBuffer().position() - p;
                int readSize = request.getAioSession().read();
                p = request.getAioSession().readBuffer().position();
                if (readSize == -1) {
                    break;
                }
            }
            multipartParsed = true;
            request.setInputStream(BodyInputStream.EMPTY_INPUT_STREAM);
            remaining -= request.getAioSession().readBuffer().position() - p;
            if (remaining != 0) {
                throw new HttpException(HttpStatus.BAD_REQUEST);
            }
        }
        if (parts == null) {
            parts = new ArrayList<>();
        }
        return parts;
    }

    public void setPart(Part part) {
        if (parts == null) {
            parts = new ArrayList<>();
        }
        this.parts.add(part);
    }

    @Override
    public SSLEngine getSslEngine() {
        return request.getSslEngine();
    }

    @Override
    public final String getHeader(String headName) {
        return request.getHeader(headName);
    }

    public final String getHeader(HeaderNameEnum headName) {
        return request.getHeader(headName);
    }

    @Override
    public final Collection<String> getHeaders(String name) {
        return request.getHeaders(name);
    }

    @Override
    public final Collection<String> getHeaderNames() {
        return request.getHeaderNames();
    }


    @Override
    public final String getRequestURI() {
        return request.getRequestURI();
    }

    @Override
    public final HttpProtocolEnum getProtocol() {
        return request.getProtocol();
    }

    @Override
    public final String getMethod() {
        return request.getMethod();
    }

    @Override
    public final String getScheme() {
        return request.getScheme();
    }

    @Override
    public final String getRequestURL() {
        return request.getRequestURL();
    }

    @Override
    public final String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public final String getContentType() {
        return request.getContentType();
    }

    @Override
    public final long getContentLength() {
        return request.getContentLength();
    }

    @Override
    public final String getParameter(String name) {
        return request.getParameter(name);
    }

    @Override
    public final Map<String, String[]> getParameters() {
        return request.getParameters();
    }

    @Override
    public final String[] getParameterValues(String name) {
        return request.getParameters().get(name);
    }

    @Override
    public final String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public final InetSocketAddress getRemoteAddress() {
        return request.getRemoteAddress();
    }

    @Override
    public final InetSocketAddress getLocalAddress() {
        return request.getLocalAddress();
    }

    @Override
    public final String getRemoteHost() {
        return request.getRemoteHost();
    }

    @Override
    public final Locale getLocale() {
        return request.getLocale();
    }

    @Override
    public final Enumeration<Locale> getLocales() {
        return request.getLocales();
    }

    @Override
    public final String getCharacterEncoding() {
        return request.getCharacterEncoding();
    }

    public final HttpEndpoint getRequest() {
        return request;
    }

    @Override
    public Cookie[] getCookies() {
        return request.getCookies();
    }

    @Override
    public Attachment getAttachment() {
        return request.getAttachment();
    }

    @Override
    public void setAttachment(Attachment attachment) {
        request.setAttachment(attachment);
    }


    @Override
    public final boolean isSecure() {
        return request.isSecure();
    }
}
