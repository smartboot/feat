package tech.smartboot.feat.server.impl;

import tech.smartboot.feat.common.DecodeState;
import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.utils.ByteTree;

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
