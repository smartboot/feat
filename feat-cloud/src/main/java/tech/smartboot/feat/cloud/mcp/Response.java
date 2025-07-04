package tech.smartboot.feat.cloud.mcp;

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