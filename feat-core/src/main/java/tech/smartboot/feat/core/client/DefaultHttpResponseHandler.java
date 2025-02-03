/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: DefaultHttpLifecycle.java
 * Date: 2021-07-15
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.client.impl.HttpResponseImpl;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.utils.Constant;
import tech.smartboot.feat.core.common.utils.FixedLengthFrameDecoder;
import tech.smartboot.feat.core.common.utils.SmartDecoder;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/7/15
 */
final class DefaultHttpResponseHandler extends ResponseHandler {
    private static final ResponseHandler DEFAULT_HANDLER = new ResponseHandler() {
        @Override
        public void onBodyStream(ByteBuffer buffer, AbstractResponse response) {
            response.getFuture().complete(response);
        }
    };
    private ResponseHandler responseHandler;
    private boolean gzip;
    private GZIPInputStream gzipInputStream;
    private ByteBuffer gzipBuffer;

    @Override
    public void onBodyStream(ByteBuffer buffer, AbstractResponse baseHttpResponse) {
        if (responseHandler != null) {
            responseHandler.onBodyStream(buffer, baseHttpResponse);
            return;
        }
        String transferEncoding = baseHttpResponse.getHeader(HeaderNameEnum.TRANSFER_ENCODING.getName());
        if (StringUtils.equals(transferEncoding, HeaderValue.TransferEncoding.CHUNKED)) {
            responseHandler = new ChunkedHttpLifecycle();
        } else if (baseHttpResponse.getContentLength() > 0) {
            responseHandler = new ContentLengthHttpLifecycle();
        } else {
            responseHandler = DEFAULT_HANDLER;
        }
        gzip = StringUtils.equals(HeaderValue.ContentEncoding.GZIP, baseHttpResponse.getHeader(HeaderNameEnum.CONTENT_ENCODING.getName()));
        onBodyStream(buffer, baseHttpResponse);
    }

    /**
     * @author 三刀（zhengjunweimail@163.com）
     * @version V1.0 , 2021/7/12
     */
    public class ChunkedHttpLifecycle extends ResponseHandler {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private PART part = PART.CHUNK_LENGTH;
        private SmartDecoder chunkedDecoder;

        @Override
        public void onBodyStream(ByteBuffer buffer, AbstractResponse response) {
            switch (part) {
                case CHUNK_LENGTH:
                    decodeChunkedLength(buffer, response);
                    break;
                case CHUNK_CONTENT:
                    decodeChunkedContent(buffer, response);
                    break;
                case CHUNK_END:
                    decodeChunkedEnd(buffer, response);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        private void decodeChunkedContent(ByteBuffer buffer, AbstractResponse response) {
            if (chunkedDecoder.decode(buffer)) {
                byte[] data = chunkedDecoder.getBuffer().array();

                try {
                    if (DefaultHttpResponseHandler.this.gzip) {
                        if (gzipBuffer != null && gzipBuffer.remaining() > 0) {
                            gzipBuffer.compact();
                            ByteBuffer newBuffer = ByteBuffer.allocate(gzipBuffer.remaining() + data.length);
                            newBuffer.put(gzipBuffer);
                            newBuffer.put(data);
                            newBuffer.flip();
                            gzipBuffer = newBuffer;
                            gzipBuffer.compact();
                        } else {
                            gzipBuffer = ByteBuffer.wrap(data);
                        }
                        if (gzipInputStream == null) {
                            gzipInputStream = new GZIPInputStream(new InputStream() {
                                @Override
                                public int read() throws IOException {
                                    return (DefaultHttpResponseHandler.this.gzipBuffer == null ? -1 : DefaultHttpResponseHandler.this.gzipBuffer.get()) & 0xFF;
                                }

                                @Override
                                public int read(byte[] b, int off, int len) throws IOException {
                                    if (DefaultHttpResponseHandler.this.gzipBuffer == null) {
                                        return -1;
                                    }
                                    int size = Math.min(DefaultHttpResponseHandler.this.gzipBuffer.remaining(), len);
                                    DefaultHttpResponseHandler.this.gzipBuffer.get(b, off, size);
                                    return size;
                                }

                                @Override
                                public int available() throws IOException {
                                    return DefaultHttpResponseHandler.this.gzipBuffer == null ? 0 : DefaultHttpResponseHandler.this.gzipBuffer.remaining();
                                }
                            }) {
                                @Override
                                public int available() throws IOException {
                                    return in.available();
                                }
                            };
                        }
                        byte[] bytes = new byte[4096];
                        int n;
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        while (gzipBuffer.remaining() > 100 && (n = gzipInputStream.read(bytes)) > 0) {
                            bos.write(bytes, 0, n);
                        }
                        data = bos.toByteArray();
                    }
                    if (DefaultHttpResponseHandler.this.steaming != null) {
                        DefaultHttpResponseHandler.this.steaming.stream((HttpResponse) response, data);
                    }
                    body.write(data);
                } catch (IOException e) {
                    throw new FeatException(e);
                }
                part = PART.CHUNK_END;
                onBodyStream(buffer, response);
            }
        }

        private void decodeChunkedEnd(ByteBuffer buffer, AbstractResponse response) {
            if (buffer.remaining() < 2) {
                return;
            }
            if (buffer.get() == Constant.CR && buffer.get() == Constant.LF) {
                part = PART.CHUNK_LENGTH;
                onBodyStream(buffer, response);
            } else {
                throw new IllegalStateException();
            }
        }

        private void decodeChunkedLength(ByteBuffer buffer, AbstractResponse response) {
            int length = StringUtils.scanUntilAndTrim(buffer, Constant.LF);
            if (length < 0) {
                return;
            }
            if (length == 1) {
                finishDecode((HttpResponseImpl) response);
                return;
            }
            int chunkedLength = StringUtils.convertHexString(buffer, buffer.position() - length - 1, length - 1);
            if (chunkedLength != 0) {
                part = PART.CHUNK_CONTENT;
                chunkedDecoder = new FixedLengthFrameDecoder(chunkedLength);
            }
            onBodyStream(buffer, response);
        }

        public void finishDecode(HttpResponseImpl response) {
            try {
                if (gzipInputStream != null && gzipInputStream.available() > 0) {
                    byte[] bytes = new byte[4096];
                    int n;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while (gzipBuffer.hasRemaining() && (n = gzipInputStream.read(bytes)) > 0) {
                        bos.write(bytes, 0, n);
                    }
                    byte[] data = bos.toByteArray();
                    if (DefaultHttpResponseHandler.this.steaming != null) {
                        DefaultHttpResponseHandler.this.steaming.stream(response, data);
                    }
                    body.write(data);
                }
            } catch (IOException e) {
                throw new FeatException(e);
            }
            response.setBody(body.toString());
            callback(response);
        }
    }

    enum PART {
        CHUNK_LENGTH, CHUNK_CONTENT, CHUNK_END
    }

    /**
     * @author 三刀（zhengjunweimail@163.com）
     * @version V1.0 , 2021/7/12
     */
    public static class ContentLengthHttpLifecycle extends ResponseHandler {
        private SmartDecoder smartDecoder;

        @Override
        public void onBodyStream(ByteBuffer buffer, AbstractResponse abstractResponse) {
            HttpResponseImpl response = (HttpResponseImpl) abstractResponse;
            if (smartDecoder == null) {
                int bodyLength = response.getContentLength();
                if (bodyLength > Constant.maxBodySize) {
                    throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
                }
                smartDecoder = new FixedLengthFrameDecoder(response.getContentLength());
            }
            if (smartDecoder.decode(buffer)) {
                response.setBody(new String(smartDecoder.getBuffer().array(), Charset.forName(response.getCharacterEncoding())));
                callback(abstractResponse);
            }
        }
    }

    private static void callback(AbstractResponse response) {
        response.getFuture().complete(response);
    }
}
