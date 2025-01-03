/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpMethodEnum.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.common.enums;

/**
 * Http支持的Method
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public enum HttpMethodEnum {
    OPTIONS("OPTIONS"),
    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    TRACE("TRACE"),
    CONNECT("CONNECT");

    private final String method;
    private final String lowerCaseMethod;

    HttpMethodEnum(String method) {
        this.method = method;
        this.lowerCaseMethod = method.toLowerCase();
    }

    public String getMethod() {
        return method;
    }

    public String getLowerCaseMethod() {
        return lowerCaseMethod;
    }
}
