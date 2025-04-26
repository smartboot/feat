/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.server.upgrade.sse;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.impl.Upgrade;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public abstract class SSEUpgrade extends Upgrade {
    SseEmitter sseEmitter;

    @Override
    public void init(HttpRequest request, HttpResponse response) throws IOException {
        response = request.getResponse();
        response.setHeader("Content-Type", "text/event-stream");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.getOutputStream().flush();
        SseEmitter sseEmitter = new SseEmitter(this.request.getAioSession());
        onOpen(sseEmitter);
    }

    @Override
    public void onBodyStream(ByteBuffer buffer) {

    }

    public abstract void onOpen(SseEmitter sseEmitter) throws IOException;

    @Override
    public void destroy() {
        if (sseEmitter != null) {
            sseEmitter.complete();
        }
    }
}
