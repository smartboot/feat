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

import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.Reset;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.utils.NumberUtils;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public abstract class AbstractResponse implements Response, Reset {

    private static final int INIT_CONTENT_LENGTH = -2;
    private static final int NONE_CONTENT_LENGTH = -1;
    /**
     * Http请求头
     */
    private final List<HeaderValue> headers = new ArrayList<>(8);
    private final AioSession session;
    private int headerSize = 0;
    /**
     * Http协议版本
     */
    private String protocol;
    private String contentType;
    private long contentLength = INIT_CONTENT_LENGTH;

    /**
     * http 响应码
     */
    private int status;
    /**
     * 响应码描述
     */
    private String reasonPhrase;
    private String encoding;
    private final CompletableFuture<AbstractResponse> future;

    public AbstractResponse(AioSession session, CompletableFuture<AbstractResponse> future) {
        this.session = session;
        this.future = future;
    }

    public AioSession getSession() {
        return session;
    }

    public final String getHeader(String headName) {
        for (int i = 0; i < headerSize; i++) {
            HeaderValue headerValue = headers.get(i);
            if (headerValue.getName().equalsIgnoreCase(headName)) {
                return headerValue.getValue();
            }
        }
        return null;
    }

    public final Collection<String> getHeaders(String name) {
        List<String> value = new ArrayList<>(4);
        for (int i = 0; i < headerSize; i++) {
            HeaderValue headerValue = headers.get(i);
            if (headerValue.getName().equalsIgnoreCase(name)) {
                value.add(headerValue.getValue());
            }
        }
        return value;
    }

    public final Collection<String> getHeaderNames() {
        Set<String> nameSet = new HashSet<>();
        for (int i = 0; i < headerSize; i++) {
            nameSet.add(headers.get(i).getName());
        }
        return nameSet;
    }

    public final void setHeader(String headerName, String value) {
        if (headerSize < headers.size()) {
            HeaderValue headerValue = headers.get(headerSize);
            headerValue.setName(headerName);
            headerValue.setValue(value);
        } else {
            headers.add(new HeaderValue(headerName, value));
        }
        headerSize++;
    }

    public final String getProtocol() {
        return protocol;
    }

    public final void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public final String getContentType() {
        if (contentType != null) {
            return contentType;
        }
        contentType = getHeader(HeaderNameEnum.CONTENT_TYPE.getName());
        return contentType;
    }

    public final long getContentLength() {
        if (contentLength > INIT_CONTENT_LENGTH) {
            return contentLength;
        }
        //不包含content-length,则为：-1
        contentLength = NumberUtils.toLong(getHeader(HeaderNameEnum.CONTENT_LENGTH.getName()), NONE_CONTENT_LENGTH);
        return contentLength;
    }

    public final String getCharacterEncoding() {
        if (encoding != null) {
            return encoding;
        }
        String contentType = getContentType();
        String charset = StringUtils.substringAfter(contentType, "charset=");
        if (StringUtils.isNotBlank(charset)) {
            this.encoding = Charset.forName(charset).name();
        } else {
            this.encoding = "utf8";
        }
        return this.encoding;
    }


    public int statusCode() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }


    public CompletableFuture<AbstractResponse> getFuture() {
        return future;
    }

    /**
     * Http header 完成解析
     */
    protected abstract void onHeaderComplete();

    /**
     * 解析 body 数据流
     *
     * @param buffer
     * @return
     */
    protected abstract void onBodyStream(ByteBuffer buffer);
}
