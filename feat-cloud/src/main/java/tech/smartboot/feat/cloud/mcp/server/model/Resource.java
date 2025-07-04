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

import java.util.function.Function;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class Resource {
    private final String uri;
    private final String name;
    private String title;
    private final String mimeType;
    private String description;
    private String size;
    private Function<ResourceContext, Resource> action;

    Resource(String uri, String name, String mimeType) {
        this.uri = uri;
        this.name = name;
        this.mimeType = mimeType;
    }

    public Resource doAction(Function<ResourceContext, Resource> action) {
        this.action = action;
        return this;
    }

    public Function<ResourceContext, Resource> getAction() {
        return action;
    }

    public static Resource of(String uri, String name) {
        return new Resource(uri, name, null);
    }

    public static Resource of(String uri, String name, String mimeType) {
        return new Resource(uri, name, mimeType);
    }

    public static TextResource ofText(String uri, String name, String text) {
        return ofText(uri, name, "text/plain", text);
    }

    public static TextResource ofText(String uri, String name, String mimeType, String text) {
        return new TextResource(Resource.of(uri, name, mimeType), text);
    }

    public static TextResource ofText(Resource resource, String text) {
        return new TextResource(resource, text);
    }

    public static BinaryResource ofBinary(String uri, String name, String mimeType, String blob) {
        return new BinaryResource(Resource.of(uri, name, mimeType), blob);
    }

    public static BinaryResource ofBinary(Resource resource, String blob) {
        return new BinaryResource(resource, blob);
    }

    public String getUri() {
        return uri;
    }


    public String getName() {
        return name;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public static class TextResource extends Resource {
        private final String text;

        public TextResource(Resource resource, String text) {
            super(resource.uri, resource.name, resource.mimeType);
            this.text = text;
            this.doAction(action -> this);
        }

        public String getText() {
            return text;
        }
    }

    public static class BinaryResource extends Resource {
        private final String blob;

        BinaryResource(Resource resource, String blob) {
            super(resource.uri, resource.name, resource.mimeType);
            this.blob = blob;
            this.doAction(action -> this);
        }

        public String getBlob() {
            return blob;
        }
    }
}
