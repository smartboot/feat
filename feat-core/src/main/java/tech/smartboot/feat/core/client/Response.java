/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpResponse.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import java.util.Collection;

/**
 * Http消息请求接口
 *
 * @author 三刀
 * @version V1.0 , 2018/8/7
 */
interface Response {
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
