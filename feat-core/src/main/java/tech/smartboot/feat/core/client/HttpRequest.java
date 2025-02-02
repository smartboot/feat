/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpRequest.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.Cookie;

import java.io.IOException;
import java.util.Collection;

/**
 * Http消息响应接口
 *
 * @author 三刀
 * @version V1.0 , 2018/2/3
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
    public void setHeader(String name, String value);

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
    public void addHeader(String name, String value);

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
    public Collection<String> getHeaders(String name);

    /**
     * Get the header names set for this HTTP response.
     *
     * @return The header names set for this HTTP response.
     * @since Servlet 3.0
     */
    public Collection<String> getHeaderNames();

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
