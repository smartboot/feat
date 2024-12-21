package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.codec.websocket.CloseReason;

/**
 * WebSocket 监听器
 * @author 三刀
 */
public interface WebSocketListener {

    /**
     * 默认方法，当WebSocket连接成功建立时被调用
     *
     * @param client WebSocketClient对象
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
