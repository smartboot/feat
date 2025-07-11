/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.client.model;

import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 7/10/25
 */
public class ToolListResponse {
    private List<Tool> tools;
    private String nextCursor;

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public class Tool {
        private String name;
        private String title;
        private String description;
        private Schema inputSchema;
        private Schema outputSchema;

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

    public class Property {
        private String type;
        private String description;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
