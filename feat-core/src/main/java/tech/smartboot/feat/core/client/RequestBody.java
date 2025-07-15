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
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface RequestBody {
    /**
     * 往缓冲区中写入数据
     */
    RequestBody write(byte[] bytes, int offset, int len);

    /**
     * 往缓冲区中写入数据
     */
    void transferFrom(ByteBuffer buffer, Consumer<RequestBody> consumer);

    /**
     * 往缓冲区中写入数据
     */
    default RequestBody write(byte[] bytes) {
        write(bytes, 0, bytes.length);
        return this;
    }

    default RequestBody write(String str) {
        return write(str.getBytes());
    }

    /**
     * 输出缓冲区的数据
     */
    RequestBody flush();
}
