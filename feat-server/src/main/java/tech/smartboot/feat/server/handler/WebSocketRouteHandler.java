/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: RouteHandle.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server.handler;

import tech.smartboot.feat.common.logging.Logger;
import tech.smartboot.feat.common.logging.LoggerFactory;
import tech.smartboot.feat.common.utils.AntPathMatcher;
import tech.smartboot.feat.server.WebSocketHandler;
import tech.smartboot.feat.server.WebSocketRequest;
import tech.smartboot.feat.server.WebSocketResponse;
import tech.smartboot.feat.server.impl.Request;
import tech.smartboot.feat.server.impl.WebSocketRequestImpl;
import tech.smartboot.feat.server.impl.WebSocketResponseImpl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/24
 */
public final class WebSocketRouteHandler extends WebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketRouteHandler.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    /**
     * 默认404
     */
    private final WebSocketHandler defaultHandler = new WebSocketHandler() {
        @Override
        public void handle(WebSocketRequest request, WebSocketResponse response) throws IOException {
            LOGGER.warn("not found");
        }
    };
    private final Map<String, WebSocketHandler> handlerMap = new ConcurrentHashMap<>();

    @Override
    public void onClose(Request request) {
        handlerMap.get(request.getRequestURI()).onClose(request);
    }

    @Override
    public void willHeaderComplete(WebSocketRequestImpl request, WebSocketResponseImpl response) {
        String uri = request.getRequestURI();
        WebSocketHandler httpHandler = handlerMap.get(uri);
        if (httpHandler == null) {
            for (Map.Entry<String, WebSocketHandler> entity : handlerMap.entrySet()) {
                if (PATH_MATCHER.match(entity.getKey(), uri)) {
                    httpHandler = entity.getValue();
                    break;
                }
            }
            if (httpHandler == null) {
                httpHandler = defaultHandler;
            }
            handlerMap.put(uri, httpHandler);
        }
        httpHandler.willHeaderComplete(request, response);
    }

    @Override
    public void whenHeaderComplete(WebSocketRequestImpl request, WebSocketResponseImpl response) {
        WebSocketHandler httpHandler = handlerMap.get(request.getRequestURI());
        httpHandler.whenHeaderComplete(request, response);
    }

    @Override
    public void handle(WebSocketRequest request, WebSocketResponse response) throws Throwable {
        WebSocketHandler httpHandler = handlerMap.get(request.getRequestURI());
        httpHandler.handle(request, response);
    }

    /**
     * 配置URL路由
     *
     * @param urlPattern url匹配
     * @param httpHandle 处理handle
     * @return
     */
    public WebSocketRouteHandler route(String urlPattern, WebSocketHandler httpHandle) {
        handlerMap.put(urlPattern, httpHandle);
        return this;
    }
}
