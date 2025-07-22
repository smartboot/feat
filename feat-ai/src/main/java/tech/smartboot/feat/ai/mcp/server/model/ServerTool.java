/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.server.model;

import tech.smartboot.feat.ai.mcp.model.Tool;
import tech.smartboot.feat.ai.mcp.model.ToolResult;

import java.util.HashMap;
import java.util.function.Function;

public class ServerTool extends Tool {
    private Function<ToolContext, ToolResult> action;

    private ServerTool(String name) {
        super(name);
    }

    public static ServerTool of(String name) {
        return new ServerTool(name);
    }

    public final ServerTool inputSchema(Property... inputs) {
        if (inputs == null) {
            return this;
        }
        if (inputSchema == null) {
            inputSchema = new Schema();
            inputSchema.setType("object");
            inputSchema.setProperties(new HashMap<>());
        }
        for (Property input : inputs) {
            inputSchema.getProperties().put(input.getName(), input);
        }
        return this;
    }

    public ServerTool doAction(Function<ToolContext, ToolResult> action) {
        this.action = action;
        return this;
    }

    public Function<ToolContext, ToolResult> getAction() {
        return action;
    }


    public ServerTool outputSchema(Property... outputs) {
        if (outputs == null) {
            return this;
        }
        if (outputSchema == null) {
            outputSchema = new Schema();
            outputSchema.setType("object");
            outputSchema.setProperties(new HashMap<>());
        }
        for (Property output : outputs) {
            outputSchema.getProperties().put(output.getName(), output);
        }
        return this;
    }

    public ServerTool title(String title) {
        super.setTitle(title);
        return this;
    }

    public ServerTool description(String description) {
        super.setDescription(description);
        return this;
    }
}

