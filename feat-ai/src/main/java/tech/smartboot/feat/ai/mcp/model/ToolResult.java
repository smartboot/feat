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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.enums.ToolResultType;

public abstract class ToolResult {
    private final String type;

    public ToolResult(ToolResultType type) {
        this.type = type.getType();
    }

    public String getType() {
        return type;
    }

    public static TextContent ofText(String text) {
        TextContent content = new TextContent();
        content.setText(text);
        return content;
    }


    public static ImageContent ofImage(String data, String mimeType) {
        ImageContent content = new ImageContent();
        content.setData(data);
        content.setMimeType(mimeType);
        return content;
    }

    public static StructuredContent ofStructuredContent(JSONObject content) {
        return new StructuredContent(content);
    }

    public static class TextContent extends ToolResult {
        private String text;

        TextContent() {
            super(ToolResultType.TEXT);
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class ResourceLinks extends ToolResult {
        private String uri;
        private String name;
        private String description;
        private String mimeType;

        public ResourceLinks() {
            super(ToolResultType.RESOURCE_LINK);
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

    public static class ImageContent extends ToolResult {
        private String data;
        private String mimeType;

        public ImageContent() {
            super(ToolResultType.IMAGE);
        }

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

    public static class AudioContent extends ToolResult {
        private String data;
        private String mimeType;

        public AudioContent() {
            super(ToolResultType.AUDIO);
        }

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

    public static class StructuredContent extends ToolResult {
        private final JSONObject content;

        public StructuredContent(JSONObject content) {
            super(ToolResultType.STRUCTURED_CONTENT);
            this.content = content;
        }

        public JSONObject getContent() {
            return content;
        }
    }
}