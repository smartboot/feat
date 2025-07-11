/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp;

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
}
