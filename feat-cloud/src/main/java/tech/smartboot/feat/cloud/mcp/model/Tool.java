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
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class Tool {
    /**
     * The name of the tool. Must be unique.
     */
    private String name;
    /**
     * Optional human-readable name of the tool for display purposes.
     */
    private String title;
    /**
     * Human-readable description of functionality
     */
    private String description;
    private final List<Property> inputSchema = new ArrayList<>();
    private List<Property> outputSchema = new ArrayList<>();
    private Function<JSONObject, JSONObject> action;

    private Tool() {
    }

    public static Tool of(String name) {
        Tool tool = new Tool();
        tool.setName(name);
        return tool;
    }

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

    public Tool title(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Tool description(String description) {
        this.description = description;
        return this;
    }

    public List<Property> getInputs() {
        return inputSchema;
    }

    public final Tool inputSchema(Property... inputs) {
        if (inputs == null) {
            return this;
        }
        inputSchema.addAll(Arrays.asList(inputs));
        return this;
    }

    public Tool doAction(Function<JSONObject, JSONObject> action) {
        this.action = action;
        return this;
    }

    public Tool setTextAction(Function<JSONObject, String> action) {
        return doAction(jsonObject -> {
            String textContent = action.apply(jsonObject);
            JSONObject result = new JSONObject();
            result.put("text", textContent);
            result.put("type", "text");
            return result;
        });
    }

    public Tool setImageAction(Function<JSONObject, ImageContent> action) {
        return doAction(jsonObject -> {
            ImageContent content = action.apply(jsonObject);
            JSONObject result = new JSONObject();
            result.put("type", "image");
            result.put("data", content.getData());
            result.put("mimeType", content.getMimeType());
            return result;
        });
    }

    public Tool setAudioAction(Function<JSONObject, AudioContent> action) {
        return doAction(jsonObject -> {
            AudioContent content = action.apply(jsonObject);
            JSONObject result = new JSONObject();
            result.put("type", "audio");
            result.put("data", content.getData());
            result.put("mimeType", content.getMimeType());
            return result;
        });
    }

    public Tool setResourceLinksAction(Function<JSONObject, ResourceLinks> action) {
        return doAction(jsonObject -> {
            ResourceLinks content = action.apply(jsonObject);
            JSONObject result = new JSONObject();
            result.put("type", "resource_link");
            result.put("uri", content.getUri());
            result.put("name", content.getName());
            result.put("description", content.getDescription());
            result.put("mimeType", content.getMimeType());
            return result;
        });
    }

    public Tool setEmbeddedResourcesAction(Function<JSONObject, ResourceLinks> action) {
        return doAction(jsonObject -> {
            ResourceLinks content = action.apply(jsonObject);
            JSONObject result = new JSONObject();
            result.put("type", "resource_link");
            result.put("uri", content.getUri());
            result.put("name", content.getName());
            result.put("description", content.getDescription());
            result.put("mimeType", content.getMimeType());
            return result;
        });
    }

    public Function<JSONObject, JSONObject> getAction() {
        return action;
    }

    public List<Property> outputSchema() {
        return outputSchema;
    }

    public Tool outputSchema(Property... output) {
        if (output == null) {
            return this;
        }
        outputSchema.addAll(Arrays.asList(output));
        return this;
    }


    class ToolResult {
        private String type;

        public ToolResult(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class ImageContent {
        private String data;
        private String mimeType;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
    }

    public static class AudioContent {
        private String data;
        private String mimeType;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
    }

    class ResourceLinks extends ToolResult {
        private String uri;
        private String name;
        private String description;
        private String mimeType;

        public ResourceLinks() {
            super("resource_link");
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
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

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
    }


}

