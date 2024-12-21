/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpRequestImpl.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.client.impl;

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
