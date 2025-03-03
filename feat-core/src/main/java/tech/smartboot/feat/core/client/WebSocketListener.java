/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.codec.websocket.CloseReason;

/**
 * WebSocket 监听器
 *
 * @author 三刀
 */
public interface WebSocketListener {

    /**
     * 默认方法，当WebSocket连接成功建立时被调用
     *
     * @param client   WebSocketClient对象
     * @param response WebSocketResponse对象
     */
    default void onOpen(WebSocketClient client, WebSocketResponse response) {
        System.out.println("连接已打开");
    }


    default void onClose(WebSocketClient client, WebSocketResponse response, CloseReason reason) {
        System.out.println("连接已关闭");
    }

    //
    default void onError(WebSocketClient client, WebSocketResponse response, Throwable throwable) {
        System.out.println("发生错误： " + throwable.getMessage());
    }

    default void onMessage(WebSocketClient client, String message) {
        System.out.println("收到消息： " + message);
    }

    default void onMessage(WebSocketClient client, byte[] message) {
        System.out.println("收到消息： " + message);
    }
}
