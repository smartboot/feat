/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpProtocolEnum.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.common.enums;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/4
 */
public enum HttpProtocolEnum {
    HTTP_11("HTTP/1.1"),
    HTTP_10("HTTP/1.0"),
    HTTP_2("HTTP/2.0"),
    ;

    private final String protocol;

    HttpProtocolEnum(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }
}
