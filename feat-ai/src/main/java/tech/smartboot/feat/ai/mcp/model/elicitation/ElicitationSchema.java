/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model.elicitation;

import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * elicitation form 模式的 requestedSchema 构造器。
 * <p>
 * 规范限定为扁平对象 + 原始类型属性，不支持嵌套对象或普通对象数组（枚举数组除外）。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class ElicitationSchema {
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final List<String> required = new ArrayList<>();

    private ElicitationSchema() {
    }

    public static ElicitationSchema of() {
        return new ElicitationSchema();
    }

    public ElicitationSchema string(String name, String description, boolean isRequired) {
        return property(name, primitive("string", description, null), isRequired);
    }

    public ElicitationSchema string(String name, String description, boolean isRequired, String defaultValue) {
        return property(name, primitive("string", description, defaultValue), isRequired);
    }

    public ElicitationSchema number(String name, String description, boolean isRequired) {
        return property(name, primitive("number", description, null), isRequired);
    }

    public ElicitationSchema number(String name, String description, boolean isRequired, Number defaultValue) {
        return property(name, primitive("number", description, defaultValue), isRequired);
    }

    public ElicitationSchema integer(String name, String description, boolean isRequired) {
        return property(name, primitive("integer", description, null), isRequired);
    }

    public ElicitationSchema integer(String name, String description, boolean isRequired, Number defaultValue) {
        return property(name, primitive("integer", description, defaultValue), isRequired);
    }

    public ElicitationSchema bool(String name, String description, boolean isRequired) {
        return property(name, primitive("boolean", description, null), isRequired);
    }

    public ElicitationSchema bool(String name, String description, boolean isRequired, Boolean defaultValue) {
        return property(name, primitive("boolean", description, defaultValue), isRequired);
    }

    public ElicitationSchema enumProperty(String name, EnumSchema enumSchema, boolean isRequired) {
        return property(name, enumSchema.toJSON(), isRequired);
    }

    /**
     * 追加任意属性 schema，便于使用规范允许但本类未内置的能力。
     */
    public ElicitationSchema property(String name, JSONObject propertySchema, boolean isRequired) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("property name is required");
        }
        properties.put(name, propertySchema);
        if (isRequired) {
            required.add(name);
        }
        return this;
    }

    private static JSONObject primitive(String type, String description, Object defaultValue) {
        JSONObject json = new JSONObject();
        json.put("type", type);
        if (description != null) {
            json.put("description", description);
        }
        if (defaultValue != null) {
            json.put("default", defaultValue);
        }
        return json;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "object");
        json.put("properties", properties);
        if (!required.isEmpty()) {
            json.put("required", required);
        }
        return json;
    }
}
