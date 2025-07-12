/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.model;

import com.alibaba.fastjson2.JSONObject;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class Response<T> extends JsonRpc {
    private T result;
    private JSONObject error;

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public JSONObject getError() {
        return error;
    }

    public void setError(JSONObject error) {
        this.error = error;
    }
}