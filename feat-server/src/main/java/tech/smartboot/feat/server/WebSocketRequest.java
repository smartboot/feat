/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: WebSocketRequest.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server;

import org.smartboot.socket.util.Attachment;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

/**
 * WebSocket消息请求接口
 *
 * @author 三刀
 * @version V1.0 , 2020/4/1
 */
public interface WebSocketRequest {

    public int getFrameOpcode();

    public byte[] getPayload();

    String getRequestURL();

    String getRequestURI();

    String getQueryString();

    Map<String, String[]> getParameters();

    InetSocketAddress getRemoteAddress();

    /**
     * 获取套接字绑定的本地地址。
     *
     * @return
     */
    InetSocketAddress getLocalAddress();


    /**
     * 是否启动安全通信
     */
    boolean isSecure();

    /**
     * 获取附件对象
     *
     * @return 附件
     */
    Attachment getAttachment();

    /**
     * 存放附件，支持任意类型
     *
     * @param attachment 附件对象
     */
    void setAttachment(Attachment attachment);

    /**
     * 获取指定名称的Http Header值
     *
     * @param headName
     * @return
     */
    String getHeader(String headName);


    Collection<String> getHeaders(String name);

    Collection<String> getHeaderNames();
}
