/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HeaderNameEnum.java
 * Date: 2020-04-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.common.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/12/6
 */
public interface HeaderValueEnum {

    String CONTINUE = "100-continue";

    interface TransferEncoding {
        String CHUNKED = "chunked";
    }

    interface Upgrade {
        String WEBSOCKET = "websocket";
        String H2 = "h2";
        String H2C = "h2c";
    }

    interface ContentEncoding {
        String GZIP = "gzip";
    }

    interface Connection {
        String UPGRADE = "Upgrade";
        String KEEPALIVE = "Keep-Alive";
        String keepalive = "keep-alive";
        String CLOSE = "close";
    }

    interface ContentType {
        String MULTIPART_FORM_DATA = "multipart/form-data";
        String X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
        String APPLICATION_JSON = "application/json";
        String APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";
        String TEXT_HTML_UTF8 = "text/html; charset=utf-8";
        String TEXT_PLAIN_UTF8 = "text/plain; charset=UTF-8";
        String EVENT_STREAM = "text/event-stream";
    }
}
