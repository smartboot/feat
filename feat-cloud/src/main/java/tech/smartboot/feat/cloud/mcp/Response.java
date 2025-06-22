package tech.smartboot.feat.cloud.mcp;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class Response<T> extends JsonRpc {
    private T result;

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}