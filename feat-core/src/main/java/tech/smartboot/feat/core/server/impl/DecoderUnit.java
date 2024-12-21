package tech.smartboot.feat.core.server.impl;

import tech.smartboot.feat.core.common.DecodeState;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.utils.ByteTree;

public class DecoderUnit extends DecodeState {
    private ByteTree<HeaderNameEnum> decodeHeaderName;

    public DecoderUnit() {
        super(DecodeState.STATE_METHOD);
    }

    public ByteTree<HeaderNameEnum> getDecodeHeaderName() {
        return decodeHeaderName;
    }

    public void setDecodeHeaderName(ByteTree<HeaderNameEnum> decodeHeaderName) {
        this.decodeHeaderName = decodeHeaderName;
    }
}
