package tech.smartboot.feat.core.client;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
class CommonBody<T extends HttpRest> implements Body<T> {
    private final Body<? extends HttpRest> body;
    private final T rest;


    public CommonBody(Body<? extends HttpRest> body, T rest) {
        this.body = body;
        this.rest = rest;
    }

    @Override
    public Body<T> write(byte[] bytes, int offset, int len) {
        body.write(bytes, offset, len);
        return this;
    }

//    @Override
//    public void write(byte[] bytes, int offset, int len, Consumer<Body<T>> consumer) {
//        body.write(bytes, offset, len, (b) -> consumer.accept(this));
//    }

    @Override
    public void transferFrom(ByteBuffer buffer, Consumer<Body<T>> consumer) {
        body.transferFrom(buffer, (b) -> consumer.accept(this));
    }

    @Override
    public final Body<T> flush() {
        body.flush();
        return this;
    }

    @Override
    public final T done() {
        return rest;
    }
}
