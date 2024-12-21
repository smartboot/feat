/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HeaderNameEnum.java
 * Date: 2020-04-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.common.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/12/6
 */
public enum HeaderValueEnum {
    CHUNKED("chunked"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
    APPLICATION_JSON("application/json"),
    UPGRADE("Upgrade"),
    WEBSOCKET("websocket"),
    KEEPALIVE("Keep-Alive"),
    keepalive("keep-alive"),
    CLOSE("close"),
    DEFAULT_CONTENT_TYPE("text/html; charset=utf-8"),
    CONTENT_TYPE_EVENT_STREAM("text/event-stream"),
    CONTINUE("100-continue"),
    GZIP("gzip"),
    H2("h2"),
    H2C("h2c");

    private final String name;

    HeaderValueEnum(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }
}
