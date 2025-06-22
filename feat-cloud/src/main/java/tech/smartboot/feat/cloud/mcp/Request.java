package tech.smartboot.feat.cloud.mcp;

/**
 * @author 三刀
 * @version v1.0 6/18/25
 */
public class Request<T> extends JsonRpc {
    private String method;

    private T params;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public T getParams() {
        return params;
    }

    public void setParams(T params) {
        this.params = params;
    }
}