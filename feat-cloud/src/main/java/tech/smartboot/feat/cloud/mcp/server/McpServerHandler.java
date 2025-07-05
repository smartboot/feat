/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.server;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.core.server.upgrade.sse.SSEUpgrade;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.RouterHandler;

import java.io.IOException;

/**
 * @author 三刀
 * @version v1.0 6/22/25
 */
public class McpServerHandler implements RouterHandler {

    private final McpServer mcp = new McpServer();


    @Override
    public void handle(Context context) throws Throwable {
        HttpRequest request = context.Request;
        String sessionId = request.getHeader("mcp-session-id");
        StreamSession session;
        if (sessionId == null) {
            session = new StreamSession();
            sessionId = session.getSessionId();
            mcp.getSseEmitters().put(session.getSessionId(), session);
        } else {
            session = mcp.getSseEmitters().get(sessionId);
        }
        if (session == null) {
            request.getResponse().setHttpStatus(HttpStatus.UNAUTHORIZED);
            request.getResponse().close();
            return;
        }
        if (request.getContentType() == null && HeaderValue.ContentType.EVENT_STREAM.equalsIgnoreCase(request.getHeader(HeaderName.ACCEPT))) {
            request.upgrade(new SSEUpgrade() {
                @Override
                public void onOpen(SseEmitter sseEmitter) throws IOException {
//                    sseEmitter.send(SseEventBuilder.event().name("init").data("init"));
                    System.out.println("onOpen");
                }
            });
            return;
        }
        Response response = mcp.jsonRpcHandle(session, request);
        if (response != null) {
            byte[] bytes = JSON.toJSONBytes(response);
            request.getResponse().setContentLength(bytes.length);
            request.getResponse().setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            request.getResponse().setHeader("Mcp-Session-Id", sessionId);
            request.getResponse().write(bytes);
        }
    }

    @Override
    public void onClose(HttpEndpoint request) {
        mcp.getSseEmitters().remove(request);
    }

    public McpServer getMcp() {
        return mcp;
    }
}
