/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: IMDemo.java
 * Date: 2021-06-20
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import tech.smartboot.feat.core.common.codec.websocket.CloseReason;
import tech.smartboot.feat.core.server.HttpBootstrap;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServerHandler;
import tech.smartboot.feat.core.server.WebSocketRequest;
import tech.smartboot.feat.core.server.WebSocketResponse;
import tech.smartboot.feat.core.server.handler.HttpRouteHandler;
import tech.smartboot.feat.core.server.handler.WebSocketDefaultHandler;
import tech.smartboot.feat.core.server.handler.WebSocketRouteHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2020/5/10
 */
public class IMDemo {
    public static void main(String[] args) {
        HttpRouteHandler routeHandle = new HttpRouteHandler();
        routeHandle.route("/", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                OutputStream writeBuffer = response.getOutputStream();
                InputStream inputStream = IMDemo.class.getClassLoader().getResourceAsStream("im.html");
                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = inputStream.read(bytes)) != -1) {
                    writeBuffer.write(bytes, 0, length);
                }
            }
        });

        WebSocketRouteHandler webSocketRouteHandle = new WebSocketRouteHandler();
        webSocketRouteHandle.route("/", new WebSocketDefaultHandler() {
            private Map<WebSocketRequest, WebSocketResponse> sessionMap = new ConcurrentHashMap<>();

            @Override
            public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String data) {
                JSONObject jsonObject = JSON.parseObject(data);
                jsonObject.put("sendTime", System.currentTimeMillis());
                jsonObject.put("id", UUID.randomUUID().toString());
                jsonObject.put("from", request.hashCode());
                jsonObject.put("avatar", "https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png");
                sessionMap.values().forEach(rsp -> {
                    System.out.println("收到消息");
                    rsp.sendTextMessage(jsonObject.toJSONString());
//                    rsp.flush();
                });
            }

            @Override
            public void onHandShake(WebSocketRequest request, WebSocketResponse response) {
                System.out.println("加入群组 session");
                sessionMap.put(request, response);
            }

            @Override
            public void onClose(WebSocketRequest request, WebSocketResponse response, CloseReason closeReason) {
                System.out.println("移除群组");
                sessionMap.remove(request);
            }
        });
        HttpBootstrap bootstrap = new HttpBootstrap();
        //配置HTTP消息处理管道
        bootstrap.httpHandler(routeHandle)
                .webSocketHandler(webSocketRouteHandle);

        //设定服务器配置并启动
        bootstrap.start();
    }
}
