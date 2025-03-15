/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.server;

import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.common.multipart.MultipartConfig;
import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.server.impl.Upgrade;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public interface HttpRequest {
    ThreadLocal<SSLEngine> SSL_ENGINE_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);

    /**
     * 获取Http Response
     *
     * @return
     */
    HttpResponse getResponse();

    /**
     * 获取指定名称的Http Header值
     *
     * @param headName
     * @return
     */
    String getHeader(String headName);


    Collection<String> getHeaders(String name);

    Collection<String> getHeaderNames();

    BodyInputStream getInputStream() throws IOException;

    String getRequestURI();

    HttpProtocol getProtocol();

    /**
     * Returns the name of the HTTP method with which this
     * request was made, for example, GET, POST, or PUT.
     * Same as the value of the CGI variable REQUEST_METHOD.
     *
     * @return a <code>String</code>
     * specifying the name
     * of the method with which
     * this request was made
     */
    String getMethod();

    /**
     * 是否启动安全通信
     */
    boolean isSecure();

    String getScheme();

    String getRequestURL();

    String getQueryString();

    String getContentType();

    long getContentLength();

    String getParameter(String name);

    String[] getParameterValues(String name);

    Map<String, String[]> getParameters();

    default Collection<Part> getParts() throws IOException {
        return getParts(new MultipartConfig());
    }

    Collection<Part> getParts(MultipartConfig configElement) throws IOException;

    default Map<String, String> getTrailerFields() {
        return Collections.emptyMap();
    }

    default boolean isTrailerFieldsReady() {
        return false;
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
    String getRemoteAddr();

    InetSocketAddress getRemoteAddress();

    InetSocketAddress getLocalAddress();

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
    String getRemoteHost();

    Locale getLocale();

    Enumeration<Locale> getLocales();

    String getCharacterEncoding();

    /**
     * 获取Cookie
     */
    Cookie[] getCookies();


    SSLEngine getSslEngine();

    default PushBuilder newPushBuilder() {
        return null;
    }

    void upgrade(Upgrade upgrade) throws IOException;
}
