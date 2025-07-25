/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class RestResult<T> {
    /**
     * 成功
     */
    public static final int SUCCESS = 200;
    /**
     * 失败
     */
    public static final int FAIL = 500;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 返回码
     */
    private int code;

    /**
     * 失败提示
     */
    private String message;
    /**
     * 响应数据
     */
    private T data;

    public static <T> RestResult<T> ok(T data) {
        RestResult<T> restResult = new RestResult<>();
        restResult.setSuccess(true);
        restResult.setCode(SUCCESS);
        restResult.setData(data);
        return restResult;
    }

    public static <T> RestResult<T> fail(String message) {
        RestResult<T> restResult = new RestResult<>();
        restResult.setSuccess(false);
        restResult.setCode(FAIL);
        restResult.setMessage(message);
        return restResult;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
