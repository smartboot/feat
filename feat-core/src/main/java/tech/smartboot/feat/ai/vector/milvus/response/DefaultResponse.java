package tech.smartboot.feat.ai.vector.milvus.response;

import com.alibaba.fastjson2.JSONObject;

public class DefaultResponse extends Response {
    private JSONObject data;

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }
}
