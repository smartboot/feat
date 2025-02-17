package tech.smartboot.feat.core.server.upgrade.sse;

import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.function.Consumer;

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
