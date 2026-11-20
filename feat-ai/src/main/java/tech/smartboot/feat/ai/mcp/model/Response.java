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

/**
 * JSON-RPC 2.0 应答对象：result 与 error 互斥，两者都不会出现或只出现其一。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 6/18/25
 */
public class Response<T> extends JsonRpc {
    private T result;
    private JsonRpcError error;

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }
}