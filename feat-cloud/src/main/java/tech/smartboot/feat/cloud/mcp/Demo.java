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

import tech.smartboot.feat.Feat;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class Demo {
    public static void main(String[] args) {
        McpServerHandler handler = new McpServerHandler();
        handler.getMcp().addTool(tool -> {
            tool.setName("test");
            tool.setTitle("测试");
            tool.setDescription("测试");
            tool.setInputs(opt -> {
                opt.withString("name", "用户名称").required();
            }, opt -> {
                opt.withString("age", "用户年龄").required();
            }).setAction(jsonObject -> {
                return jsonObject;
            });
        });
        Feat.httpServer(opt -> opt.debug(true)).httpHandler(handler).listen(3002);
    }
}
