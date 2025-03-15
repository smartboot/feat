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

import tech.smartboot.feat.core.common.HeaderName;

import java.util.Collection;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
interface Response {
    /**
     * 获取指定名称的Http Header值
     */
    default String getHeader(HeaderName headName) {
        return getHeader(headName.getName());
    }

    /**
     * 获取指定名称的Http Header值
     */
    String getHeader(String headName);


    Collection<String> getHeaders(String name);

    Collection<String> getHeaderNames();

    String getProtocol();

    /**
     * 获取响应码
     *
     * @return
     */
    int statusCode();

    /**
     * 获取响应描述
     *
     * @return
     */
    String getReasonPhrase();
}
