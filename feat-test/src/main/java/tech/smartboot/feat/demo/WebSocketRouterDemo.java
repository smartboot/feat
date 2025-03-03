/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.upgrade.websocket.WebSocketUpgrade;
import tech.smartboot.feat.router.Router;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/1
 */
public class WebSocketRouterDemo {
    public static void main(String[] args) {
        Router router = new Router();
        router.route("/ws1", ctx -> {
            ctx.Request.upgrade(new WebSocketUpgrade() {
                @Override
                public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String message) {
                    response.sendTextMessage("ws1接受到客户端消息：" + message);
                }
            });
        }).route("/ws2", ctx -> {
            ctx.Request.upgrade(new WebSocketUpgrade() {
                @Override
                public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String message) {
                    response.sendTextMessage("ws2接受到客户端消息：" + message);
                }
            });
        }).route("/http", ctx -> {
            ctx.Request.getResponse().write("http".getBytes());
        });
        Feat.httpServer().httpHandler(router).listen();
    }
}
