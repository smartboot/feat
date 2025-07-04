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

public class Argument {
    private String name;
    private String description;
    private boolean required;

    private Argument(String name, String description, boolean required) {
        this.name = name;
        this.description = description;
        this.required = required;
    }

    public static Argument of(String name, String description) {
        return new Argument(name, description, false);
    }

    public static Argument requiredOf(String name, String description) {
        return new Argument(name, description, true);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}