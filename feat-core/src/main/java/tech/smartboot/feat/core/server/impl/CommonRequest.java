/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: Request.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.Attachment;
import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.Reset;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.common.enums.HttpTypeEnum;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.common.utils.Constant;
import tech.smartboot.feat.core.common.utils.HttpUtils;
import tech.smartboot.feat.core.common.utils.NumberUtils;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.Http2ServerHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServerConfiguration;
import tech.smartboot.feat.core.server.ServerHandler;
import tech.smartboot.feat.core.server.WebSocketHandler;

import javax.net.ssl.SSLEngine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
public abstract class CommonRequest implements Reset {
    private static final Locale defaultLocale = Locale.getDefault();
    private static final int INIT_CONTENT_LENGTH = -2;
    private static final int NONE_CONTENT_LENGTH = -1;

    protected final AioSession aioSession;

    /**
     * Http请求头
     */
    protected final Map<String, HeaderValue> headers = new HashMap<>();
    protected final HttpServerConfiguration configuration;
    /**
     * 请求参数
     */
    protected Map<String, String[]> parameters;
    /**
     * 原始的完整请求
     */
    protected String uri;
    protected int headerSize = 0;
    /**
     * 请求方法
     */
    protected String method;
    /**
     * Http协议版本
     */
    protected HttpProtocolEnum protocol = HttpProtocolEnum.HTTP_11;
    protected String requestUri;
    protected String requestUrl;
    protected String contentType;
    protected String connection;
    /**
     * 跟在URL后面的请求信息
     */
    protected String queryString;

    protected long contentLength = INIT_CONTENT_LENGTH;
    protected String remoteAddr;
    protected String remoteHost;
    private String hostHeader;
    /**
     * 消息类型
     */
    private HttpTypeEnum type = null;

    private Cookie[] cookies;
    private final SSLEngine sslEngine;

    /**
     * 附件对象
     */
    private Attachment attachment;

    private ServerHandler serverHandler;
    private boolean multiplexing = false;


    CommonRequest(AioSession aioSession, HttpServerConfiguration configuration) {
        this.aioSession = aioSession;
        this.configuration = configuration;
        this.sslEngine = HttpRequest.SSL_ENGINE_THREAD_LOCAL.get();
        if (sslEngine != null) {
            HttpRequest.SSL_ENGINE_THREAD_LOCAL.remove();
        }
    }

    public SSLEngine getSslEngine() {
        return sslEngine;
    }

    public final String getHost() {
        if (hostHeader == null) {
            hostHeader = getHeader(HeaderNameEnum.HOST);
        }
        return hostHeader;
    }


    final String getInnerHeader(String lowCaseHeaderName) {
        HeaderValue headerValue = headers.get(lowCaseHeaderName);
        return headerValue == null ? null : headerValue.getValue();
    }

    public final String getHeader(HeaderNameEnum headerNameEnum) {
        return getInnerHeader(headerNameEnum.getLowCaseName());
    }

    public final String getHeader(String headName) {
        return getInnerHeader(headName.toLowerCase());
    }


    public final Collection<String> getHeaders(String name) {
        HeaderValue headerValue = headers.get(name.toLowerCase());
        if (headerValue == null) {
            return Collections.emptyList();
        }
        List<String> value = new ArrayList<>(4);
        while (headerValue != null) {
            value.add(headerValue.getValue());
            headerValue = headerValue.getNextValue();
        }
        return value;
    }


    public final Collection<String> getHeaderNames() {
        Set<String> nameSet = new HashSet<>();
        headers.forEach((k, v) -> {
            while (v != null) {
                nameSet.add(v.getName());
                v = v.getNextValue();
            }
        });
        return nameSet;
    }

    public final int getHeaderSize() {
        return headerSize;
    }


    public BodyInputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    private void setHeader(String lowCaseHeader, String headerName, String value) {
        if (value == null) {
            HeaderValue oldValue = headers.remove(lowCaseHeader);
            if (oldValue != null) {
                do {
                    headerSize--;
                } while ((oldValue = oldValue.getNextValue()) != null);
            }
            return;
        }
        HeaderValue headerValue = headers.get(lowCaseHeader);
        if (headerValue == null) {
            headerSize++;
            headers.put(lowCaseHeader, new HeaderValue(headerName, value));
        } else {
            headerValue.setName(headerName);
            headerValue.setValue(value);
            HeaderValue nextValue = headerValue.getNextValue();
            while (nextValue != null) {
                headerSize--;
                nextValue = nextValue.getNextValue();
            }
            headerValue.setNextValue(null);
        }
    }

    public final void setHeader(String headerName, String value) {
        setHeader(headerName.toLowerCase(), headerName, value);
    }

    final void addHeader(String lowCaseHeader, String headerName, String value) {
        HeaderValue oldValue = headers.get(lowCaseHeader);
        if (oldValue != null) {
            while (oldValue.getNextValue() != null) {
                oldValue = oldValue.getNextValue();
            }
            oldValue.setNextValue(new HeaderValue(headerName, value));
            headerSize++;
        } else {
            setHeader(lowCaseHeader, headerName, value);
        }
    }

    public final HttpTypeEnum getRequestType() {
        if (type != null) {
            return type;
        }
        if (serverHandler instanceof WebSocketHandler) {
            type = HttpTypeEnum.WEBSOCKET;
        } else if (serverHandler instanceof Http2ServerHandler) {
            type = HttpTypeEnum.HTTP_2;
        } else {
            type = HttpTypeEnum.HTTP;
        }
        return type;
    }

    public final void setType(HttpTypeEnum type) {
        this.type = type;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }


    public final String getRequestURI() {
        return requestUri;
    }

    public final void setRequestURI(String uri) {
        this.requestUri = uri;
    }


    public final HttpProtocolEnum getProtocol() {
        return protocol;
    }

    public final void setProtocol(HttpProtocolEnum protocol) {
        this.protocol = protocol;
    }

    public final String getMethod() {
        return method;
    }


    public final boolean isSecure() {
        return configuration.isSecure();
    }

    public final void setMethod(String method) {
        this.method = method;
    }

    public final String getUri() {
        return uri;
    }

    public final void setUri(String uri) {
        this.uri = uri;
    }

    public final String getRequestURL() {
        if (requestUrl != null) {
            return requestUrl;
        }
        if (requestUri.startsWith("/")) {
            requestUrl = getScheme() + "://" + getHost() + getRequestURI();
        } else {
            requestUrl = requestUri;
        }
        return requestUrl;
    }

    public final String getScheme() {
        return configuration.isSecure() ? Constant.SCHEMA_HTTPS : Constant.SCHEMA_HTTP;
    }

    public final String getQueryString() {
        return queryString;
    }

    public final void setQueryString(String queryString) {
        this.queryString = queryString;
    }


    public final String getContentType() {
        if (contentType != null) {
            return contentType;
        }
        contentType = getHeader(HeaderNameEnum.CONTENT_TYPE);
        return contentType;
    }

    public final String getConnection() {
        if (connection != null) {
            return connection;
        }
        connection = getHeader(HeaderNameEnum.CONNECTION);
        return connection;
    }


    public long getContentLength() {
        if (contentLength > INIT_CONTENT_LENGTH) {
            return contentLength;
        }
        //不包含content-length,则为：-1
        contentLength = NumberUtils.toLong(getHeader(HeaderNameEnum.CONTENT_LENGTH), NONE_CONTENT_LENGTH);

        return contentLength;
    }


    public final String getParameter(String name) {
        String[] arr = (name != null ? getParameterValues(name) : null);
        return (arr != null && arr.length > 0 ? arr[0] : null);
    }


    public final String[] getParameterValues(String name) {
        if (parameters != null) {
            return parameters.get(name);
        }
        parameters = new HashMap<>();
        //识别url中的参数
        String urlParamStr = queryString;
        if (StringUtils.isNotBlank(urlParamStr)) {
            urlParamStr = StringUtils.substringBefore(urlParamStr, "#");
            HttpUtils.decodeParamString(urlParamStr, parameters);
        }

        //application/x-www-form-urlencoded
        //application/x-www-form-urlencoded;charset=utf-8
        String contentTypeTmp = getContentType();
        if (contentTypeTmp != null && contentTypeTmp.startsWith(HeaderValueEnum.X_WWW_FORM_URLENCODED.getName())) {
            try {
                InputStream inputStream = getInputStream();
                if (inputStream != BodyInputStream.EMPTY_INPUT_STREAM) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, len);
                    }
                    HttpUtils.decodeParamString(outputStream.toString(), parameters);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return getParameterValues(name);
    }


    public final Map<String, String[]> getParameters() {
        if (parameters == null) {
            getParameter("");
        }
        return parameters;
    }

    /**
     * Returns the Internet Protocol (IP) address of the client
     * or last proxy that sent the request.
     * For HTTP servlets, same as the value of the
     * CGI variable <code>REMOTE_ADDR</code>.
     *
     * @return a <code>String</code> containing the
     * IP address of the client that sent the request
     */

    public abstract String getRemoteAddr();


    public abstract InetSocketAddress getRemoteAddress();


    public abstract InetSocketAddress getLocalAddress();

    /**
     * Returns the fully qualified name of the client
     * or the last proxy that sent the request.
     * If the engine cannot or chooses not to resolve the hostname
     * (to improve performance), this method returns the dotted-string form of
     * the IP address. For HTTP servlets, same as the value of the CGI variable
     * <code>REMOTE_HOST</code>.
     *
     * @return a <code>String</code> containing the fully
     * qualified name of the client
     */

    public abstract String getRemoteHost();


    public final Locale getLocale() {
        return getLocales().nextElement();
    }


    public final Enumeration<Locale> getLocales() {
        Collection<String> acceptLanguage = getHeaders(HeaderNameEnum.ACCEPT_LANGUAGE.getName());
        if (acceptLanguage.isEmpty()) {
            return Collections.enumeration(Collections.singletonList(defaultLocale));
        }
        List<Locale> locales = new ArrayList<>();
        for (String language : acceptLanguage) {
            for (String lan : language.split(",")) {
                locales.add(Locale.forLanguageTag(lan));
            }
        }
        return Collections.enumeration(locales);
    }


    public final String getCharacterEncoding() {
        return "utf8";
    }


    public final Cookie[] getCookies() {
        if (cookies != null) {
            return cookies;
        }

        HeaderValue headerValue = headers.get(HeaderNameEnum.COOKIE.getLowCaseName());
        if (headerValue == null) {
            return new Cookie[0];
        }
        final List<Cookie> parsedCookies = new ArrayList<>();
        while (headerValue != null) {
            parsedCookies.addAll(HttpUtils.decodeCookies(headerValue.getValue()));
            headerValue = headerValue.getNextValue();
        }
        cookies = new Cookie[parsedCookies.size()];
        parsedCookies.toArray(cookies);
        return cookies;
    }

    /**
     * 获取附件对象
     *
     * @return 附件
     */
    public final Attachment getAttachment() {
        return attachment;
    }

    /**
     * 存放附件，支持任意类型
     *
     * @param attachment 附件对象
     */
    public final void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    public AioSession getAioSession() {
        return aioSession;
    }

    public final HttpServerConfiguration getConfiguration() {
        return configuration;
    }

    public boolean isMultiplexing() {
        return multiplexing;
    }

    @Override
    public void reset() {
        headerSize = 0;
        headers.clear();
        uri = null;
        requestUrl = null;
        parameters = null;
        contentType = null;
        contentLength = INIT_CONTENT_LENGTH;
        cookies = null;
        type = null;
        queryString = null;
        requestUri = null;
        multiplexing = true;
    }
}
