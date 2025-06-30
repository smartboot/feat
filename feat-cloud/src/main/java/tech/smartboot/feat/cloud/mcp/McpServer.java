/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp;

import tech.smartboot.feat.cloud.mcp.model.Prompt;
import tech.smartboot.feat.cloud.mcp.model.Resource;
import tech.smartboot.feat.cloud.mcp.model.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 6/28/25
 */
public class McpServer {
    private final List<Tool> tools = new ArrayList<>();
    private final List<Prompt> prompts = new ArrayList<>();
    private final List<Resource> resources = new ArrayList<>();

    public List<Tool> getTools() {
        return tools;
    }

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void addTool(Consumer<Tool> tool) {
        Tool t = new Tool();
        tool.accept(t);
        tools.add(t);
    }
}
