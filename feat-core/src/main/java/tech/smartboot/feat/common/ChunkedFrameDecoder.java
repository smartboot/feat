//package tech.smartboot.feat.common;
//
//import tech.smartboot.feat.common.enums.HttpStatus;
//import tech.smartboot.feat.common.exception.HttpException;
//import tech.smartboot.feat.common.utils.Constant;
//import tech.smartboot.feat.common.utils.SmartDecoder;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//
//public class ChunkedFrameDecoder implements SmartDecoder {
//    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(8);
//    private boolean readChunkedLengthFlag = true;
//    /**
//     * 剩余可读字节数
//     */
//    private int remainingThreshold = -1;
//
//    private int chunkedSize = -1;
//
//    @Override
//    public boolean decode(ByteBuffer byteBuffer) {
//        readChunkedLength(byteBuffer);
//        if (chunkedSize < 0) {
//            return false;
//        }
//        int remaining = byteBuffer.remaining();
//        if (remaining == 0) {
//            return false;
//        }
//        //chunked 结束
//        if (remainingThreshold == 0) {
//            if (remaining < 2) {
//                return false;
//            }
//            readCrlf(byteBuffer);
//            if (chunkedSize == 0) {
//                return true;
//            } else {
//                chunkedSize = -1;
//                readChunkedLengthFlag = true;
//                return decode(byteBuffer);
//            }
//        }
//        //读取chunked内容
//        int readSize = Math.min(byteBuffer.remaining(), remainingThreshold);
//        byte[] bytes = new byte[readSize];
//        byteBuffer.get(bytes);
//        try {
//            buffer.write(bytes);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        remainingThreshold -= readSize;
//        return decode(byteBuffer);
//    }
//
//    @Override
//    public ByteBuffer getBuffer() {
//        return ByteBuffer.wrap(buffer.toByteArray());
//    }
//
//    private void readChunkedLength(ByteBuffer byteBuffer) {
//        byteBuffer.mark();
//        while (readChunkedLengthFlag && byteBuffer.remaining() > 2) {
//            if (byteBuffer.get() != Constant.CR) {
//                continue;
//            }
//            if (byteBuffer.get() != Constant.LF) {
//                throw new HttpException(HttpStatus.BAD_REQUEST);
//            }
//            int position = byteBuffer.position();
//            byteBuffer.reset();
//            byte[] bytes = new byte[position - byteBuffer.position() - 2];
//            byteBuffer.get(bytes);
//            byteBuffer.position(position);
//            chunkedSize = Integer.parseInt(new String(bytes), 16);
//            remainingThreshold = chunkedSize;
//            readChunkedLengthFlag = false;
//        }
//        if (chunkedSize < 0) {
//            byteBuffer.reset();
//            return;
//        }
//        byteBuffer.mark();
//    }
//
//    private void readCrlf(ByteBuffer buffer) {
//        if (buffer.get() != Constant.CR) {
//            throw new HttpException(HttpStatus.BAD_REQUEST);
//        }
//        if (buffer.get() != Constant.LF) {
//            throw new HttpException(HttpStatus.BAD_REQUEST);
//        }
//    }
//}
