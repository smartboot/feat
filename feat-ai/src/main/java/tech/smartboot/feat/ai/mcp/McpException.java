/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp;

import com.alibaba.fastjson2.JSONObject;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 7/4/25
 */
public class McpException extends RuntimeException {
    /**
     * 资源不存在。2026-07-28 起改用 JSON-RPC 标准错误码 INVALID_PARAMS（-32602），见 SEP-2164。
     *
     * @deprecated 请使用 {@link #INVALID_PARAMS}
     */
    @Deprecated
    public static final int RESOURCE_NOT_FOUND = -32602;
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    /**
     * Invalid Parameters
     */
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    /**
     * 服务端需要用户提供额外信息才能继续，携带 url 模式的 elicitation 列表。
     */
    public static final int URL_ELICITATION_REQUIRED = -32042;
    private int code;
    private JSONObject data;

    public McpException(int code, String message) {
        this(code, message, null);
    }

    public McpException(int code, String message, JSONObject data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }
}
