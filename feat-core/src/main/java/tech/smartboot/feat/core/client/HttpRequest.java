/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.HeaderName;

import java.io.IOException;
import java.util.Collection;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
public interface HttpRequest {
    String getProtocol();

    String getMethod();

    String getUri();

    /**
     * Sets a response header with the given name and value. If the header had
     * already been set, the new value overwrites the previous one. The
     * <code>containsHeader</code> method can be used to test for the presence
     * of a header before setting its value.
     *
     * @param name  the name of the header
     * @param value the header value If it contains octet string, it should be
     *              encoded according to RFC 2047
     *              (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #addHeader
     */
    void setHeader(String name, String value);

    default void setHeader(HeaderName name, String value) {
        setHeader(name.getName(), value);
    }

    /**
     * Adds a response header with the given name and value. This method allows
     * response headers to have multiple values.
     *
     * @param name  the name of the header
     * @param value the additional header value If it contains octet string, it
     *              should be encoded according to RFC 2047
     *              (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #setHeader
     */
    void addHeader(String name, String value);

    default void addHeader(HeaderName name, String value) {
        addHeader(name.getName(), value);
    }

    default String getHeader(HeaderName name) {
        return getHeader(name.getName());
    }

    String getHeader(String name);

    /**
     * Return a Collection of all the header values associated with the
     * specified header name.
     *
     * @param name Header name to look up
     * @return The values for the specified header. These are the raw values so
     * if multiple values are specified in a single header that will be
     * returned as a single header value.
     * @since Servlet 3.0
     */
    Collection<String> getHeaders(String name);

    /**
     * Get the header names set for this HTTP response.
     *
     * @return The header names set for this HTTP response.
     * @since Servlet 3.0
     */
    Collection<String> getHeaderNames();

    void setContentLength(int contentLength);

    void setContentType(String contentType);

    void write(byte[] data) throws IOException;


    /**
     * 添加Cookie信息
     *
     * @param cookie
     */
    void addCookie(Cookie cookie);
}
