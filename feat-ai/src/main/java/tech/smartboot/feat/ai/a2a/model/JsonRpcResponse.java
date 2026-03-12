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

/**
 * A2A JSON-RPC 响应类
 *
 * <p>基于JSON-RPC 2.0协议的响应封装。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class JsonRpcResponse<T> {
    /**
     * JSON-RPC版本
     */
    private String jsonrpc = "2.0";

    /**
     * 请求ID
     */
    private String id;

    /**
     * 响应结果
     */
    private T result;

    /**
     * 错误信息
     */
    private JsonRpcError error;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    /**
     * 检查响应是否成功
     *
     * @return 如果响应成功则返回true
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * 创建成功的响应
     *
     * @param id     请求ID
     * @param result 结果
     * @return JsonRpcResponse实例
     */
    public static <T> JsonRpcResponse<T> success(String id, T result) {
        JsonRpcResponse<T> response = new JsonRpcResponse<>();
        response.setId(id);
        response.setResult(result);
        return response;
    }

    /**
     * 创建错误的响应
     *
     * @param id    请求ID
     * @param code  错误码
     * @param message 错误消息
     * @return JsonRpcResponse实例
     */
    public static <T> JsonRpcResponse<T> error(String id, int code, String message) {
        JsonRpcResponse<T> response = new JsonRpcResponse<>();
        response.setId(id);
        JsonRpcError error = new JsonRpcError();
        error.setCode(code);
        error.setMessage(message);
        response.setError(error);
        return response;
    }
}
