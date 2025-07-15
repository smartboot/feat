/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.mcp.server.model;

import tech.smartboot.feat.core.server.HttpRequest;

/**
 * @author 三刀
 * @version v1.0 7/4/25
 */
public class ResourceContext {
    private final HttpRequest request;
    private final ServerResource resource;

    public ResourceContext(HttpRequest request, ServerResource resource) {
        this.request = request;
        this.resource = resource;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public ServerResource getResource() {
        return resource;
    }
}
