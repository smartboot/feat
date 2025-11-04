/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import tech.smartboot.feat.core.common.DecodeState;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.core.server.impl.Upgrade;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 异步读取请求体的升级处理器
 * <p>
 * 该类用于处理HTTP请求体的异步读取，当请求体较大或需要特殊处理时，
 * 将HTTP连接升级为异步读取模式，以提高处理效率和资源利用率。
 * </p>
 *
 * @author 三刀
 * @version v1.0 9/23/25
 */
public class AsyncBodyReadUpgrade extends Upgrade {
    /**
     * HTTP端点请求对象
     */
    private final HttpEndpoint request;

    /**
     * 用于存储请求体数据的缓冲区
     */
    private final ByteBuffer buffer;

    /**
     * 构造函数，创建异步读取请求体的升级处理器
     *
     * @param request HTTP端点请求对象
     * @param length  请求体长度
     */
    public AsyncBodyReadUpgrade(HttpEndpoint request, int length) {
        this.request = request;
        // 分配指定大小的缓冲区用于存储请求体数据
        buffer = ByteBuffer.allocate(length);
    }

    /**
     * 初始化方法，由于异步读取不支持初始化，直接抛出异常
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @throws IOException 由于不支持初始化，总是抛出IllegalStateException异常
     */
    @Override
    public void init(HttpRequest request, HttpResponse response) throws IOException {
        // 异步读取不支持初始化操作，直接抛出异常
        throw new IllegalStateException("AsyncBodyReadUpgrade not support");
    }

    /**
     * 处理请求体数据流
     * <p>
     * 当有新的请求体数据到达时，将数据写入缓冲区，当缓冲区满时，
     * 创建异步输入流并完成读取过程。
     * </p>
     *
     * @param readBuffer 包含新数据的ByteBuffer
     */
    @Override
    public void onBodyStream(ByteBuffer readBuffer) {
        // 如果读取缓冲区中的数据量小于等于剩余缓冲区空间
        if (readBuffer.remaining() <= buffer.remaining()) {
            // 直接将数据放入缓冲区
            buffer.put(readBuffer);
        } else {
            // 如果数据量大于剩余缓冲区空间，则只读取能容纳的部分
            int limit = readBuffer.limit();
            readBuffer.limit(readBuffer.position() + buffer.remaining());
            buffer.put(readBuffer);
            readBuffer.limit(limit);
        }

        // 如果缓冲区还有剩余空间，说明还未读取完成，直接返回
        if (buffer.hasRemaining()) {
            return;
        }

        // 缓冲区已满，创建异步输入流并设置到请求对象中
        request.setInputStream(new AsyncBodyInputStream(request));
        // 更新请求解码状态为异步读取完成
        request.getDecodeState().setState(DecodeState.STATE_BODY_ASYNC_READING_DONE);
        // 清除升级处理器
        request.setUpgrade(null);
        // 翻转缓冲区，准备读取数据
        buffer.flip();
    }

    /**
     * 异步请求体输入流实现类
     * <p>
     * 该类继承自BodyInputStream，用于提供对缓冲区中请求体数据的流式访问。
     * </p>
     */
    private class AsyncBodyInputStream extends BodyInputStream {
        /**
         * 构造函数，创建异步请求体输入流
         *
         * @param session HTTP端点会话对象
         */
        public AsyncBodyInputStream(HttpEndpoint session) {
            super(session);
        }


        /**
         * 获取输入流中可读取的字节数
         *
         * @return 缓冲区中剩余的字节数
         */
        @Override
        public int available() {
            return buffer.remaining();
        }

        /**
         * 判断输入流是否已读取完成
         *
         * @return 总是返回true，因为数据已全部读取到缓冲区中
         */
        @Override
        public boolean isFinished() {
            return true;
        }

        /**
         * 从输入流中读取数据到指定的字节数组
         *
         * @param b   目标字节数组
         * @param off 目标数组中的起始偏移量
         * @param len 要读取的最大字节数
         * @return 实际读取的字节数，如果已无数据可读则返回-1
         */
        @Override
        public int read(byte[] b, int off, int len) {
            // 如果请求读取的长度大于缓冲区剩余数据量，则调整为剩余数据量
            if (len > buffer.remaining()) {
                len = buffer.remaining();
            }
            // 如果没有数据可读，返回-1
            if (len == 0) {
                return -1;
            }
            // 从缓冲区中读取数据到目标数组
            buffer.get(b, off, len);
            return len;
        }
    }
}