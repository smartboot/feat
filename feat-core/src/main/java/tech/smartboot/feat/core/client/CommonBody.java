/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

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
