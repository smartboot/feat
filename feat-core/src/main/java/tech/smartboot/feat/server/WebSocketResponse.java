/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: WebSocketResponse.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server;

import tech.smartboot.feat.common.utils.Constant;

/**
 * WebSocket消息响应接口
 *
 * @author 三刀
 * @version V1.0 , 2020/3/31
 */
public interface WebSocketResponse {
    /**
     * 发送文本响应
     *
     * @param text
     */
    void sendTextMessage(String text);

    /**
     * 发送二进制响应
     *
     * @param bytes
     */
    void sendBinaryMessage(byte[] bytes);

    /**
     * 发送二进制响应
     *
     * @param bytes
     */
    void sendBinaryMessage(byte[] bytes, int offset, int length);

    default void pong() {
        pong(Constant.EMPTY_BYTES);
    }


    void pong(byte[] bytes);

    void ping(byte[] bytes);

    default void ping() {
        ping(Constant.EMPTY_BYTES);
    }

    /**
     * 关闭ws通道
     */
    void close();

    void close(int code, String reason);

    /**
     * 输出数据
     */
    void flush();
}
