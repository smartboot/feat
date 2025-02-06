/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpPost.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.HttpMethod;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/4
 */
public final class HttpPost extends HttpRestWrapper {

    HttpPost(HttpRestImpl rest) {
        super(rest);
        rest.setMethod(HttpMethod.POST);
    }


    public HttpPost postBody(Consumer<PostBody> body) {
        body.accept(body());
        return this;
    }

    @Override
    public PostBody body() {
        return new PostBody(this);
    }

    @Override
    public HttpPost onSuccess(Consumer<HttpResponse> consumer) {
        super.onSuccess(consumer);
        return this;
    }

    @Override
    public HttpPost onFailure(Consumer<Throwable> consumer) {
        super.onFailure(consumer);
        return this;
    }

    @Override
    public HttpPost header(Consumer<Header> header) {
        super.header(header);
        return this;
    }
}
