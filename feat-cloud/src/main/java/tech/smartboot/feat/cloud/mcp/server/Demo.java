/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.server;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.mcp.enums.RoleEnum;
import tech.smartboot.feat.cloud.mcp.server.model.Argument;
import tech.smartboot.feat.cloud.mcp.server.model.Prompt;
import tech.smartboot.feat.cloud.mcp.server.model.PromptResult;
import tech.smartboot.feat.cloud.mcp.server.model.Property;
import tech.smartboot.feat.cloud.mcp.server.model.Resource;
import tech.smartboot.feat.cloud.mcp.server.model.Tool;
import tech.smartboot.feat.cloud.mcp.server.model.ToolResult;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class Demo {
    public static void main(String[] args) {
        Tool tool = Tool.of("test").title("测试").description("测试").inputSchema(Property.withString("name", "用户名称"), Property.withRequiredString("age", "用户年龄")).outputSchema(Property.withRequiredNumber("age", "年龄")).doAction(input -> {
            return ToolResult.ofText("aaa");
        });

        Tool structTool = Tool.of("structResultTool").inputSchema(Property.withString("aa", "aa")).doAction(toolContext -> {
            JSONObject j = new JSONObject();
            j.put("name", "name");
            j.put("age", 18);
            j.put("text", toolContext.getArguments().get("aa"));
            j.put("resource", Resource.of("test", "test.txt"));
            return ToolResult.ofStructuredContent(j);
        });

        McpServerHandler handler = new McpServerHandler();
        McpServer mcp = handler.getMcp();
        mcp.addTool(tool).addTool(Tool.of("errorTool").inputSchema(Property.withString("aa", "aa")).doAction(jsonObject -> {
            throw new IllegalStateException("exception...");
        })).addTool(structTool);

        //prompt
        mcp.addPrompt(Prompt.of("code_review")
                        .title("Request Code Review")
                        .description("Asks the LLM to analyze code quality and suggest improvements")
                        .arguments(Argument.requiredOf("code", "The code to review"))
                        .doAction(promptContext -> {
                            String code = promptContext.getArguments().getString("code");
                            return PromptResult.ofText(RoleEnum.User, "Please review the following code and provide suggestions for improvement:" + code);
                        }))
                .addPrompt(Prompt.of("image_review")
                        .title("Request Image Review")
                        .description("Asks the LLM to analyze image quality and suggest improvements")
                        .arguments(Argument.requiredOf("image", "The image to review"))
                        .doAction(promptContext -> {
                            String image = promptContext.getArguments().getString("image");
                            return PromptResult.ofImage(RoleEnum.User, image, "image/png");
                        }))
                .addPrompt(Prompt.of("audio_review")
                        .title("Request Audio Review")
                        .description("Asks the LLM to analyze audio quality and suggest improvements")
                        .arguments(Argument.requiredOf("audio", "The audio to review"))
                        .doAction(promptContext -> {
                            String image = promptContext.getArguments().getString("audio");
                            return PromptResult.ofAudio(RoleEnum.User, "YXNkZmFzZGY=", "audio/wav");
                        }))
                .addPrompt(Prompt.of("embedded_resource_review")
                        .title("Request Embedded Resource Review")
                        .description("Asks the LLM to analyze embedded resource quality and suggest improvements")
                        .doAction(promptContext -> {
                            Resource.TextResource resource = Resource.ofText("test", "test.txt", "Hello World");
                            return PromptResult.ofEmbeddedResource(RoleEnum.Assistant, resource);
                        }));

        // resources
        mcp.addResource(Resource.of("test", "test.txt").doAction(resourceContext -> {
            return Resource.ofText(resourceContext.getResource(), "contentcontentcontentcontentcontent");
        }));
        mcp.addResource(Resource.of("bbbbb", "test.bin").doAction(resourceContext -> {
            return Resource.ofBinary(resourceContext.getResource(), "YXNkZmFzZGY=");
        }));

        Feat.httpServer(opt -> opt.debug(true)).httpHandler(handler).listen(3002);
    }
}
