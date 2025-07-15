/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.impl;

import tech.smartboot.feat.core.client.HttpRequest;
import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpMethod;
import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.io.FeatOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
class AbstractRequest implements HttpRequest {
    private String uri;
    private String protocol;
    /**
     * 请求类型
     */
    private String method;
    /**
     * 输入流
     */
    private AbstractOutputStream outputStream;

    /**
     * 响应消息头
     */
    private Map<String, HeaderValue> headers = null;

    /**
     * 响应正文长度
     */
    private int contentLength = -1;

    /**
     * 正文编码方式
     */
    private String contentType;

    private List<Cookie> cookies;

    protected void init(AbstractOutputStream outputStream) {
        this.outputStream = outputStream;
    }


    public final FeatOutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public final void setHeader(String name, String value) {
        setHeader(name, value, true);
    }

    @Override
    public final void addHeader(String name, String value) {
        setHeader(name, value, false);
    }

    /**
     * @param name    header name
     * @param value   header value
     * @param replace true:replace,false:append
     */
    private void setHeader(String name, String value, boolean replace) {
        char cc = name.charAt(0);
        if (cc == 'C' || cc == 'c') {
            if (checkSpecialHeader(name, value)) {
                return;
            }

        }

        if (headers == null) {
            headers = new HashMap<>();
        }
        if (replace) {
            if (value == null) {
                headers.remove(name);
            } else {
                headers.put(name, new HeaderValue(null, value));
            }
            return;
        }

        HeaderValue headerValue = headers.get(name);
        if (headerValue == null) {
            setHeader(name, value, true);
            return;
        }
        HeaderValue preHeaderValue = null;
        while (headerValue != null && !headerValue.getValue().equals(value)) {
            preHeaderValue = headerValue;
            headerValue = headerValue.getNextValue();
        }
        if (headerValue == null) {
            preHeaderValue.setNextValue(new HeaderValue(null, value));
        }
    }

    /**
     * 部分header需要特殊处理
     */
    private boolean checkSpecialHeader(String name, String value) {
        if (name.equalsIgnoreCase(HeaderName.CONTENT_TYPE.getName())) {
            setContentType(value);
            return true;
        } else if (name.equalsIgnoreCase(HeaderName.CONTENT_LENGTH.getName())) {
            setContentLength(Integer.parseInt(value));
            return true;
        }
        return false;
    }

    @Override
    public final String getHeader(String name) {
        HeaderValue headerValue = null;
        if (headers != null) {
            headerValue = headers.get(name);
        }
        return headerValue == null ? null : headerValue.getValue();
    }

    final Map<String, HeaderValue> getHeaders() {
        return headers;
    }

    @Override
    public final Collection<String> getHeaders(String name) {
        if (headers == null) {
            return Collections.emptyList();
        }
        Vector<String> result = new Vector<>();
        HeaderValue headerValue = headers.get(name);
        while (headerValue != null) {
            result.addElement(headerValue.getValue());
            headerValue = headerValue.getNextValue();
        }
        return result;
    }

    @Override
    public final Collection<String> getHeaderNames() {
        if (headers == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(headers.keySet());
    }


    public final void write(byte[] buffer) throws IOException {
        outputStream.write(buffer);
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    @Override
    public void addCookie(Cookie cookie) {
        if (cookies == null) {
            cookies = new ArrayList<>();
        }
        cookies.add(cookie);
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
        if (contentLength >= 0) {
            outputStream.disableChunked();
        }
    }

    public final String getContentType() {
        return contentType;
    }

    public final void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
        if (HttpMethod.GET.equals(method)) {
            outputStream.disableChunked();
        }
    }

    @Override
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
        if (!HttpProtocol.HTTP_11.getProtocol().equals(protocol)) {
            outputStream.disableChunked();
        }
    }
}
