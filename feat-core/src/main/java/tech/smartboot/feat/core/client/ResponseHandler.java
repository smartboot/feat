/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: ResponseHandler.java
 * Date: 2021-07-25
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.Handler;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/7/25
 */
public abstract class ResponseHandler implements Handler<AbstractResponse> {

    @Override
    public final void onClose(AbstractResponse request) {

    }
}
