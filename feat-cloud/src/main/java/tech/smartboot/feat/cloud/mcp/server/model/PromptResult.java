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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.cloud.mcp.Resource;
import tech.smartboot.feat.cloud.mcp.enums.RoleEnum;
import tech.smartboot.feat.cloud.mcp.enums.ToolResultType;
import tech.smartboot.feat.cloud.mcp.server.McpServerException;
import tech.smartboot.feat.core.common.FeatUtils;

public final class PromptResult {
    private final String role;

    private final JSONObject content;

    PromptResult(RoleEnum role, JSONObject content) {
        this.role = role.getRole();
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public JSONObject getContent() {
        return content;
    }

    public static PromptResult ofText(RoleEnum roleEnum, String text) {
        return new PromptResult(roleEnum, JSONObject.from(new TextPromptContent(text)));
    }

    public static PromptResult ofImage(RoleEnum roleEnum, String data, String mimeType) {
        return new PromptResult(roleEnum, JSONObject.from(new ImagePromptContent(data, mimeType)));
    }

    public static PromptResult ofAudio(RoleEnum roleEnum, String data, String mimeType) {
        if (FeatUtils.isBlank(data)) {
            throw new McpServerException(McpServerException.INTERNAL_ERROR, "Audio data cannot be empty");
        }
        return new PromptResult(roleEnum, JSONObject.from(new AudioPromptContent(data, mimeType)));
    }

    public static PromptResult ofEmbeddedResource(RoleEnum roleEnum, Resource resource) {
        return new PromptResult(roleEnum, JSONObject.from(new EmbeddedResourcePromptContent(resource)));
    }


    public abstract static class PromptContent {
        private final String type;

        public PromptContent(ToolResultType type) {
            this.type = type.getType();
        }

        public String getType() {
            return type;
        }
    }

    public static class ImagePromptContent extends PromptContent {
        private final String data;
        private final String mimeType;

        public ImagePromptContent(String data, String mimeType) {
            super(ToolResultType.IMAGE);
            this.mimeType = mimeType;
            this.data = data;
        }

        public String getData() {
            return data;
        }


        public String getMimeType() {
            return mimeType;
        }

    }

    public static class EmbeddedResourcePromptContent extends PromptContent {
        private final Resource resource;

        public EmbeddedResourcePromptContent(Resource resource) {
            super(ToolResultType.EMBEDDED_RESOURCE);
            this.resource = resource;
        }

        public Resource getResource() {
            return resource;
        }
    }

    public static class AudioPromptContent extends PromptContent {
        private final String data;
        private final String mimeType;

        public AudioPromptContent(String data, String mimeType) {
            super(ToolResultType.AUDIO);
            this.mimeType = mimeType;
            this.data = data;
        }

        public String getData() {
            return data;
        }


        public String getMimeType() {
            return mimeType;
        }

    }

    public static class TextPromptContent extends PromptContent {
        private final String text;

        public TextPromptContent(String text) {
            super(ToolResultType.TEXT);
            this.text = text;
        }


        public String getText() {
            return text;
        }
    }
}