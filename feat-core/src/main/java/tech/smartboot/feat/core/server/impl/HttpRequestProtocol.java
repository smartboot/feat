/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.server.impl;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.common.ByteTree;
import tech.smartboot.feat.core.common.DecodeState;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HttpProtocol;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.core.server.waf.WAF;

import java.nio.ByteBuffer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HttpRequestProtocol implements Protocol<HttpEndpoint> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestProtocol.class);
    private final ServerOptions options;
    private static final ByteTree.EndMatcher URI_END_MATCHER = endByte -> (endByte == ' ' || endByte == '?');
    private static final byte HTTP_VERSION_0 = '0';
    private static final byte HTTP_VERSION_1 = '1';
    private static final byte HTTP_VERSION_2 = '2';

    public HttpRequestProtocol(ServerOptions options) {
        this.options = options;
    }

    @Override
    public HttpEndpoint decode(ByteBuffer byteBuffer, AioSession session) {
        HttpEndpoint request = session.getAttachment();
        int p = byteBuffer.position();
        boolean flag = decode(byteBuffer, request);
        request.decodeSize(byteBuffer.position() - p);
        return flag ? request : null;
    }

    private boolean decode(ByteBuffer byteBuffer, HttpEndpoint request) {
        DecoderUnit decodeState = request.getDecodeState();
        switch (decodeState.getState()) {
            case DecodeState.STATE_METHOD: {
                ByteTree<?> method = FeatUtils.scanByteTree(byteBuffer, ByteTree.SP_END_MATCHER, options.getByteCache());
                if (method == null) {
                    break;
                }
                request.setMethod(method.getStringValue());
                decodeState.setState(DecodeState.STATE_URI);
                WAF.methodCheck(options, request);
            }
            case DecodeState.STATE_URI: {
                ByteTree<HttpHandler> uriTreeNode = FeatUtils.scanByteTree(byteBuffer, URI_END_MATCHER, options.getUriByteTree());
                if (uriTreeNode == null) {
                    break;
                }
                request.setUri(uriTreeNode.getStringValue());
                if (uriTreeNode.getAttach() != null) {
                    request.setServerHandler(uriTreeNode.getAttach());
                }
                WAF.checkUri(options, request);
                switch (byteBuffer.get(byteBuffer.position() - 1)) {
                    case FeatUtils.SP:
                        decodeState.setState(DecodeState.STATE_PROTOCOL_DECODE);
                        break;
                    case '?':
                        decodeState.setState(DecodeState.STATE_URI_QUERY);
                        break;
                    default:
                        throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                return decode(byteBuffer, request);
            }
            case DecodeState.STATE_URI_QUERY: {
                ByteTree<?> query = FeatUtils.scanByteTree(byteBuffer, ByteTree.SP_END_MATCHER, options.getByteCache());
                if (query == null) {
                    break;
                }
                request.setQueryString(query.getStringValue());
                decodeState.setState(DecodeState.STATE_PROTOCOL_DECODE);
            }
            case DecodeState.STATE_PROTOCOL_DECODE: {
                // 跳过空格
                do {
                    byteBuffer.mark();
                } while (byteBuffer.hasRemaining() && byteBuffer.get() == FeatUtils.SP);
                byteBuffer.reset();
                if (byteBuffer.remaining() < 9) {
                    break;
                }
                byte major = byteBuffer.get(byteBuffer.position() + 5);
                if (major == HTTP_VERSION_1) {
                    byte minor = byteBuffer.get(byteBuffer.position() + 7);
                    if (minor == HTTP_VERSION_1) {
                        request.setProtocol(HttpProtocol.HTTP_11);
                    } else if (minor == HTTP_VERSION_0) {
                        request.setProtocol(HttpProtocol.HTTP_10);
                    } else {
                        throw new HttpException(HttpStatus.BAD_REQUEST);
                    }
                } else if (major == HTTP_VERSION_2) {
                    request.setProtocol(HttpProtocol.HTTP_2);
                } else {
                    byte[] bytes = new byte[byteBuffer.limit()];
                    byteBuffer.position(0);
                    byteBuffer.get(bytes);
                    LOGGER.error("Unsupported HTTP version, remote:{}, method:{}, uri:{} , data:{} ", request.getRemoteAddr(), request.getMethod(), request.getUri(), new String(bytes));
                    throw new HttpException(HttpStatus.BAD_REQUEST, "Unsupported HTTP version");
                }

                byteBuffer.position(byteBuffer.position() + 9);
                decodeState.setState(DecodeState.STATE_START_LINE_END);
            }
            case DecodeState.STATE_START_LINE_END: {
                if (byteBuffer.remaining() == 0) {
                    break;
                }
                if (byteBuffer.get() != FeatUtils.LF) {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                decodeState.setState(DecodeState.STATE_HEADER_END_CHECK);
            }
            // header结束判断
            case DecodeState.STATE_HEADER_END_CHECK: {
                if (byteBuffer.remaining() < 2) {
                    break;
                }
                //header解码结束
                byteBuffer.mark();
                if (byteBuffer.get() == FeatUtils.CR) {
                    if (byteBuffer.get() != FeatUtils.LF) {
                        throw new HttpException(HttpStatus.BAD_REQUEST);
                    }
                    decodeState.setState(DecodeState.STATE_HEADER_CALLBACK);
                    return true;
                }
                byteBuffer.reset();
                if (request.getHeaderSize() < options.getHeaderLimiter()) {
                    decodeState.setState(DecodeState.STATE_HEADER_NAME);
                } else {
                    decodeState.setState(DecodeState.STATE_HEADER_IGNORE);
                    return decode(byteBuffer, request);
                }
            }
            // header name解析
            case DecodeState.STATE_HEADER_NAME: {
                ByteTree<HeaderName> name = FeatUtils.scanByteTree(byteBuffer, ByteTree.COLON_END_MATCHER, options.getHeaderNameByteTree());
                if (name == null) {
                    break;
                }
                decodeState.setDecodeHeaderName(name);
                decodeState.setState(DecodeState.STATE_HEADER_VALUE);
            }
            // header value解析
            case DecodeState.STATE_HEADER_VALUE: {
                ByteTree<?> value = FeatUtils.scanByteTree(byteBuffer, ByteTree.CR_END_MATCHER, options.getByteCache());
                if (value == null) {
                    if (byteBuffer.remaining() == byteBuffer.capacity()) {
                        throw new HttpException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE, "The length of the value of header <u>" + decodeState.getDecodeHeaderName().getStringValue() + "</u> exceeds the read buffer.");
                    }
                    break;
                }
                HeaderName headerName = decodeState.getDecodeHeaderName().getAttach();
                if (headerName != null) {
                    request.addHeader(headerName.getLowCaseName(), decodeState.getDecodeHeaderName().getStringValue(), value.getStringValue());
                } else {
                    request.addHeader(decodeState.getDecodeHeaderName().getStringValue().toLowerCase(), decodeState.getDecodeHeaderName().getStringValue(), value.getStringValue());
                }

                decodeState.setState(DecodeState.STATE_HEADER_LINE_END);
            }
            // header line结束
            case DecodeState.STATE_HEADER_LINE_END: {
                if (!byteBuffer.hasRemaining()) {
                    break;
                }
                if (byteBuffer.get() != FeatUtils.LF) {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                decodeState.setState(DecodeState.STATE_HEADER_END_CHECK);
                return decode(byteBuffer, request);
            }
            case DecodeState.STATE_HEADER_IGNORE: {
                while (byteBuffer.remaining() >= 4) {
                    int position = byteBuffer.position() + 3;
                    byte b = byteBuffer.get(position);
                    if (b == FeatUtils.CR) {
                        byteBuffer.position(position - 2);
                        continue;
                    } else if (b != FeatUtils.LF) {
                        byteBuffer.position(position);
                        continue;
                    }
                    // header 结束符匹配，最后2字节已经是CR、LF,无需重复验证
                    if (byteBuffer.get(position - 3) == FeatUtils.CR && byteBuffer.get(position - 2) == FeatUtils.LF) {
                        byteBuffer.position(position + 1);
                        decodeState.setState(DecodeState.STATE_HEADER_CALLBACK);
                        return true;
                    } else {
                        byteBuffer.position(position - 1);
                    }
                }
                return false;
            }
            case DecodeState.STATE_BODY_READING_MONITOR:
                decodeState.setState(DecodeState.STATE_BODY_READING_CALLBACK);
                if (byteBuffer.position() > 0) {
                    break;
                }
            case DecodeState.STATE_BODY_READING_CALLBACK:
                return true;
        }
        return false;
    }
}

