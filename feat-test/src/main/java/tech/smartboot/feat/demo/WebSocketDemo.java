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

/**
 * @author 三刀
 * @version V1.0 , 2020/4/1
 */
public class WebSocketDemo {
    public static void main(String[] args) {
        Feat.httpServer().httpHandler(request -> {
            request.upgrade(new WebSocketUpgrade() {
                @Override
                public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String message) {
                    response.sendTextMessage("接受到客户端消息：" + message);
                }
            });
        }).listen();
    }
}
