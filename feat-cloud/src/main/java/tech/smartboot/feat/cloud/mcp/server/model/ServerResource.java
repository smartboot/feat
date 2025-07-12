/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.server.model;

import tech.smartboot.feat.cloud.mcp.model.Resource;

import java.util.function.Function;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class ServerResource extends Resource {
    private Function<ResourceContext, ServerResource> action;

    protected ServerResource(String uri, String name, String mimeType) {
        super(uri, name, mimeType);
    }

    public ServerResource doAction(Function<ResourceContext, ServerResource> action) {
        this.action = action;
        return this;
    }

    public Function<ResourceContext, ServerResource> getAction() {
        return action;
    }

    public static ServerResource of(String uri, String name) {
        return new ServerResource(uri, name, null);
    }

    public static ServerResource of(String uri, String name, String mimeType) {
        return new ServerResource(uri, name, mimeType);
    }

    public static TextServerResource ofText(String uri, String name, String text) {
        return ofText(uri, name, "text/plain", text);
    }

    public static TextServerResource ofText(String uri, String name, String mimeType, String text) {
        return new TextServerResource(ServerResource.of(uri, name, mimeType), text);
    }

    public static TextServerResource ofText(ServerResource resource, String text) {
        return new TextServerResource(resource, text);
    }

    public static BinaryServerResource ofBinary(String uri, String name, String mimeType, String blob) {
        return new BinaryServerResource(ServerResource.of(uri, name, mimeType), blob);
    }

    public static BinaryServerResource ofBinary(ServerResource resource, String blob) {
        return new BinaryServerResource(resource, blob);
    }

    public static class TextServerResource extends ServerResource {

        public TextServerResource(ServerResource resource, String text) {
            super(resource.getUri(), resource.getName(), resource.getMimeType());
            setText(text);
            this.doAction(action -> this);
        }

        @Override
        public String getBlob() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public static class BinaryServerResource extends ServerResource {

        BinaryServerResource(ServerResource resource, String blob) {
            super(resource.getUri(), resource.getName(), resource.getMimeType());
            setBlob(blob);
            this.doAction(action -> this);
        }

        @Override
        public String getText() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
