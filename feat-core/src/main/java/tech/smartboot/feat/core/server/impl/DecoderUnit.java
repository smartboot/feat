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

import tech.smartboot.feat.core.common.DecodeState;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.utils.ByteTree;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class DecoderUnit extends DecodeState {
    private ByteTree<HeaderName> decodeHeaderName;

    public DecoderUnit() {
        super(DecodeState.STATE_METHOD);
    }

    public ByteTree<HeaderName> getDecodeHeaderName() {
        return decodeHeaderName;
    }

    public void setDecodeHeaderName(ByteTree<HeaderName> decodeHeaderName) {
        this.decodeHeaderName = decodeHeaderName;
    }
}
