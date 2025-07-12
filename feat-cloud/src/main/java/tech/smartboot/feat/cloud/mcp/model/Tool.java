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

import tech.smartboot.feat.cloud.mcp.server.model.Property;

import java.util.ArrayList;
import java.util.List;

public class Tool {
    /**
     * The name of the tool. Must be unique.
     */
    private final String name;
    /**
     * Optional human-readable name of the tool for display purposes.
     */
    protected String title;
    /**
     * Human-readable description of functionality
     */
    protected String description;
    private final List<Property> inputSchema = new ArrayList<>();
    private final List<Property> outputSchema = new ArrayList<>();

    protected Tool(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<Property> getInputs() {
        return inputSchema;
    }

    public final List<Property> outputSchema() {
        return outputSchema;
    }
}

