/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.mcp;

import com.alibaba.fastjson2.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.feat.ai.mcp.McpProtocol;
import tech.smartboot.feat.ai.mcp.enums.RoleEnum;
import tech.smartboot.feat.ai.mcp.model.Tool;
import tech.smartboot.feat.ai.mcp.model.elicitation.ElicitationRequest;
import tech.smartboot.feat.ai.mcp.model.elicitation.ElicitationResult;
import tech.smartboot.feat.ai.mcp.model.elicitation.ElicitationSchema;
import tech.smartboot.feat.ai.mcp.model.elicitation.EnumSchema;
import tech.smartboot.feat.ai.mcp.model.sampling.CreateMessageRequest;
import tech.smartboot.feat.ai.mcp.model.sampling.ToolChoice;
import tech.smartboot.feat.ai.mcp.model.sampling.ToolResultContent;
import tech.smartboot.feat.ai.mcp.model.sampling.ToolUseContent;
import tech.smartboot.feat.ai.mcp.server.model.ServerTool;

/**
 * 2025-11-25 新增协议结构的序列化测试，不依赖服务端。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class McpProtocolModelTest {

    @Test
    public void testProtocolVersionRegistry() {
        Assert.assertEquals("2025-11-25", McpProtocol.LATEST);
        Assert.assertTrue(McpProtocol.isSupported(McpProtocol.V2025_06_18));
        Assert.assertTrue(McpProtocol.isSupported(McpProtocol.V2025_11_25));
        Assert.assertFalse(McpProtocol.isSupported("1999-01-01"));
    }

    /**
     * url 模式只暴露 url 与 elicitationId，不得携带 requestedSchema。
     */
    @Test
    public void testUrlElicitationPayload() {
        ElicitationRequest request = ElicitationRequest.url("请提供 API Key",
                "https://mcp.example.com/ui/set_api_key", "550e8400-e29b-41d4");
        JSONObject json = JSONObject.from(request);
        Assert.assertEquals("url", json.getString("mode"));
        Assert.assertEquals("https://mcp.example.com/ui/set_api_key", json.getString("url"));
        Assert.assertEquals("550e8400-e29b-41d4", json.getString("elicitationId"));
        Assert.assertNull(json.get("requestedSchema"));
        Assert.assertNull(JSONObject.from(ElicitationResult.accept()).get("content"));
    }

    @Test
    public void testFormElicitationPayload() {
        ElicitationSchema schema = ElicitationSchema.of()
                .string("name", "用户名", true, "guest")
                .bool("remember", "是否记住", false, Boolean.TRUE);
        JSONObject json = JSONObject.from(ElicitationRequest.form("请补充信息", schema));
        Assert.assertEquals("form", json.getString("mode"));
        JSONObject requested = json.getJSONObject("requestedSchema");
        Assert.assertEquals("object", requested.getString("type"));
        Assert.assertEquals("guest", requested.getJSONObject("properties")
                .getJSONObject("name").getString("default"));
        Assert.assertEquals(1, requested.getJSONArray("required").size());
        Assert.assertNull(json.get("url"));
    }

    /**
     * 枚举的四种形态：无标题单选、有标题单选、无标题多选、有标题多选。
     */
    @Test
    public void testEnumSchemaShapes() {
        JSONObject single = EnumSchema.single("Red", "Green").toJSON();
        Assert.assertEquals("string", single.getString("type"));
        Assert.assertEquals(2, single.getJSONArray("enum").size());

        JSONObject singleTitled = EnumSchema.singleTitled(
                EnumSchema.Option.of("#FF0000", "Red"), EnumSchema.Option.of("#00FF00", "Green")).toJSON();
        Assert.assertEquals("string", singleTitled.getString("type"));
        Assert.assertEquals("#FF0000", singleTitled.getJSONArray("oneOf").getJSONObject(0).getString("const"));
        Assert.assertEquals("Red", singleTitled.getJSONArray("oneOf").getJSONObject(0).getString("title"));

        JSONObject multi = EnumSchema.multi("Red", "Green", "Blue").minItems(1).maxItems(2).toJSON();
        Assert.assertEquals("array", multi.getString("type"));
        Assert.assertEquals(3, multi.getJSONObject("items").getJSONArray("enum").size());
        Assert.assertEquals(2, multi.getIntValue("maxItems"));

        JSONObject multiTitled = EnumSchema.multiTitled(EnumSchema.Option.of("#FF0000", "Red")).toJSON();
        Assert.assertEquals("array", multiTitled.getString("type"));
        Assert.assertEquals("#FF0000", multiTitled.getJSONObject("items").getJSONArray("anyOf")
                .getJSONObject(0).getString("const"));
    }

    @Test
    public void testEnumDefaultValue() {
        JSONObject schema = EnumSchema.single("Red", "Green").title("颜色").defaultValue("Red").toJSON();
        Assert.assertEquals("Red", schema.getString("default"));
        Assert.assertEquals("颜色", schema.getString("title"));
    }

    @Test
    public void testElicitationModeValidation() {
        try {
            ElicitationRequest.form("msg", null).setMode("bad");
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("form or url"));
        }
    }

    @Test
    public void testSamplingToolPayload() {
        Tool getWeather = ServerTool.of("get_weather")
                .description("Get current weather for a city")
                .inputSchema(Tool.stringProperty("city", "City name"));

        CreateMessageRequest request = CreateMessageRequest.of()
                .message(RoleEnum.User, JSONObject.of("type", "text", "text", "巴黎天气如何"))
                .tools(getWeather)
                .toolChoice(ToolChoice.auto())
                .maxTokens(1000);

        JSONObject json = JSONObject.from(request);
        Assert.assertEquals(1, json.getJSONArray("messages").size());
        Assert.assertEquals("user", json.getJSONArray("messages").getJSONObject(0).getString("role"));
        Assert.assertEquals(1, json.getJSONArray("tools").size());
        Assert.assertEquals("get_weather", json.getJSONArray("tools").getJSONObject(0).getString("name"));
        Assert.assertEquals("auto", json.getJSONObject("toolChoice").getString("mode"));
        Assert.assertEquals(1000, json.getIntValue("maxTokens"));
    }

    @Test
    public void testToolUseAndResultShape() {
        JSONObject use = JSONObject.from(ToolUseContent.of("call_abc", "get_weather",
                JSONObject.of("city", "Paris")));
        Assert.assertEquals("tool_use", use.getString("type"));
        Assert.assertEquals("call_abc", use.getString("id"));
        Assert.assertEquals("Paris", use.getJSONObject("input").getString("city"));

        JSONObject result = JSONObject.from(ToolResultContent.ofText("call_abc", "18°C"));
        Assert.assertEquals("tool_result", result.getString("type"));
        Assert.assertEquals("call_abc", result.getString("toolUseId"));
        Assert.assertEquals("text", result.getJSONArray("content").getJSONObject(0).getString("type"));
    }

    @Test
    public void testToolChoiceValidation() {
        Assert.assertEquals(ToolChoice.MODE_NONE, ToolChoice.none().getMode());
        Assert.assertEquals(ToolChoice.MODE_REQUIRED, ToolChoice.required().getMode());
        try {
            ToolChoice.auto().setMode("sometimes");
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("auto, required or none"));
        }
    }
}
