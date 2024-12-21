/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpRequestImpl.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client.impl;

import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public final class HttpRequestImpl extends AbstractRequest {

    public HttpRequestImpl(AioSession session) {
        init(new HttpOutputStream(this, session));
    }
}
