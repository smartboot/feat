/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: ResponseHandler.java
 * Date: 2021-07-25
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.client;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/7/25
 */
public abstract class ResponseHandler {
    /**
     * 解析 body 数据流
     *
     * @param buffer
     * @param request
     * @return
     */
    public abstract void onBodyStream(ByteBuffer buffer, AbstractResponse request);


    /**
     * Http header 完成解析
     */
    public void onHeaderComplete(AbstractResponse request) throws IOException {
    }


    public final void onClose(AbstractResponse request) {

    }
}
