/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.server.model;

import tech.smartboot.feat.ai.mcp.model.Resource;

import java.util.function.Function;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 6/28/25
 */
public class ServerResource extends Resource {
    private Function<ResourceContext, String> action;
    private final boolean isText;

    private ServerResource(String uri, String name, String mimeType, boolean isText) {
        super(uri, name, mimeType);
        this.isText = isText;
    }

    public ServerResource doAction(Function<ResourceContext, String> action) {
        this.action = action;
        return this;
    }

    public Function<ResourceContext, String> getAction() {
        return action;
    }

    @Override
    public ServerResource description(String description) {
        super.description(description);
        return this;
    }

    public static ServerResource ofText(String uri, String name, String text) {
        return ofText(uri, name, "text/plain", text);
    }

    public static ServerResource ofText(String uri, String name, String mimeType, String text) {
        return new ServerResource(uri, name, mimeType, true).doAction(resourceContext -> text);
    }

    public static ServerResource ofText(Resource resource, String text) {
        return ofText(resource.getUri(), resource.getName(), resource.getMimeType(), text);
    }

    public static ServerResource ofBinary(String uri, String name) {
        return ofBinary(uri, name, null, null);
    }

    public static ServerResource ofBinary(String uri, String name, String mimeType) {
        return ofBinary(uri, name, mimeType, null);
    }

    public static ServerResource ofBinary(String uri, String name, String mimeType, String blob) {
        return new ServerResource(uri, name, mimeType, false).doAction(resourceContext -> blob);
    }

    public static ServerResource ofBinary(Resource resource, String blob) {
        return ofBinary(resource.getUri(), resource.getName(), resource.getMimeType(), blob);
    }

    public boolean isText() {
        return isText;
    }
}
