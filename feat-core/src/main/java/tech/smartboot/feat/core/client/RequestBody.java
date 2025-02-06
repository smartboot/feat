/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: BodyStream.java
 * Date: 2021-07-17
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/7/17
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
