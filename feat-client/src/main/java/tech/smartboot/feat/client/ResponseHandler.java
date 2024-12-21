/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: ResponseHandler.java
 * Date: 2021-07-25
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.client;

import tech.smartboot.feat.common.Handler;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/7/25
 */
public abstract class ResponseHandler implements Handler<AbstractResponse> {

    @Override
    public final void onClose(AbstractResponse request) {

    }
}
