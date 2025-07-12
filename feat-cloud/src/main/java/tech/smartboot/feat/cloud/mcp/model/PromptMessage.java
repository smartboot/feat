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

import tech.smartboot.feat.cloud.mcp.enums.RoleEnum;
import tech.smartboot.feat.cloud.mcp.enums.ToolResultType;
import tech.smartboot.feat.cloud.mcp.server.McpServerException;
import tech.smartboot.feat.core.common.FeatUtils;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class PromptMessage<T extends PromptMessage.PromptContent> {
    private String role;
    private T content;

    PromptMessage(RoleEnum role, T content) {
        this.role = role.getRole();
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public PromptContent getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public static PromptMessage<TextPromptContent> ofText(RoleEnum roleEnum, String text) {
        return new PromptMessage<>(roleEnum, new PromptMessage.TextPromptContent(text));
    }

    public static PromptMessage<PromptMessage.ImagePromptContent> ofImage(RoleEnum roleEnum, String data, String mimeType) {
        return new PromptMessage<>(roleEnum, new PromptMessage.ImagePromptContent(data, mimeType));
    }

    public static PromptMessage<PromptMessage.AudioPromptContent> ofAudio(RoleEnum roleEnum, String data, String mimeType) {
        if (FeatUtils.isBlank(data)) {
            throw new McpServerException(McpServerException.INTERNAL_ERROR, "Audio data cannot be empty");
        }
        return new PromptMessage<>(roleEnum, new PromptMessage.AudioPromptContent(data, mimeType));
    }

    public static PromptMessage<PromptMessage.EmbeddedResourcePromptContent> ofEmbeddedResource(RoleEnum roleEnum, Resource resource) {
        return new PromptMessage<>(roleEnum, new PromptMessage.EmbeddedResourcePromptContent(resource));
    }

    public abstract static class PromptContent {
        private final String type;

        PromptContent(ToolResultType type) {
            this.type = type.getType();
        }

        public String getType() {
            return type;
        }
    }

    public static class ImagePromptContent extends PromptContent {
        private final String data;
        private final String mimeType;

        ImagePromptContent(String data, String mimeType) {
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

        EmbeddedResourcePromptContent(Resource resource) {
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

        AudioPromptContent(String data, String mimeType) {
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

        TextPromptContent(String text) {
            super(ToolResultType.TEXT);
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}

