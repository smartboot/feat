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

import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;

import java.util.UUID;

/**
 * @author 三刀
 * @version v1.0 6/22/25
 */
public class StreamSession {
    public static final int STATE_INITIALIZE = 1;
    public static final int STATE_INITIALIZED = 2;
    public static final int STATE_READY = 3;
    private int state = STATE_INITIALIZE;
    private SseEmitter sseEmitter;
    private final String sessionId = UUID.randomUUID().toString();

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
}
