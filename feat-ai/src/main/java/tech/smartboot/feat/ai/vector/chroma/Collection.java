/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.vector.chroma;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.vector.Document;
import tech.smartboot.feat.ai.vector.chroma.collection.DocumentDeleteRequest;
import tech.smartboot.feat.ai.vector.chroma.collection.Query;
import tech.smartboot.feat.ai.vector.chroma.collection.Request;
import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HttpMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Collection {
    private String id;
    private String name;
    private String tenant;
    private String database;
    private String collection;
    private Chroma chroma;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public void setChroma(Chroma chroma) {
        this.chroma = chroma;
    }

    public int count() {
        HttpGet http = chroma.getHttpClient().get("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/count");
        return Chroma.execute(http, int.class);
    }

    /**
     * 获取集合信息
     */
    public void get() {
        get(new Request());
    }

    public void delete() {
        HttpRest http = chroma.getHttpClient().rest(HttpMethod.DELETE, "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + name);
        Chroma.execute(http);
    }

    public void get(Request request) {
        HttpPost httpPost = chroma.getHttpClient().post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/get");
        httpPost.postJson(request);
        Chroma.execute(httpPost);
    }

    /**
     * 更新文档，若不存在则忽略
     *
     * @param document
     */
    public void upsert(Document document) {
        update(Collections.singletonList(document));
    }

    public void upsert(List<Document> document) {
        HttpPost httpPost = chroma.getHttpClient().post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/upsert");
        JSONObject object = toVector(document);
        httpPost.postJson(object);
        Chroma.execute(httpPost);
    }

    /**
     * 更新文档，若不存在则忽略
     *
     * @param document
     */
    public void update(Document document) {
        update(Collections.singletonList(document));
    }

    public void update(List<Document> document) {
        HttpPost httpPost = chroma.getHttpClient().post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/update");
        JSONObject object = toVector(document);
        httpPost.postJson(object);
        Chroma.execute(httpPost);
    }

    public boolean add(Document document) {
        return add(Collections.singletonList(document));
    }

    public boolean add(List<Document> document) {
        HttpPost httpPost = chroma.getHttpClient().post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/add");
        JSONObject object = toVector(document);
        httpPost.postJson(object);
        return Chroma.execute(httpPost, boolean.class);
    }

    public void delete(String id) {
        delete(Collections.singletonList(id));
    }

    public void delete(List<String> idList) {
        DocumentDeleteRequest request = new DocumentDeleteRequest();
        request.setIds(idList);
        delete(request);
    }

    public void delete(DocumentDeleteRequest request) {
        HttpPost httpPost = chroma.getHttpClient().post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/delete");
        httpPost.postJson(request);
        Chroma.execute(httpPost);
    }

    public void query(Query query) {
        HttpPost httpPost = chroma.getHttpClient().post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/query");
        // 若queryTexts不为空，则将其转换为embeddings
        if (FeatUtils.isNotEmpty(query.getQueryTexts())) {
            query.setQueryEmbeddings(chroma.options().embeddingModel().embed(query.getQueryTexts()));
        }
        httpPost.postJson(query);
        Chroma.execute(httpPost);
    }

    private JSONObject toVector(List<Document> document) {
        List<String> ids = new ArrayList<>();
        List<Map<String, String>> metadatas = new ArrayList<>();
        List<float[]> embeddings = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        for (Document doc : document) {
            ids.add(doc.getId());
            metadatas.add(doc.getMetadata());
            embeddings.add(chroma.options().embeddingModel().embed(doc.getDocument()));
            documents.add(doc.getDocument());
        }
        JSONObject object = new JSONObject();
        object.put("ids", ids);
        object.put("metadatas", metadatas);
        object.put("documents", documents);
        object.put("embeddings", embeddings);
        return object;
    }
}
