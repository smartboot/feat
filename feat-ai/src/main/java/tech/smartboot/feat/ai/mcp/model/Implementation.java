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

public class Implementation {
    private String name;
    private String title;
    private String version;
    /**
     * 人类可读的实现说明，2025-11-25 起为可选字段。
     */
    private String description;
    private List<Icon> icons;

    private Implementation(String name, String title, String version) {
        this.name = name;
        this.title = title;
        this.version = version;
    }

    public static Implementation of(String name, String title, String version) {
        return new Implementation(name, title, version);
    }

    public String getTitle() {
        return title;
    }

    public Implementation setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getName() {
        return name;
    }

    public Implementation setName(String name) {
        this.name = name;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Implementation setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Implementation description(String description) {
        this.description = description;
        return this;
    }

    public List<Icon> getIcons() {
        return icons;
    }

    public Implementation icons(Icon... icons) {
        this.icons = icons == null ? Collections.<Icon>emptyList() : Arrays.asList(icons);
        return this;
    }
}