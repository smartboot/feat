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

import com.alibaba.fastjson2.JSONObject;

public class ServerCapabilities {
    private JSONObject prompts;
    private JSONObject resources;
    private JSONObject tools;
    private JSONObject logging;
    private JSONObject completions;
    private JSONObject experimental;

    public JSONObject getPrompts() {
        return prompts;
    }

    public void setPrompts(JSONObject prompts) {
        this.prompts = prompts;
    }

    public JSONObject getResources() {
        return resources;
    }

    public void setResources(JSONObject resources) {
        this.resources = resources;
    }

    public JSONObject getTools() {
        return tools;
    }

    public void setTools(JSONObject tools) {
        this.tools = tools;
    }

    public JSONObject getLogging() {
        return logging;
    }

    public void setLogging(JSONObject logging) {
        this.logging = logging;
    }

    public JSONObject getCompletions() {
        return completions;
    }

    public void setCompletions(JSONObject completions) {
        this.completions = completions;
    }

    public JSONObject getExperimental() {
        return experimental;
    }

    public void setExperimental(JSONObject experimental) {
        this.experimental = experimental;
    }
}