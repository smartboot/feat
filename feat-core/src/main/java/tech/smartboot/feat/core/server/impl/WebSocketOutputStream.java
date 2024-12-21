/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: WebSocketOutputStream.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.utils.Constant;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
final class WebSocketOutputStream extends AbstractOutputStream {

    public WebSocketOutputStream(WebSocketRequestImpl webSocketRequest, WebSocketResponseImpl response) {
        super(webSocketRequest.request, response);
        //ws不支持chunked
        disableChunked();
    }

    protected void writeHeadPart(boolean hasHeader) throws IOException {
        // HTTP/1.1
        writeString(request.getProtocol().getProtocol());
        writeBuffer.writeByte((byte) ' ');

        // Status
        HttpStatus httpStatus = response.getHttpStatus();
        httpStatus.write(writeBuffer);
        writeString(HeaderNameEnum.CONTENT_TYPE.getName());
        writeBuffer.writeByte((byte) ':');
        writeString(response.getContentType());
        writeBuffer.write(Constant.CRLF_BYTES);
        if (!hasHeader) {
            writeBuffer.write(Constant.CRLF_BYTES);
        }
    }

}
