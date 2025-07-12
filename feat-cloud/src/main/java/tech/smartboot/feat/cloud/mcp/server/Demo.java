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
import tech.smartboot.feat.cloud.mcp.model.Tool;
import tech.smartboot.feat.cloud.mcp.enums.RoleEnum;
import tech.smartboot.feat.cloud.mcp.model.ToolCalledResult;
import tech.smartboot.feat.cloud.mcp.model.Prompt;
import tech.smartboot.feat.cloud.mcp.model.PromptMessage;
import tech.smartboot.feat.cloud.mcp.model.Resource;
import tech.smartboot.feat.cloud.mcp.model.ResourceTemplate;
import tech.smartboot.feat.cloud.mcp.server.model.ServerPrompt;
import tech.smartboot.feat.cloud.mcp.server.model.ServerResource;
import tech.smartboot.feat.cloud.mcp.server.model.ServerTool;
import tech.smartboot.feat.router.Router;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class Demo {
    public static void main(String[] args) {
        ServerTool tool = ServerTool.of("test").title("测试").description("测试").inputSchema(Tool.stringProperty("name", "用户名称"), Tool.requiredStringProperty("age", "用户年龄")).outputSchema(Tool.requiredNumberProperty("age", "年龄")).doAction(input -> {
            return ToolCalledResult.ofText("aaa");
        });

        ServerTool structTool = ServerTool.of("structResultTool").inputSchema(Tool.stringProperty("aa", "aa")).doAction(toolContext -> {
            JSONObject j = new JSONObject();
            j.put("name", "name");
            j.put("age", 18);
            j.put("text", toolContext.getArguments().get("aa"));
            j.put("resource", Resource.of("test", "test.txt"));
            return ToolCalledResult.ofStructuredContent(j);
        });

        McpServer mcp = new McpServer();
        mcp.addTool(tool).addTool(ServerTool.of("errorTool").inputSchema(Tool.stringProperty("aa", "aa")).doAction(jsonObject -> {
            throw new IllegalStateException("exception...");
        })).addTool(structTool);

        //prompt
        mcp.addPrompt(ServerPrompt.of("code_review")
                        .title("Request Code Review")
                        .description("Asks the LLM to analyze code quality and suggest improvements")
                        .arguments(Prompt.requiredArgument("code", "The code to review"))
                        .doAction(promptContext -> {
                            String code = promptContext.getArguments().getString("code");
                            return PromptMessage.ofText(RoleEnum.User, "Please review the following code and provide suggestions for improvement:" + code);
                        }))
                .addPrompt(ServerPrompt.of("image_review")
                        .title("Request Image Review")
                        .description("Asks the LLM to analyze image quality and suggest improvements")
                        .arguments(Prompt.requiredArgument("image", "The image to review"))
                        .doAction(promptContext -> {
                            String image = promptContext.getArguments().getString("image");
                            return PromptMessage.ofImage(RoleEnum.User, image, "image/png");
                        }))
                .addPrompt(ServerPrompt.of("audio_review")
                        .title("Request Audio Review")
                        .description("Asks the LLM to analyze audio quality and suggest improvements")
                        .arguments(Prompt.requiredArgument("audio", "The audio to review"))
                        .doAction(promptContext -> {
                            String image = promptContext.getArguments().getString("audio");
                            return PromptMessage.ofAudio(RoleEnum.User, "YXNkZmFzZGY=", "audio/wav");
                        }))
                .addPrompt(ServerPrompt.of("embedded_resource_review")
                        .title("Request Embedded Resource Review")
                        .description("Asks the LLM to analyze embedded resource quality and suggest improvements")
                        .doAction(promptContext -> {
                            Resource resource = Resource.of("test", "test.txt", "text/plain");
                            resource.setText("context");
                            return PromptMessage.ofEmbeddedResource(RoleEnum.Assistant, resource);
                        }));

        // resources
        mcp.addResource(ServerResource.ofText("test", "test.txt", "contentcontentcontentcontentcontent"))
                .addResource(ServerResource.ofBinary("bbbbb", "test.bin").doAction(resourceContext -> {
                    return "adfasfasdf";
                })).addResource(ServerResource.ofText("file:///aa.txt", "test.txt", "text/plain", "bbbbb"))
                .addResource(ServerResource.ofText("file:///bb.txt", "bb.txt", "text/plain", "aaaaaaa"));

        // resourceTemplate
        mcp.addResourceTemplate(ResourceTemplate.of("file:///{path}", "testTemplate").title("\uD83D\uDCC1 Project Files").description("Access files in the project directory").mimeType("application/octet-stream"));

        Router router = new Router();
        router.route(mcp.getOptions().getSseEndpoint(), mcp.sseHandler());
        router.route(mcp.getOptions().getSseMessageEndpoint(), mcp.sseMessageHandler());
        router.route(mcp.getOptions().getMcpEndpoint(), mcp.mcpHandler());
        Feat.httpServer(opt -> opt.debug(true)).httpHandler(router).listen(3002);
    }
}
