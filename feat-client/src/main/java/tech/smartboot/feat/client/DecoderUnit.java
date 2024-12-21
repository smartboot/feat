/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: RequestAttachment.java
 * Date: 2021-05-26
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.client;

import tech.smartboot.feat.common.DecodeState;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/5/26
 */
class DecoderUnit extends DecodeState {
    private String decodeHeaderName;

    public DecoderUnit() {
        super(DecodeState.STATE_PROTOCOL_DECODE);
    }

    private AbstractResponse response;


    public AbstractResponse getResponse() {
        return response;
    }

    public void setResponse(AbstractResponse response) {
        this.response = response;
    }

    public String getDecodeHeaderName() {
        return decodeHeaderName;
    }

    public void setDecodeHeaderName(String decodeHeaderName) {
        this.decodeHeaderName = decodeHeaderName;
    }

}
