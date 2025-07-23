/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 6/28/25
 */
public class Resource {
    /**
     * Unique identifier for the resource
     */
    private final String uri;
    /**
     * Name of the resource
     */
    private final String name;
    /**
     * Optional human-readable name of the resource for display purposes.
     */
    private String title;
    /**
     * Optional MIME type
     */
    private final String mimeType;
    /**
     * Optional description
     */
    private String description;
    /**
     * Optional size in bytes
     */
    private String size;
    private String text;
    private String blob;

    protected Resource(String uri, String name, String mimeType) {
        this.uri = uri;
        this.name = name;
        this.mimeType = mimeType;
    }

    public static Resource of(String uri, String name) {
        return new Resource(uri, name, null);
    }

    public static Resource of(String uri, String name, String mimeType) {
        return new Resource(uri, name, mimeType);
    }

    public static Resource copy(Resource resource) {
        return new Resource(resource.uri, resource.name, resource.mimeType);
    }


//    public static TextServerResource ofText(String uri, String name, String text) {
//        return ofText(uri, name, "text/plain", text);
//    }
//
//    public static TextServerResource ofText(String uri, String name, String mimeType, String text) {
//        return new TextServerResource(ServerResource.of(uri, name, mimeType), text);
//    }
//
//    public static TextServerResource ofText(Resource resource, String text) {
//        return new TextServerResource(resource, text);
//    }
//
//    public static BinaryServerResource ofBinary(String uri, String name, String mimeType, String blob) {
//        return new BinaryServerResource(ServerResource.of(uri, name, mimeType), blob);
//    }
//
//    public static BinaryServerResource ofBinary(Resource resource, String blob) {
//        return new BinaryServerResource(resource, blob);
//    }

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

    public Resource description(String description) {
        this.description = description;
        return this;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getBlob() {
        return blob;
    }

    public void setBlob(String blob) {
        this.blob = blob;
    }

    public static class TextServerResource extends Resource {

        public TextServerResource(Resource resource, String text) {
            super(resource.getUri(), resource.getName(), resource.getMimeType());
            setText(text);
        }

        @Override
        public String getBlob() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public static class BinaryServerResource extends Resource {

        public BinaryServerResource(Resource resource, String blob) {
            super(resource.getUri(), resource.getName(), resource.getMimeType());
            setBlob(blob);
        }

        @Override
        public String getText() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
