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

import com.alibaba.fastjson2.annotation.JSONField;
import tech.smartboot.feat.ai.mcp.enums.PropertyType;

import java.util.List;
import java.util.Map;

public class Tool {
    private String name;
    private String title;
    private String description;
    protected Schema inputSchema;
    protected Schema outputSchema;

    public void setName(String name) {
        this.name = name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Schema getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Schema inputSchema) {
        this.inputSchema = inputSchema;
    }

    public Schema getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(Schema outputSchema) {
        this.outputSchema = outputSchema;
    }

    public static Property stringProperty(String name, String description) {
        return new Property(name, PropertyType.String, description, false);
    }

    public static Property requiredStringProperty(String name, String description) {
        return new Property(name, PropertyType.String, description, true);
    }

    public static Property numberProperty(String name, String description) {
        return new Property(name, PropertyType.Number, description, false);
    }

    public static Property requiredNumberProperty(String name, String description) {
        return new Property(name, PropertyType.Number, description, true);
    }

    public static Property boolProperty(String name, String description) {
        return new Property(name, PropertyType.Boolean, description, false);
    }

    public static Property requiredBoolProperty(String name, String description) {
        return new Property(name, PropertyType.Boolean, description, true);
    }

    public class Schema {
        private String type;
        private Map<String, Property> properties;
        private List<String> required;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Property> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Property> properties) {
            this.properties = properties;
        }

        public List<String> getRequired() {
            return required;
        }

        public void setRequired(List<String> required) {
            this.required = required;
        }
    }

    public static class Property {
        @JSONField(serialize = false)
        private String name;
        private String type;
        private String description;
        @JSONField(serialize = false)
        private boolean required;

        public Property() {
        }

        Property(String name, PropertyType type, String description, boolean required) {
            this.name = name;
            this.type = type.getType();
            this.description = description;
            this.required = required;
        }

        public String getName() {
            return name;
        }


        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}