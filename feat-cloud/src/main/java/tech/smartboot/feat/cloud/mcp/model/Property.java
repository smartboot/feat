/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.model;

public class Property {
    private final String name;
    private final PropertyType type;
    private final String description;
    private final boolean required;

    Property(String name, PropertyType type, String description, boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public static Property withString(String name, String description) {
        return new Property(name, PropertyType.String, description, false);
    }

    public static Property withRequiredString(String name, String description) {
        return new Property(name, PropertyType.String, description, true);
    }

    public static Property withNumber(String name, String description) {
        return new Property(name, PropertyType.Number, description, false);
    }

    public static Property withRequiredNumber(String name, String description) {
        return new Property(name, PropertyType.Number, description, false);
    }

    public static Property ofBool(String name, String description) {
        return new Property(name, PropertyType.Boolean, description, false);
    }


    public PropertyType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }
}