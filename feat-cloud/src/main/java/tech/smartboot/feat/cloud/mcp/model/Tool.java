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

import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Tool {
    private String name;
    private String title;
    private String description;
    private List<Property> inputs = new ArrayList<>();
    private List<Property> outputs = new ArrayList<>();
    private Function<JSONObject, JSONObject> action;

    public String getName() {
        return name;
    }

    public Tool setName(String name) {
        this.name = name;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Tool setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Tool setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<Property> getInputs() {
        return inputs;
    }

    public Tool setInputs(Consumer<Property>... inputs) {
        for (Consumer<Property> input : inputs) {
            Property property = new Property();
            input.accept(property);
            this.inputs.add(property);
        }
        return this;
    }

    public Tool setAction(Function<JSONObject, JSONObject> action) {
        this.action = action;
        return this;
    }

    public Function<JSONObject, JSONObject> getAction() {
        return action;
    }

    public List<Property> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Property> outputs) {
        this.outputs = outputs;
    }

    public enum PropertyType {
        Object("object"), Number("number"), String("string"), Boolean("boolean");
        private final String type;

        PropertyType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class Property {
        private String name;
        private PropertyType type;
        private String description;
        private boolean required;

        public String getName() {
            return name;
        }

        public Property withString(String name, String description) {
            this.name = name;
            this.type = PropertyType.String;
            this.description = description;
            return this;
        }

        public Property withNumber(String name, String description) {
            this.name = name;
            this.type = PropertyType.Number;
            this.description = description;
            return this;
        }

        public Property withBool(String name, String description) {
            this.name = name;
            this.type = PropertyType.Boolean;
            this.description = description;
            return this;
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

        public Property required() {
            this.required = true;
            return this;
        }
    }
}

