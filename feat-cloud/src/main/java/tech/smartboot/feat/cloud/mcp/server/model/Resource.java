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

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class Resource {
    private final String uri;
    private final String name;
    private String title;
    private String mimeType;
    private String description;
    private String size;

    Resource(String uri, String name) {
        this.uri = uri;
        this.name = name;
    }

    public static Resource of(String uri, String name) {
        return new Resource(uri, name);
    }

    public static TextResource ofText(String uri, String name) {
        return new TextResource(uri, name);
    }

    public static BinaryResource ofBinary(String uri, String name) {
        return new BinaryResource(uri, name);
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

    public Resource setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
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
        private String text;

        public TextResource(String uri, String name) {
            super(uri, name);
            setMimeType("text/plain");
        }

        public String getText() {
            return text;
        }

        public TextResource setText(String text) {
            this.text = text;
            return this;
        }

        @Override
        public TextResource setMimeType(String mimeType) {
            super.setMimeType(mimeType);
            return this;
        }
    }

    public static class BinaryResource extends Resource {
        private String blob;

        BinaryResource(String uri, String name) {
            super(uri, name);
        }

        public String getBlob() {
            return blob;
        }

        public BinaryResource setBlob(String mimeType, String blob) {
            setMimeType(mimeType);
            this.blob = blob;
            return this;
        }
    }
}
