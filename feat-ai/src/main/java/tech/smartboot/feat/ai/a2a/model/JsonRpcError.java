/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.model;

import com.alibaba.fastjson2.JSONObject;

/**
 * A2A JSON-RPC 错误类
 *
 * <p>描述JSON-RPC协议中的错误信息。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class JsonRpcError {
    /**
     * 解析错误
     */
    public static final int PARSE_ERROR = -32700;

    /**
     * 无效请求
     */
    public static final int INVALID_REQUEST = -32600;

    /**
     * 方法未找到
     */
    public static final int METHOD_NOT_FOUND = -32601;

    /**
     * 无效参数
     */
    public static final int INVALID_PARAMS = -32602;

    /**
     * 内部错误
     */
    public static final int INTERNAL_ERROR = -32603;

    /**
     * 任务未找到
     */
    public static final int TASK_NOT_FOUND = -32001;

    /**
     * 任务已存在
     */
    public static final int TASK_ALREADY_EXISTS = -32002;

    /**
     * 认证失败
     */
    public static final int UNAUTHORIZED = -32003;

    /**
     * 权限不足
     */
    public static final int FORBIDDEN = -32004;

    /**
     * 错误码
     */
    private int code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误数据
     */
    private JSONObject data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }
}
