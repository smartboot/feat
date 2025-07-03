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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.mcp.model.Property;
import tech.smartboot.feat.cloud.mcp.model.Resource;
import tech.smartboot.feat.cloud.mcp.model.Tool;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class Demo {
    public static void main(String[] args) {
        Tool tool = Tool.of("test").title("测试").description("测试")
                .inputSchema(Property.withString("name", "用户名称"), Property.withRequiredString("age", "用户年龄"))
                .outputSchema(Property.withRequiredNumber("age", "年龄"))
                .doAction(input -> {
                    JSONObject object = new JSONObject();
                    return object;
                });
        McpServerHandler handler = new McpServerHandler();
        handler.getMcp().addTool(tool).addTool(Tool.of("textResult").inputSchema(Property.withString("aa", "aa")).setTextAction(jsonObject -> "Hello World:" + jsonObject.getString("aa")));

        handler.getMcp().addResource(Resource.of("test", "test.txt")).addResource(Resource.ofText("test2", "test2.txt").setText("Hello World")).addResource(Resource.ofBinary("test3", "test3.txt").setBlob("text/plain", "Hello World"));

        Feat.httpServer(opt -> opt.debug(true)).httpHandler(handler).listen(3002);
    }
}
