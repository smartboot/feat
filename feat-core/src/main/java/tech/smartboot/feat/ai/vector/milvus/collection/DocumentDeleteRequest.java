package tech.smartboot.feat.ai.vector.milvus.collection;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentDeleteRequest {
    private List<String> ids;
    private Map<String, String> where;
    @JSONField(name = "where_document")
    private Map<String, String> whereDocument;

    public void where(String key, String value) {
        if (where == null) {
            where = new HashMap<>();
        }
        where.put(key, value);
    }

    public void whereDocument(String key, String value) {
        if (whereDocument == null) {
            whereDocument = new HashMap<>();
        }
        whereDocument.put(key, value);
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
