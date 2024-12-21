/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpResponse.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.client;

/**
 * Http消息请求接口
 *
 * @author 三刀
 * @version V1.0 , 2018/8/7
 */
public interface HttpResponse extends Response {

    String getContentType();

    int getContentLength();

    String getCharacterEncoding();

    String body();

}
