/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class FunctionParameters {
    private String type = "object";
    private Map<String, ParameterProperty> properties = new HashMap<>();
    private Set<String> required = new HashSet<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, ParameterProperty> getProperties() {
        return properties;
    }


    public void addParameter(String name, String description, String type, boolean required) {
        this.properties.put(name, new ParameterProperty(type, description));
        if (required) {
            this.required.add(name);
        }
    }

    public Set<String> getRequired() {
        return required;
    }

    public void setRequired(Set<String> required) {
        this.required = required;
    }
}
