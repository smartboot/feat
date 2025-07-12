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

import tech.smartboot.feat.cloud.mcp.model.CallToolResult;
import tech.smartboot.feat.cloud.mcp.model.Tool;

import java.util.Arrays;
import java.util.function.Function;

public class ServerTool extends Tool {
    private Function<ToolContext, CallToolResult.Content> action;

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
        getInputs().addAll(Arrays.asList(inputs));
        return this;
    }

    public ServerTool doAction(Function<ToolContext, CallToolResult.Content> action) {
        this.action = action;
        return this;
    }

    public Function<ToolContext, CallToolResult.Content> getAction() {
        return action;
    }


    public ServerTool outputSchema(Property... output) {
        if (output == null) {
            return this;
        }
        outputSchema().addAll(Arrays.asList(output));
        return this;
    }

    public ServerTool title(String title) {
        this.title = title;
        return this;
    }

    public ServerTool description(String description) {
        this.description = description;
        return this;
    }
}

