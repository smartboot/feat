package tech.smartboot.feat.ai.vector.milvus.response;

import java.util.List;

public class CollectionListResponse extends Response {
    private List<String> data;

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }
}
