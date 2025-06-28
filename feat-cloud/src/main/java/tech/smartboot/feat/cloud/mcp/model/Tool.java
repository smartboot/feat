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

import java.util.List;

public class Tool {
    private String name;
    private String title;
    private String description;
    private List<Property> inputs;
    private List<Property> outputs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Property> getInputs() {
        return inputs;
    }

    public void setInputs(List<Property> inputs) {
        this.inputs = inputs;
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

        public void setName(String name) {
            this.name = name;
        }

        public PropertyType getType() {
            return type;
        }

        public void setType(PropertyType type) {
            this.type = type;
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
}

