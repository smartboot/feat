/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: AbstractRequest.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server.impl;

import tech.smartboot.feat.common.Cookie;
import tech.smartboot.feat.common.Reset;
import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.server.HttpRequest;
import org.smartboot.socket.util.Attachment;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
abstract class AbstractRequest implements HttpRequest, Reset {

    public Request request;

    protected void init(Request request) {
        this.request = request;
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

    public final Request getRequest() {
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

    public abstract AbstractResponse getResponse();

    @Override
    public final boolean isSecure() {
        return request.isSecure();
    }

}
