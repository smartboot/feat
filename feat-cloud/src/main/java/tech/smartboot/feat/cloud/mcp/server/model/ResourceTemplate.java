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
public class ResourceTemplate {
    private final String uriTemplate;
    private final String name;
    private String title;
    private String mimeType;
    private String description;

    ResourceTemplate(String uriTemplate, String name) {
        this.uriTemplate = uriTemplate;
        this.name = name;
    }


    public static ResourceTemplate of(String uriTemplate, String name) {
        return new ResourceTemplate(uriTemplate, name);
    }

    public String getUriTemplate() {
        return uriTemplate;
    }

    public String getName() {
        return name;
    }


    public String getTitle() {
        return title;
    }

    public ResourceTemplate title(String title) {
        this.title = title;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public ResourceTemplate mimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ResourceTemplate description(String description) {
        this.description = description;
        return this;
    }


}
