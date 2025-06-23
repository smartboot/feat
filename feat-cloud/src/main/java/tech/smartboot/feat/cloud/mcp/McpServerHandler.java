/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.core.server.upgrade.sse.SSEUpgrade;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 6/22/25
 */
public class McpServerHandler implements HttpHandler {
    private final Map<String, StreamSession> sseEmitters = new HashMap<>();

    @Override
    public void handle(HttpRequest request) throws Throwable {
        String sessionId = request.getHeader("mcp-session-id");
        StreamSession session;
        if (sessionId == null) {
            session = new StreamSession();
            sessionId = session.getSessionId();
            sseEmitters.put(session.getSessionId(), session);
        } else {
            session = sseEmitters.get(sessionId);
        }
        if (session == null) {
            request.getResponse().setHttpStatus(HttpStatus.UNAUTHORIZED);
            request.getResponse().close();
            return;
        }
        switch (session.getState()) {
            case StreamSession.STATE_INITIALIZE: {
                Request<McpInitializeRequest> req = JSON.parseObject(FeatUtils.asString(request.getInputStream()), new TypeReference<Request<McpInitializeRequest>>() {
                });
                if (req == null) {
                    request.getResponse().close();
                    return;
                }
                Response<McpInitializeResponse> rsp = new Response<>();
                rsp.setId(req.getId());
                McpInitializeResponse initializeResponse = McpInitializeResponse.builder().loggingEnable().promptsEnable().build();
                rsp.setResult(initializeResponse);
                byte[] bytes = JSON.toJSONBytes(rsp);
                request.getResponse().setContentLength(bytes.length);
                request.getResponse().setContentType(HeaderValue.ContentType.APPLICATION_JSON);
                request.getResponse().setHeader("Mcp-Session-Id", sessionId);
                request.getResponse().write(bytes);
                session.setState(StreamSession.STATE_INITIALIZED);
                break;
            }
            case StreamSession.STATE_INITIALIZED: {
                String json1 = FeatUtils.asString(request.getInputStream());
                System.out.println(json1);
                session.setState(StreamSession.STATE_READY);
                request.getResponse().setHttpStatus(HttpStatus.ACCEPTED);
                break;
            }

            case StreamSession.STATE_READY: {
                System.out.println("ready: " + request);
                if (HeaderValue.ContentType.APPLICATION_JSON.equals(request.getContentType())) {
                    Request req = JSON.parseObject(FeatUtils.asString(request.getInputStream()), Request.class);
                    String method = req.getMethod();
                    if ("prompts/list".equals(method)) {
                        Response<JSONObject> response = new Response<>();
                        response.setId(req.getId());
                        JSONObject result = new JSONObject();
                        result.put("prompts", new JSONArray());
                        result.put("nextCursor", null);
                        response.setResult(result);
                        byte[] bytes = JSON.toJSONBytes(response);
                        request.getResponse().setContentLength(bytes.length);
                        request.getResponse().setContentType(HeaderValue.ContentType.APPLICATION_JSON);
                        request.getResponse().write(bytes);

                    } else if ("ping".equals(method)) {
                        Response<JSONObject> response = new Response<>();
                        response.setResult(new JSONObject());
                        response.setId(req.getId());
                        byte[] bytes = JSON.toJSONBytes(response);
                        request.getResponse().setContentLength(bytes.length);
                        request.getResponse().setContentType(HeaderValue.ContentType.APPLICATION_JSON);
                        request.getResponse().write(bytes);
                    }
                    return;
                }

                if ("text/event-stream".equalsIgnoreCase(request.getHeader(HeaderName.ACCEPT.getName()))) {
                    request.upgrade(new SSEUpgrade() {
                        @Override
                        public void onOpen(SseEmitter sseEmitter) throws IOException {

                        }
                    });
                }

                break;
            }
        }
    }

    @Override
    public void onClose(HttpEndpoint request) {
        sseEmitters.remove(request);
    }
}
