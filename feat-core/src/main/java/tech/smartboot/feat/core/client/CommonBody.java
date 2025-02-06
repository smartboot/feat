package tech.smartboot.feat.core.client;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
class CommonBody implements RequestBody {
    private final RequestBody body;


    public CommonBody(RequestBody body) {
        this.body = body;
    }

    @Override
    public RequestBody write(byte[] bytes, int offset, int len) {
        body.write(bytes, offset, len);
        return this;
    }

    @Override
    public void transferFrom(ByteBuffer buffer, Consumer<RequestBody> consumer) {
        body.transferFrom(buffer, (b) -> consumer.accept(this));
    }

    @Override
    public final RequestBody flush() {
        body.flush();
        return this;
    }
}
