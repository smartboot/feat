/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: WebSocketDemo.java
 * Date: 2021-06-20
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

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
        router.route("/ws1", request -> {
            request.upgrade(new WebSocketUpgrade() {
                @Override
                public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String message) {
                    response.sendTextMessage("ws1接受到客户端消息：" + message);
                }
            });
        }).route("/ws2", request -> {
            request.upgrade(new WebSocketUpgrade() {
                @Override
                public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String message) {
                    response.sendTextMessage("ws2接受到客户端消息：" + message);
                }
            });
        }).route("/http", request -> {
            request.getResponse().write("http".getBytes());
        });
        Feat.httpServer().httpHandler(router).listen();
    }
}
