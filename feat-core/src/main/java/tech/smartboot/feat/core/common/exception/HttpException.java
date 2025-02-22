/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpException.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.common.exception;

import tech.smartboot.feat.core.common.enums.HttpStatus;

/**
 * HTTP异常
 *
 * @author 三刀
 * @version V1.0 , 2018/6/3
 */
public class HttpException extends RuntimeException {
    private final HttpStatus httpStatus;

    public HttpException(HttpStatus httpStatus) {
        this(httpStatus, httpStatus.getReasonPhrase());
    }

    public HttpException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpException(int httpStatusCode, String desc) {
        this(new HttpStatus(httpStatusCode, desc));
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
