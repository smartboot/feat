/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpResponse.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.client.stream.Stream;

import java.util.function.Consumer;

/**
 * Http消息请求接口
 *
 * @author 三刀
 * @version V1.0 , 2018/8/7
 */
public interface HttpResponse extends Response {

    String getContentType();

    long getContentLength();

    String getCharacterEncoding();

    String body();

    HttpResponse headerCompleted(Consumer<HttpResponse> resp);

    HttpResponse onStream(Stream streaming);
}
