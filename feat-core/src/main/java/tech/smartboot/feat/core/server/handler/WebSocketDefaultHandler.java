/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: WebSocketHandle.java
 * Date: 2020-03-31
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.handler;

import tech.smartboot.feat.core.common.codec.websocket.CloseReason;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.utils.WebSocketUtil;
import tech.smartboot.feat.core.server.WebSocketHandler;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.impl.WebSocketRequestImpl;
import tech.smartboot.feat.core.server.impl.WebSocketResponseImpl;
import org.smartboot.socket.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * @author 三刀
 * @version V1.0 , 2020/3/31
 */
public class WebSocketDefaultHandler extends WebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketDefaultHandler.class);

    @Override
    public void whenHeaderComplete(WebSocketRequestImpl request, WebSocketResponseImpl response) {
        onHandShake(request, response);
    }

    @Override
    public final void handle(WebSocketRequest request, WebSocketResponse response) throws IOException {
        try {
            switch (request.getFrameOpcode()) {
                case WebSocketUtil.OPCODE_TEXT:
                    handleTextMessage(request, response, new String(request.getPayload(), StandardCharsets.UTF_8));
                    break;
                case WebSocketUtil.OPCODE_BINARY:
                    handleBinaryMessage(request, response, request.getPayload());
                    break;
                case WebSocketUtil.OPCODE_CLOSE:
                    try {
                        onClose(request, response, new CloseReason(request.getPayload()));
                    } finally {
                        response.close();
                    }
                    break;
                case WebSocketUtil.OPCODE_PING:
                    handlePing(request, response);
                    break;
                case WebSocketUtil.OPCODE_PONG:
                    handlePong(request, response);
                    break;
                case WebSocketUtil.OPCODE_CONTINUE:
                    LOGGER.warn("unSupport OPCODE_CONTINUE now,ignore payload: {}", StringUtils.toHexString(request.getPayload()));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        } catch (Throwable throwable) {
            onError(request, throwable);
            throw throwable;
        }
    }

    public void handlePing(WebSocketRequest request, WebSocketResponse response) {
        response.pong(request.getPayload());
    }

    public void handlePong(WebSocketRequest request, WebSocketResponse response) {
        LOGGER.warn("receive pong...");
    }

    /**
     * 握手成功
     *
     * @param request
     * @param response
     */
    public void onHandShake(WebSocketRequest request, WebSocketResponse response) {
        LOGGER.warn("handShake success");
    }

    /**
     * 连接关闭
     *
     * @param request
     * @param response
     */
    public void onClose(WebSocketRequest request, WebSocketResponse response, CloseReason closeReason) {
        LOGGER.warn("close connection");
    }

    /**
     * 处理字符串请求消息
     *
     * @param request
     * @param response
     * @param data
     */
    public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String data) {
        System.out.println(data);
    }

    /**
     * 处理二进制请求消息
     *
     * @param request
     * @param response
     * @param data
     */
    public void handleBinaryMessage(WebSocketRequest request, WebSocketResponse response, byte[] data) {
        System.out.println(data);
    }

    /**
     * 连接异常
     *
     * @param request
     * @param throwable
     */
    public void onError(WebSocketRequest request, Throwable throwable) {
        throwable.printStackTrace();
    }
}
