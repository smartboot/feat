/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

import java.util.Map;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class Context {
    public final HttpRequest Request;
    public final HttpResponse Response;
    private final Map<String, String> pathParams;

    public Context(HttpRequest request, Map<String, String> pathParams) {
        this.Request = request;
        this.Response = request.getResponse();
        this.pathParams = pathParams;
    }

    public String pathParam(String key) {
        return pathParams.get(key);
    }
}
