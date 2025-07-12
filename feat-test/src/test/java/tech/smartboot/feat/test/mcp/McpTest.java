/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.mcp;

import com.alibaba.fastjson2.JSONObject;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.mcp.Argument;
import tech.smartboot.feat.cloud.mcp.CallToolResult;
import tech.smartboot.feat.cloud.mcp.McpInitializeResponse;
import tech.smartboot.feat.cloud.mcp.client.ClientCapabilities;
import tech.smartboot.feat.cloud.mcp.client.McpClient;
import tech.smartboot.feat.cloud.mcp.enums.RoleEnum;
import tech.smartboot.feat.cloud.mcp.server.McpServer;
import tech.smartboot.feat.cloud.mcp.server.model.PromptResult;
import tech.smartboot.feat.cloud.mcp.server.model.Property;
import tech.smartboot.feat.cloud.mcp.server.model.ResourceTemplate;
import tech.smartboot.feat.cloud.mcp.server.model.ServerPrompt;
import tech.smartboot.feat.cloud.mcp.server.model.ServerResource;
import tech.smartboot.feat.cloud.mcp.server.model.Tool;
import tech.smartboot.feat.router.Router;

/**
 * @author 三刀
 * @version v1.0 7/9/25
 */
public class McpTest {
    private McpServer mcp;
    private McpClient sseClient;
    private McpClient streamClient;

    @Before
    public void init() {
        Tool tool = Tool.of("test").title("测试").description("测试").inputSchema(Property.withString("name", "用户名称"), Property.withRequiredString("age", "用户年龄")).outputSchema(Property.withRequiredNumber("age", "年龄")).doAction(input -> {
            return CallToolResult.ofText("aaa");
        });

        Tool structTool = Tool.of("structResultTool").inputSchema(Property.withString("aa", "aa")).doAction(toolContext -> {
            JSONObject j = new JSONObject();
            j.put("name", "name");
            j.put("age", 18);
            j.put("text", toolContext.getArguments().get("aa"));
            j.put("resource", ServerResource.of("test", "test.txt"));
            return CallToolResult.ofStructuredContent(j);
        });

        mcp = new McpServer();
        mcp.addTool(tool).addTool(Tool.of("errorTool").inputSchema(Property.withString("aa", "aa")).doAction(jsonObject -> {
            throw new IllegalStateException("exception...");
        })).addTool(structTool);

        //prompt
        mcp.addPrompt(ServerPrompt.of("code_review")
                        .title("Request Code Review")
                        .description("Asks the LLM to analyze code quality and suggest improvements")
                        .arguments(Argument.requiredOf("code", "The code to review"))
                        .doAction(promptContext -> {
                            String code = promptContext.getArguments().getString("code");
                            return PromptResult.ofText(RoleEnum.User, "Please review the following code and provide suggestions for improvement:" + code);
                        }))
                .addPrompt(ServerPrompt.of("image_review")
                        .title("Request Image Review")
                        .description("Asks the LLM to analyze image quality and suggest improvements")
                        .arguments(Argument.requiredOf("image", "The image to review"))
                        .doAction(promptContext -> {
                            String image = promptContext.getArguments().getString("image");
                            return PromptResult.ofImage(RoleEnum.User, image, "image/png");
                        }))
                .addPrompt(ServerPrompt.of("audio_review")
                        .title("Request Audio Review")
                        .description("Asks the LLM to analyze audio quality and suggest improvements")
                        .arguments(Argument.requiredOf("audio", "The audio to review"))
                        .doAction(promptContext -> {
                            String image = promptContext.getArguments().getString("audio");
                            return PromptResult.ofAudio(RoleEnum.User, "YXNkZmFzZGY=", "audio/wav");
                        }))
                .addPrompt(ServerPrompt.of("embedded_resource_review")
                        .title("Request Embedded Resource Review")
                        .description("Asks the LLM to analyze embedded resource quality and suggest improvements")
                        .doAction(promptContext -> {
                            ServerResource.TextServerResource resource = ServerResource.ofText("test", "test.txt", "Hello World");
                            return PromptResult.ofEmbeddedResource(RoleEnum.Assistant, resource);
                        }));

        // resources
        mcp.addResource(ServerResource.of("test", "test.txt").doAction(resourceContext -> {
                    return ServerResource.ofText(resourceContext.getResource(), "contentcontentcontentcontentcontent");
                })).addResource(ServerResource.of("bbbbb", "test.bin").doAction(resourceContext -> {
                    return ServerResource.ofBinary(resourceContext.getResource(), "YXNkZmFzZGY=");
                })).addResource(ServerResource.ofText("file:///aa.txt", "test.txt", "text/plain", "bbbbb"))
                .addResource(ServerResource.ofText("file:///bb.txt", "bb.txt", "text/plain", "aaaaaaa"));

        // resourceTemplate
        mcp.addResourceTemplate(ResourceTemplate.of("file:///{path}", "testTemplate").title("\uD83D\uDCC1 Project Files").description("Access files in the project directory").mimeType("application/octet-stream"));

        Router router = new Router();
        router.route(mcp.getOptions().getSseEndpoint(), mcp.sseHandler());
        router.route(mcp.getOptions().getSseMessageEndpoint(), mcp.sseMessageHandler());
        router.route(mcp.getOptions().getMcpEndpoint(), mcp.mcpHandler());
        Feat.httpServer(opt -> opt.debug(true)).httpHandler(router).listen(3002);

        sseClient = McpClient.newSseClient(opt -> opt.baseUrl("http://localhost:3002").setMcpEndpoint("/mcp"));
        sseClient.initialize(new ClientCapabilities());
        streamClient = McpClient.newStreamableClient(opt -> opt.baseUrl("http://localhost:3002").setMcpEndpoint("/mcp"));
        streamClient.initialize(new ClientCapabilities());
    }

    @Test
    public void test() throws Exception {
        McpClient client = McpClient.newSseClient(opt -> opt.baseUrl("http://localhost:3002").setMcpEndpoint("/mcp"));
        McpInitializeResponse response = client.initialize(new ClientCapabilities());
        System.out.println(JSONObject.toJSONString(response));
        System.out.println(JSONObject.toJSONString(client.listTools()));
        System.out.println(JSONObject.toJSONString(client.listPrompts()));
        System.out.println(JSONObject.toJSONString(client.listResources()));


    }

    @Test
    public void test2() throws Exception {
        mcp.addPrompt(ServerPrompt.of("code_review_1")
                .title("Request Code Review")
                .description("Asks the LLM to analyze code quality and suggest improvements")
                .arguments(Argument.requiredOf("code", "The code to review"))
                .doAction(promptContext -> {
                    String code = promptContext.getArguments().getString("code");
                    return PromptResult.ofText(RoleEnum.User, "Please review the following code and provide suggestions for improvement:" + code);
                }));
        System.out.println(JSONObject.toJSONString(sseClient.getPrompt("code_review_1")));
    }

    @Test
    public void test3() throws Exception {
        mcp.addTool(Tool.of("test_aaa").title("测试").description("测试").inputSchema(Property.withString("name", "用户名称"), Property.withRequiredString("age", "用户年龄")).outputSchema(Property.withRequiredNumber("age", "年龄")).doAction(input -> {
            return CallToolResult.ofText("aaa");
        }));
        System.out.println(JSONObject.toJSONString(sseClient.callTool("test_aaa")));
        System.out.println(JSONObject.toJSONString(streamClient.callTool("test_aaa")));
    }
}
