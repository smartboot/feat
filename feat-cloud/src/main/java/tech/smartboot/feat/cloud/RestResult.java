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
 * RESTful API响应结果封装类
 * <p>
 * 该类用于统一封装RESTful API的响应结果，包括成功/失败状态、响应码、提示信息和业务数据。
 * 提供了标准化的响应格式，便于前端处理和统一异常处理。
 * </p>
 *
 * @param <T> 响应数据的类型参数
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class RestResult<T> {
    /**
     * 成功状态码
     */
    public static final int SUCCESS = 200;

    /**
     * 失败状态码
     */
    public static final int FAIL = 500;

    /**
     * 是否成功标识
     * <p>
     * true表示请求处理成功，false表示请求处理失败
     * </p>
     */
    private boolean success;

    /**
     * 响应状态码
     * <p>
     * 用于表示请求处理的结果状态，遵循HTTP状态码规范
     * </p>
     */
    private int code;

    /**
     * 失败提示信息
     * <p>
     * 当请求处理失败时，提供详细的错误提示信息
     * </p>
     */
    private String message;

    /**
     * 响应业务数据
     * <p>
     * 用于承载请求处理成功后的业务数据
     * </p>
     */
    private T data;

    /**
     * 创建成功的响应结果
     * <p>
     * 该方法用于创建表示请求处理成功的响应结果，设置状态码为200，success为true。
     * </p>
     *
     * @param data 响应业务数据
     * @param <T>  业务数据类型
     * @return 成功的响应结果实例
     */
    public static <T> RestResult<T> ok(T data) {
        RestResult<T> restResult = new RestResult<>();
        restResult.setSuccess(true);
        restResult.setCode(SUCCESS);
        restResult.setData(data);
        return restResult;
    }

    /**
     * 创建失败的响应结果
     * <p>
     * 该方法用于创建表示请求处理失败的响应结果，设置状态码为500，success为false。
     * </p>
     *
     * @param message 失败提示信息
     * @param <T>     业务数据类型
     * @return 失败的响应结果实例
     */
    public static <T> RestResult<T> fail(String message) {
        RestResult<T> restResult = new RestResult<>();
        restResult.setSuccess(false);
        restResult.setCode(FAIL);
        restResult.setMessage(message);
        return restResult;
    }

    /**
     * 获取是否成功标识
     *
     * @return 是否成功标识
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置是否成功标识
     *
     * @param success 是否成功标识
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 获取响应状态码
     *
     * @return 响应状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置响应状态码
     *
     * @param code 响应状态码
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 获取响应业务数据
     *
     * @return 响应业务数据
     */
    public T getData() {
        return data;
    }

    /**
     * 设置响应业务数据
     *
     * @param data 响应业务数据
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * 获取失败提示信息
     *
     * @return 失败提示信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置失败提示信息
     *
     * @param message 失败提示信息
     */
    public void setMessage(String message) {
        this.message = message;
    }
}