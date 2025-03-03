/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.DecodeState;

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
