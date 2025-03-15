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

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpMethod;
import tech.smartboot.feat.core.common.HeaderName;

import java.util.function.Consumer;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
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

    public HttpPost postJson(Object object) {
        byte[] bytes = JSON.toJSONBytes(object);
        header().set(HeaderName.CONTENT_TYPE, HeaderValue.ContentType.APPLICATION_JSON_UTF8).set(HeaderName.CONTENT_LENGTH, bytes.length);
        body().write(bytes);
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
