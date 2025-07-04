/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.server;

import com.alibaba.fastjson2.JSONObject;

/**
 * @author 三刀
 * @version v1.0 7/4/25
 */
public class McpServerException extends RuntimeException {
    public static final int RESOURCE_NOT_FOUND = -32002;
    /**
     * Invalid Parameters
     */
    public static final int INTERNAL_ERROR = -32602;
    private int code;
    private JSONObject data;

    public McpServerException(int code, String message) {
        this(code, message, null);
    }

    public McpServerException(int code, String message, JSONObject data) {
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
