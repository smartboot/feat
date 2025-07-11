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
    private final List<Property> outputSchema = new ArrayList<>();
    private Function<ToolContext, ToolResultContext> action;

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

    public Tool doAction(Function<ToolContext, ToolResultContext> action) {
        this.action = action;
        return this;
    }

    public Function<ToolContext, ToolResultContext> getAction() {
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


}

