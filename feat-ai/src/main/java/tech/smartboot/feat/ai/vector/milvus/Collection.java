/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.vector.milvus;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.vector.Document;
import tech.smartboot.feat.ai.vector.MilvusVectorStore;
import tech.smartboot.feat.ai.vector.expression.Expression;
import tech.smartboot.feat.ai.vector.milvus.collection.HybridSearch;
import tech.smartboot.feat.ai.vector.milvus.response.CollectionInsertResponse;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.utils.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class Collection {
    private final String name;
    private final String database;
    private final Milvus milvus;

    public Collection(String name, String database, Milvus chroma) {
        this.name = name;
        this.database = database;
        this.milvus = chroma;
    }


    public String getName() {
        return name;
    }


    public void drop() {
        HttpPost http = milvus.getHttpClient().post("/v2/vectordb/collections/drop");
        Map<String, String> body = new HashMap<>();
        body.put("dbName", database);
        body.put("collectionName", name);
        http.postJson(body);
        Milvus.execute(http);
    }


    public boolean add(Document document) {
        return add(Collections.singletonList(document));
    }

    public boolean add(List<Document> document) {
        HttpPost httpPost = milvus.getHttpClient().post("/v2/vectordb/entities/insert");
        JSONObject body = new JSONObject();
        body.put("dbName", database);
        body.put("collectionName", name);
        body.put("data", toVectors(document));
        httpPost.postJson(body);
        Milvus.execute(httpPost, CollectionInsertResponse.class);
        return true;
    }

    public void delete(long id) {
        delete(Expression.of("id").eq(id));
    }

    public void delete(String id) {
        delete(Expression.of("id").eq(id));
    }

    public void delete(long[] idList) {
        delete(Expression.of("id").in(idList));
    }

    public void delete(String[] idList) {
        delete(Expression.of("id").in(idList));
    }

    public void delete(Expression request) {
        StringBuilder sb = new StringBuilder();
        request.build(sb, MilvusVectorStore.convert);
        HttpPost httpPost = milvus.getHttpClient().post("/v2/vectordb/entities/delete");
        Map<String, String> body = new HashMap<>();
        body.put("dbName", database);
        body.put("collectionName", name);
        body.put("filter", sb.toString());
        httpPost.postJson(body);
        Milvus.execute(httpPost);
    }

    public void query(HybridSearch query) {
//        HttpPost httpPost = milvus.getHttpClient().post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/query");
//        // 若queryTexts不为空，则将其转换为embeddings
//        if (CollectionUtils.isNotEmpty(query.getQueryTexts())) {
//            query.setQueryEmbeddings(milvus.options().embeddingModel().embed(query.getQueryTexts()));
//        }
//        httpPost.postJson(query);
//        Milvus.execute(httpPost);
    }

    private JSONArray toVectors(List<Document> document) {
        JSONArray array = new JSONArray();
        for (Document d : document) {
            JSONObject object = new JSONObject();
            object.put("id", d.getId());
            object.put("document", d.getDocument());
            object.put("vector", milvus.options().embeddingModel().embed(d.getDocument()));
            object.putAll(d.getMetadata());
            array.add(object);
        }
        return array;
    }
}
