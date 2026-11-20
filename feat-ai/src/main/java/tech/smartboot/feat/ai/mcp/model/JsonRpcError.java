/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.model;

import com.alibaba.fastjson2.JSONObject;

/**
 * JSON-RPC 2.0 响应中的 error 对象。
 * <p>
 * 规范要求：code 必须为整数，message 必须为字符串；data 为可选的附加信息，
 * 值为 null 时序列化输出中省略该成员，不得输出 {@code "data": null}。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public class JsonRpcError {
    private int code;
    private String message;
    private JSONObject data;

    public JsonRpcError() {
    }

    public JsonRpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public JsonRpcError(int code, String message, JSONObject data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

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
