/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.server.handler;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.core.server.HttpRequest;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/5/25
 */
public class LoggingSetLevelHandler implements ServerHandler {
    @Override
    public JSONObject handle(McpServer mcp, HttpRequest request, JSONObject jsonObject) {
        JSONObject params = jsonObject.getJSONObject("params");
        String level = params.getString("level");

        System.out.println("set log level to " + level);
//        PromptContext promptContext = new PromptContext(request, promptParams);
//
//        Prompt prompt = mcp.getPrompts().stream().filter(t -> t.getName().equals(promptName)).findFirst().orElse(null);
//        if (prompt == null) {
//            throw new McpServerException(McpServerException.INTERNAL_ERROR, "Unknown prompt: " + promptName);
//        }

        return new JSONObject();
    }
}
