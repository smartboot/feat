/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpRequestProtocol.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server.impl;

import tech.smartboot.feat.common.DecodeState;
import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.common.enums.HttpStatus;
import tech.smartboot.feat.common.exception.HttpException;
import tech.smartboot.feat.common.utils.ByteTree;
import tech.smartboot.feat.common.utils.Constant;
import tech.smartboot.feat.common.utils.StringUtils;
import tech.smartboot.feat.server.HttpServerConfiguration;
import tech.smartboot.feat.server.ServerHandler;
import tech.smartboot.feat.server.waf.WAF;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
public class HttpRequestProtocol implements Protocol<Request> {
    private final HttpServerConfiguration configuration;
    private static final ByteTree.EndMatcher URI_END_MATCHER = endByte -> (endByte == ' ' || endByte == '?');

    public HttpRequestProtocol(HttpServerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Request decode(ByteBuffer byteBuffer, AioSession session) {
        Request request = session.getAttachment();
        int p = byteBuffer.position();
        boolean flag = decode(byteBuffer, request);
        request.decodeSize(byteBuffer.position() - p);
        return flag ? request : null;
    }

    private boolean decode(ByteBuffer byteBuffer, Request request) {
        DecoderUnit decodeState = request.getDecodeState();
        switch (decodeState.getState()) {
            case DecodeState.STATE_METHOD: {
                ByteTree<?> method = StringUtils.scanByteTree(byteBuffer, ByteTree.SP_END_MATCHER, configuration.getByteCache());
                if (method == null) {
                    break;
                }
                request.setMethod(method.getStringValue());
                decodeState.setState(DecodeState.STATE_URI);
                WAF.methodCheck(configuration, request);
            }
            case DecodeState.STATE_URI: {
                ByteTree<ServerHandler<?, ?>> uriTreeNode = StringUtils.scanByteTree(byteBuffer, URI_END_MATCHER, configuration.getUriByteTree());
                if (uriTreeNode == null) {
                    break;
                }
                request.setUri(uriTreeNode.getStringValue());
                if (uriTreeNode.getAttach() == null) {
                    request.setServerHandler(request.getConfiguration().getHttpServerHandler());
                } else {
                    request.setServerHandler(uriTreeNode.getAttach());
                }
                WAF.checkUri(configuration, request);
                switch (byteBuffer.get(byteBuffer.position() - 1)) {
                    case Constant.SP:
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
                ByteTree<?> query = StringUtils.scanByteTree(byteBuffer, ByteTree.SP_END_MATCHER, configuration.getByteCache());
                if (query == null) {
                    break;
                }
                request.setQueryString(query.getStringValue());
                decodeState.setState(DecodeState.STATE_PROTOCOL_DECODE);
            }
            case DecodeState.STATE_PROTOCOL_DECODE: {
                ByteTree<?> protocol = StringUtils.scanByteTree(byteBuffer, ByteTree.CR_END_MATCHER, configuration.getByteCache());
                if (protocol == null) {
                    break;
                }
                HttpProtocolEnum protocolEnum = (HttpProtocolEnum) protocol.getAttach();
                request.setProtocol(protocolEnum);
                decodeState.setState(DecodeState.STATE_START_LINE_END);
            }
            case DecodeState.STATE_START_LINE_END: {
                if (byteBuffer.remaining() == 0) {
                    break;
                }
                if (byteBuffer.get() != Constant.LF) {
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
                if (byteBuffer.get() == Constant.CR) {
                    if (byteBuffer.get() != Constant.LF) {
                        throw new HttpException(HttpStatus.BAD_REQUEST);
                    }
                    decodeState.setState(DecodeState.STATE_HEADER_CALLBACK);
                    return true;
                }
                byteBuffer.reset();
                if (request.getHeaderSize() > configuration.getHeaderLimiter()) {
                    decodeState.setState(DecodeState.STATE_HEADER_IGNORE);
                    return decode(byteBuffer, request);
                } else {
                    decodeState.setState(DecodeState.STATE_HEADER_NAME);
                }
            }
            // header name解析
            case DecodeState.STATE_HEADER_NAME: {
                ByteTree<HeaderNameEnum> name = StringUtils.scanByteTree(byteBuffer, ByteTree.COLON_END_MATCHER, configuration.getHeaderNameByteTree());
                if (name == null) {
                    break;
                }
                decodeState.setDecodeHeaderName(name);
                decodeState.setState(DecodeState.STATE_HEADER_VALUE);
            }
            // header value解析
            case DecodeState.STATE_HEADER_VALUE: {
                ByteTree<?> value = StringUtils.scanByteTree(byteBuffer, ByteTree.CR_END_MATCHER, configuration.getByteCache());
                if (value == null) {
                    if (byteBuffer.remaining() == byteBuffer.capacity()) {
                        throw new HttpException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
                    }
                    break;
                }
                HeaderNameEnum headerName = decodeState.getDecodeHeaderName().getAttach();
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
                if (byteBuffer.get() != Constant.LF) {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                decodeState.setState(DecodeState.STATE_HEADER_END_CHECK);
                return decode(byteBuffer, request);
            }
            case DecodeState.STATE_HEADER_IGNORE: {
                int position = byteBuffer.position() + byteBuffer.arrayOffset();
                int limit = byteBuffer.limit() + byteBuffer.arrayOffset();
                byte[] data = byteBuffer.array();

                while (limit - position >= 4) {
                    byte b = data[position + 3];
                    if (b == Constant.CR) {
                        position++;
                        continue;
                    } else if (b != Constant.LF) {
                        position += 7;
                        if (position >= limit || (data[position] == Constant.CR || data[position] == Constant.LF)) {
                            position -= 3;
                        }
                        continue;
                    }
                    // header 结束符匹配，最后2字节已经是CR、LF,无需重复验证
                    if (data[position] == Constant.CR && data[position + 1] == Constant.LF) {
                        byteBuffer.position(position + 4 - byteBuffer.arrayOffset());
                        decodeState.setState(DecodeState.STATE_HEADER_CALLBACK);
                        return true;
                    } else {
                        position += 2;
                    }
                }
                byteBuffer.position(position - byteBuffer.arrayOffset());
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

