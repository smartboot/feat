/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: AbstractResponse.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.Reset;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.io.BufferOutputStream;
import tech.smartboot.feat.core.server.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Supplier;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public class AbstractResponse implements HttpResponse, Reset {
    /**
     * 输入流
     */
    protected AbstractOutputStream outputStream;

    /**
     * 响应消息头
     */
    private Map<String, HeaderValue> headers = Collections.emptyMap();
    /**
     * http响应码
     */
    private HttpStatus httpStatus = HttpStatus.OK;


    /**
     * 响应正文长度
     */
    private long contentLength = -1;

    /**
     * 正文编码方式
     */
    private String contentType = HeaderValueEnum.ContentType.TEXT_HTML_UTF8;

    private AioSession session;

    /**
     * 是否关闭Socket连接通道
     */
    protected boolean closed = false;

    private List<Cookie> cookies = Collections.emptyList();

    protected void init(AioSession session, AbstractOutputStream outputStream) {
        this.session = session;
        this.outputStream = outputStream;
    }


    public final void reset() {
        outputStream.reset();
        headers.clear();
        setHttpStatus(HttpStatus.OK);
        contentType = HeaderValueEnum.ContentType.TEXT_HTML_UTF8;
        contentLength = -1;
        cookies = Collections.emptyList();
        this.closed = false;
    }


    public final BufferOutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public final void setHttpStatus(HttpStatus httpStatus) {
        Objects.requireNonNull(httpStatus);
        if (httpStatus.value() < 100 || httpStatus.value() > 1000) {
            throw new IllegalArgumentException("httpStatus must between 100 and 1000");
        }
        this.httpStatus = httpStatus;
    }


    public final void setHttpStatus(int value, String reasonPhrase) {
        setHttpStatus(new HttpStatus(value, Objects.requireNonNull(reasonPhrase)));
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
            if (checkSpecialHeader(name, value)) return;
        }
        Map<String, HeaderValue> emptyHeaders = Collections.emptyMap();
        if (headers == emptyHeaders) {
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
        if (name.equalsIgnoreCase(HeaderNameEnum.CONTENT_TYPE.getName())) {
            setContentType(value);
            return true;
        } else if (name.equalsIgnoreCase(HeaderNameEnum.CONTENT_LENGTH.getName())) {
            setContentLength(Long.parseLong(value));
            return true;
        }
        return false;
    }

    @Override
    public final String getHeader(String name) {
        HeaderValue headerValue = headers.get(name);
        return headerValue == null ? null : headerValue.getValue();
    }

    final Map<String, HeaderValue> getHeaders() {
        return headers;
    }

    @Override
    public final Collection<String> getHeaders(String name) {
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
        return new ArrayList<>(headers.keySet());
    }


    public final void write(byte[] buffer) throws IOException {
        outputStream.write(buffer);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            if (outputStream != null && !outputStream.isClosed()) {
                outputStream.close();
            }
        } catch (IOException ignored) {
        } finally {
            session.close(false);
        }
        closed = true;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    @Override
    public void addCookie(Cookie cookie) {
        List<Cookie> emptyList = Collections.emptyList();
        if (cookies == emptyList) {
            cookies = new ArrayList<>();
        }
        cookies.add(cookie);
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public final String getContentType() {
        return contentType;
    }

    @Override
    public final void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 是否要断开TCP连接
     *
     * @return true/false
     */
    public final boolean isClosed() {
        return closed;
    }


    @Override
    public Supplier<Map<String, String>> getTrailerFields() {
        return outputStream.getTrailerFields();
    }
}
