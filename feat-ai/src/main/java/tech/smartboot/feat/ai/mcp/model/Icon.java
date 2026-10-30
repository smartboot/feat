/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 服务端对外暴露的图标元数据（2025-11-25 起支持）。
 * <p>
 * 注意：{@code src} 只接受 https 或 data: URI，消费方应拒绝 javascript:、file: 等不安全协议。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class Icon {
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    /**
     * 图标地址，必填，https URL 或 data: URI。
     */
    private final String src;
    /**
     * 图标 MIME 类型，可选。
     */
    private String mimeType;
    /**
     * 尺寸列表，例如 48x48，矢量图可用 any。
     */
    private List<String> sizes;
    /**
     * 主题偏好：light 或 dark。
     */
    private String theme;

    private Icon(String src) {
        this.src = src;
    }

    public static Icon of(String src) {
        if (src == null || src.trim().isEmpty()) {
            throw new IllegalArgumentException("icon src is required");
        }
        return new Icon(src);
    }

    public String getSrc() {
        return src;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Icon mimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public List<String> getSizes() {
        return sizes;
    }

    public Icon sizes(String... sizes) {
        this.sizes = sizes == null ? Collections.<String>emptyList() : Arrays.asList(sizes);
        return this;
    }

    public String getTheme() {
        return theme;
    }

    public Icon theme(String theme) {
        if (!THEME_LIGHT.equals(theme) && !THEME_DARK.equals(theme)) {
            throw new IllegalArgumentException("icon theme must be light or dark: " + theme);
        }
        this.theme = theme;
        return this;
    }
}
