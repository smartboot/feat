/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpMessageProcessor.java
 * Date: 2021-02-08
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.DecodeState;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.utils.ByteTree;
import tech.smartboot.feat.core.common.utils.Constant;
import tech.smartboot.feat.core.common.utils.StringUtils;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.extension.processor.AbstractMessageProcessor;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

/**
 * @author 三刀
 * @version V1.0 , 2018/6/10
 */
final class HttpMessageProcessor extends AbstractMessageProcessor<AbstractResponse> implements Protocol<AbstractResponse> {
    private final ExecutorService executorService;

    public HttpMessageProcessor() {
        this(null);
    }

    public HttpMessageProcessor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public AbstractResponse decode(ByteBuffer buffer, AioSession session) {
        DecoderUnit attachment = session.getAttachment();
        AbstractResponse response = attachment.getResponse();
        switch (attachment.getState()) {
            // 协议解析
            case DecodeState.STATE_PROTOCOL_DECODE: {
                ByteTree<?> method = StringUtils.scanByteTree(buffer, ByteTree.SP_END_MATCHER, ByteTree.DEFAULT);
                if (method == null) {
                    return null;
                }
                response.setProtocol(method.getStringValue());
                attachment.setState(DecodeState.STATE_STATUS_CODE);
            }
            // 状态码解析
            case DecodeState.STATE_STATUS_CODE: {
                ByteTree<?> byteTree = StringUtils.scanByteTree(buffer, ByteTree.SP_END_MATCHER, ByteTree.DEFAULT);
                if (byteTree == null) {
                    return null;
                }
                int statusCode = Integer.parseInt(byteTree.getStringValue());
                response.setStatus(statusCode);
                attachment.setState(DecodeState.STATE_STATUS_DESC);
            }
            // 状态码描述解析
            case DecodeState.STATE_STATUS_DESC: {
                ByteTree<?> byteTree = StringUtils.scanByteTree(buffer, ByteTree.CR_END_MATCHER, ByteTree.DEFAULT);
                if (byteTree == null) {
                    return null;
                }
                response.setReasonPhrase(byteTree.getStringValue());
                attachment.setState(DecodeState.STATE_START_LINE_END);
            }
            // 状态码结束
            case DecodeState.STATE_START_LINE_END: {
                if (buffer.remaining() == 0) {
                    return null;
                }
                if (buffer.get() != Constant.LF) {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                attachment.setState(DecodeState.STATE_HEADER_END_CHECK);
            }
            // header结束判断
            case DecodeState.STATE_HEADER_END_CHECK: {
                if (buffer.remaining() < 2) {
                    return null;
                }
                //header解码结束
                buffer.mark();
                if (buffer.get() == Constant.CR) {
                    if (buffer.get() != Constant.LF) {
                        throw new HttpException(HttpStatus.BAD_REQUEST);
                    }
                    attachment.setState(DecodeState.STATE_HEADER_CALLBACK);
                    return response;
                } else {
                    buffer.reset();
                    attachment.setState(DecodeState.STATE_HEADER_NAME);
                }
            }
            // header name解析
            case DecodeState.STATE_HEADER_NAME: {
                ByteTree<?> name = StringUtils.scanByteTree(buffer, ByteTree.COLON_END_MATCHER, ByteTree.DEFAULT);
                if (name == null) {
                    return null;
                }
                attachment.setDecodeHeaderName(name.getStringValue());
                attachment.setState(DecodeState.STATE_HEADER_VALUE);
            }
            // header value解析
            case DecodeState.STATE_HEADER_VALUE: {
                ByteTree<?> value = StringUtils.scanByteTree(buffer, ByteTree.CR_END_MATCHER, ByteTree.DEFAULT);
                if (value == null) {
                    if (buffer.remaining() == buffer.capacity()) {
                        throw new HttpException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
                    }
                    return null;
                }
                response.setHeader(attachment.getDecodeHeaderName(), value.getStringValue());
                attachment.setState(DecodeState.STATE_HEADER_LINE_END);
            }
            // header line结束
            case DecodeState.STATE_HEADER_LINE_END: {
                if (!buffer.hasRemaining()) {
                    return null;
                }
                if (buffer.get() != Constant.LF) {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                attachment.setState(DecodeState.STATE_HEADER_END_CHECK);
                return decode(buffer, session);
            }
            //
            case DecodeState.STATE_BODY: {
                response.getResponseHandler().onBodyStream(buffer, response);
            }
        }
        return null;
    }

    @Override
    public void process0(AioSession session, AbstractResponse response) {
        DecoderUnit decoderUnit = session.getAttachment();
        ResponseHandler responseHandler = response.getResponseHandler();

        switch (decoderUnit.getState()) {
            case DecodeState.STATE_HEADER_CALLBACK:
                try {
                    responseHandler.onHeaderComplete(response);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                decoderUnit.setState(DecoderUnit.STATE_BODY);
                responseHandler.onBodyStream(session.readBuffer(), response);
                return;
            case DecodeState.STATE_FINISH:
                if (executorService == null) {
                    response.getFuture().complete(response);
                } else {
                    session.awaitRead();
                    executorService.execute(() -> {
                        response.getFuture().complete(response);
                        session.signalRead();
                    });
                }
                break;
            default:
                throw new RuntimeException("unreachable");
        }
    }


    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION: {
                DecoderUnit attachment = new DecoderUnit();
                session.setAttachment(attachment);
            }
            break;
            case PROCESS_EXCEPTION:
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                session.close();
                break;
            case DECODE_EXCEPTION:
                throwable.printStackTrace();
                break;
            case SESSION_CLOSED:
//                ResponseAttachment attachment = session.getAttachment();
//                attachment.getResponse().getFuture().is
                System.out.println("closed");
                break;
        }
    }

}