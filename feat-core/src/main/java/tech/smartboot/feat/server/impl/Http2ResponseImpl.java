/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpResponseImpl.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server.impl;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
class Http2ResponseImpl extends AbstractResponse {

    public Http2ResponseImpl(int streamId, Http2RequestImpl httpRequest, boolean push) {
        init(httpRequest.getAioSession(), new Http2OutputStream(streamId, httpRequest, this,push));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closed = true;
        }
    }
}
