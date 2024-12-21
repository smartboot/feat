/*******************************************************************************
 * Copyright (c) 2017-2022, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: LfDecoder.java
 * Date: 2022-01-12
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server.decode;

import tech.smartboot.feat.common.enums.HttpStatus;
import tech.smartboot.feat.common.exception.HttpException;
import tech.smartboot.feat.common.utils.Constant;
import tech.smartboot.feat.server.HttpServerConfiguration;
import tech.smartboot.feat.server.impl.Request;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/12
 */
class LfDecoder extends AbstractDecoder {
    private final AbstractDecoder nextDecoder;

    public LfDecoder(AbstractDecoder nextDecoder, HttpServerConfiguration configuration) {
        super(configuration);
        this.nextDecoder = nextDecoder;
    }

    @Override
    public Decoder decode0(ByteBuffer byteBuffer, Request request) {
        if (byteBuffer.hasRemaining()) {
            if (byteBuffer.get() != Constant.LF) {
                throw new HttpException(HttpStatus.BAD_REQUEST);
            }
            return nextDecoder.decode(byteBuffer, request);
        }
        return this;
    }
}
