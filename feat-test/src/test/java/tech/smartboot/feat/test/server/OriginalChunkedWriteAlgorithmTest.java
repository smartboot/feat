/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.server;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.feat.core.common.FeatUtils;
import io.github.smartboot.socket.transport.WriteBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 证明FeatOutputStream原始chunked算法（write(ByteBuffer, callback)）存在缺陷：
 * <p>
 * 原始算法意图复用buffer空间就地写入块大小行与CRLF，但误用了
 * {@code ByteBuffer.put(byte[] src, int offset, int length)}的相对语义——
 * offset/length索引的是源数组（长度仅3~6字节），而代码传入了buffer自身的position/limit。
 * <p>
 * 以下originalWrite方法体逐行复制自git提交ac6e548b（本次修复前）的
 * FeatOutputStream#write(ByteBuffer, Runnable) chunked分支，未做任何改动。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class OriginalChunkedWriteAlgorithmTest {

    /**
     * 无操作WriteBuffer桩，仅记录写入内容
     */
    static class StubWriteBuffer implements WriteBuffer {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public void writeShort(short s) {
        }

        @Override
        public void writeByte(byte b) {
        }

        @Override
        public void writeInt(int i) {
        }

        @Override
        public void writeLong(long l) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
            out.write(b, off, len);
        }

        @Override
        public void write(ByteBuffer buffer, Runnable callback) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            out.write(bytes, 0, bytes.length);
            if (callback != null) {
                callback.run();
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    /**
     * 原始算法，逐行复制自git提交ac6e548b的FeatOutputStream#write(ByteBuffer, Runnable) chunked分支
     */
    private static void originalWrite(WriteBuffer writeBuffer, ByteBuffer buffer, Runnable callback) throws IOException {
        byte[] start = (Integer.toHexString(buffer.remaining()) + "\r\n").getBytes();
        if (buffer.position() >= start.length) {
            buffer.put(start, buffer.position() - start.length, start.length);
            buffer.position(buffer.position() - start.length);
        } else {
            writeBuffer.write(start);
        }
        if (buffer.capacity() - buffer.limit() >= FeatUtils.CRLF_BYTES.length) {
            buffer.put(FeatUtils.CRLF_BYTES, buffer.limit(), FeatUtils.CRLF_BYTES.length);
            buffer.limit(buffer.limit() + FeatUtils.CRLF_BYTES.length);
            writeBuffer.write(buffer, callback);
        } else {
            writeBuffer.write(buffer, () -> {
                try {
                    writeBuffer.write(ByteBuffer.wrap(FeatUtils.CRLF_BYTES), callback);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static ByteBuffer buffer(String data, int capacity, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        buffer.position(offset);
        buffer.put(data.getBytes(StandardCharsets.UTF_8));
        buffer.limit(buffer.position());
        buffer.position(offset);
        return buffer;
    }

    @Test
    public void fullFlippedBuffer_onlySurvivingPath() throws IOException {
        //满buffer（flip后position=0、limit=capacity）：两条就地写入分支均不满足条件，走fallback
        //这是旧算法唯一能正常工作的输入形态，也是其长期未暴露缺陷的原因
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put("World!".getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        StubWriteBuffer writeBuffer = new StubWriteBuffer();
        originalWrite(writeBuffer, buffer, null);
        Assert.assertEquals("6\r\nWorld!\r\n", writeBuffer.out.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void partialBufferWithTailRoom_throws() throws IOException {
        //现实场景：chunked流的最后一个分块，buffer未写满（剩余尾部空间触发就地CRLF分支）
        //put(CRLF_BYTES, 5, 2)：offset=limit=5，超出2字节的源数组边界，必然越界
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.put("Hello".getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        originalWrite(new StubWriteBuffer(), buffer, null);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void positionedBuffer_throws() throws IOException {
        //带前置空间的buffer（触发就地块大小行分支）
        //put(start, 7, 3)：offset=position-start.length=7，超出3字节的源数组边界，必然越界
        ByteBuffer buffer = buffer("Hello", 64, 10);
        originalWrite(new StubWriteBuffer(), buffer, null);
    }

    @Test
    public void headerEdgeCase_corruptsDataThenThrows() throws IOException {
        //分支一唯一不越界的输入（position恰好等于块大小行长度）：相对put把头写进数据区而非数据前
        ByteBuffer buffer = buffer("Hello", 16, 3);
        try {
            originalWrite(new StubWriteBuffer(), buffer, null);
            Assert.fail("应当抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            //分支二仍因limit>0越界抛出；此时数据区已被分支一污染
        }
        byte[] data = new byte[5];
        buffer.position(3);
        buffer.get(data);
        //期望数据"Hello"，实际前3字节被块大小行"5\r\n"覆盖
        Assert.assertEquals("5\r\nlo", new String(data, StandardCharsets.UTF_8));
    }
}
