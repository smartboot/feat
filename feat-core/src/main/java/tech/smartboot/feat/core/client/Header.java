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

import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HeaderName;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public interface Header {
    /**
     * 添加 header，支持同名追加
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    Header add(String headerName, String headerValue);

    /**
     * 添加 header，支持同名追加
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    default Header add(String headerName, int headerValue) {
        add(headerName, String.valueOf(headerValue));
        return this;
    }

    /**
     * 设置header，覆盖同名header
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    Header set(String headerName, String headerValue);

    /**
     * 设置header，覆盖同名header
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    default Header set(String headerName, int headerValue) {
        set(headerName, String.valueOf(headerValue));
        return this;
    }

    Header setContentType(String contentType);

    Header setContentLength(int contentLength);

    default Header keepalive(boolean flag) {
        return keepalive(flag ? HeaderValue.Connection.KEEPALIVE : HeaderValue.Connection.CLOSE);
    }

    default Header keepalive(String headerValue) {
        return set(HeaderName.CONNECTION.getName(), headerValue);
    }
}
