/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.server;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.model.McpInitializeRequest;
import tech.smartboot.feat.ai.mcp.model.Roots;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 6/22/25
 */
public class StreamSession {
    public static final int STATE_INITIALIZE = 1;
    public static final int STATE_INITIALIZED = 2;
    public static final int STATE_READY = 3;
    private int state = STATE_INITIALIZE;
    private SseEmitter sseEmitter;
    private final String sessionId = FeatUtils.createSessionId();
    private McpInitializeRequest initializeRequest;
    private final AtomicInteger id = new AtomicInteger(0);
    private final Map<Integer, Consumer<JSONObject>> responseCallbacks = new ConcurrentHashMap<>();
    private final List<Roots> roots = new ArrayList<>();

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getSessionId() {
        return sessionId;
    }

    public SseEmitter getSseEmitter() {
        return sseEmitter;
    }

    public void setSseEmitter(SseEmitter sseEmitter) {
        this.sseEmitter = sseEmitter;
    }

    public McpInitializeRequest getInitializeRequest() {
        return initializeRequest;
    }

    public void setInitializeRequest(McpInitializeRequest initializeRequest) {
        this.initializeRequest = initializeRequest;
    }

    public AtomicInteger getId() {
        return id;
    }

    public void registerResponse(int id, Consumer<JSONObject> response) {
        if (responseCallbacks.containsKey(id)) {
            throw new FeatException("response " + id + " already exists");
        }
        responseCallbacks.put(id, response);
    }

    public Consumer<JSONObject> doResponse(int id) {
        return responseCallbacks.remove(id);
    }

    void rootsList() throws IOException {
        JSONObject capabilities = initializeRequest.getCapabilities();
        if (capabilities == null) {
            return;
        }
        JSONObject rootsCapabilities = capabilities.getJSONObject("roots");
        if (rootsCapabilities == null || !rootsCapabilities.getBooleanValue("listChanged")) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("jsonrpc", "2.0");
        jsonObject.put("method", "roots/list");
        int id = getId().incrementAndGet();
        jsonObject.put("id", id);
        registerResponse(id, result -> {
            JSONArray array = result.getJSONArray("roots");
            roots.clear();
            roots.addAll(array.toList(Roots.class));
            for (Roots root : roots) {
                System.out.println("root:" + root);
            }
        });
//        if (sseEmitter != null) {
        sseEmitter.send(SseEmitter.event().data(jsonObject.toString()));
//        }
    }

    void sampling() {

    }
}
