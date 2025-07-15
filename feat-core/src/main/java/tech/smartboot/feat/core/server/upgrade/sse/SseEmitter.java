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

import com.alibaba.fastjson2.JSON;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SseEmitter {
    private final AioSession aioSession;

    public SseEmitter(AioSession aioSession) {
        this.aioSession = aioSession;
    }

    public void send(SseEventBuilder builder) throws IOException {
        aioSession.writeBuffer().write(builder.build().getBytes());
        aioSession.writeBuffer().flush();
    }

    public void send(String data) throws IOException {
        send(event().data(data));
    }

    public void send(String data, Consumer<Throwable> consumer) {
        try {
            send(event().data(data));
        } catch (Throwable e) {
            try {
                consumer.accept(e);
            } finally {
                complete();
            }
        }
    }

    public void sendAsJson(Object data) {
        sendAsJson(data, throwable -> {

        });
    }

    public void sendAsJson(Object data, Consumer<Throwable> consumer) {
        try {
            if (data instanceof String) {
                send((String) data);
            } else {
                send(event().data(JSON.toJSONString(data)));
            }
        } catch (Throwable e) {
            try {
                consumer.accept(e);
            } finally {
                complete();
            }
        }
    }

    public synchronized void onTimeout(Runnable callback) {
    }

    public synchronized void onError(Consumer<Throwable> callback) {
    }

    public synchronized void onCompletion(Runnable callback) {
    }

    public void complete() {
        aioSession.close();
    }

    public static SseEventBuilder event() {
        return new SseEventBuilderImpl();
    }
}
