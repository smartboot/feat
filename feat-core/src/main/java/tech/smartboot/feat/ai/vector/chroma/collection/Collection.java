package tech.smartboot.feat.ai.vector.chroma.collection;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.vector.chroma.Chroma;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.HttpMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Collection {
    private String id;
    private String name;
    private String tenant;
    private String database;
    private String collection;
    private HttpClient httpClient;

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

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public int count() {
        HttpGet http = httpClient.get("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/count");
        return Chroma.execute(http, int.class);
    }

    /**
     * 获取集合信息
     */
    public void get() {
        get(new Request());
    }

    public void delete() {
        HttpRest http = httpClient.rest(HttpMethod.DELETE, "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + name);
        Chroma.execute(http);
    }

    public void get(Request request) {
        HttpPost httpPost = httpClient.post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/get");
        httpPost.postJson(request);
        Chroma.execute(httpPost);
    }

    /**
     * 更新文档，若不存在则忽略
     * @param document
     */
    public void upsert(Document document) {
        update(Collections.singletonList(document));
    }

    public void upsert(List<Document> document) {
        HttpPost httpPost = httpClient.post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/upsert");
        JSONObject object = toVector(document);
        httpPost.postJson(object);
        Chroma.execute(httpPost);
    }

    /**
     * 更新文档，若不存在则忽略
     * @param document
     */
    public void update(Document document) {
        update(Collections.singletonList(document));
    }

    public void update(List<Document> document) {
        HttpPost httpPost = httpClient.post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/update");
        JSONObject object = toVector(document);
        httpPost.postJson(object);
        Chroma.execute(httpPost);
    }

    public boolean add(Document document) {
        return add(Collections.singletonList(document));
    }

    public boolean add(List<Document> document) {
        HttpPost httpPost = httpClient.post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/add");
        JSONObject object = toVector(document);
        httpPost.postJson(object);
        return Chroma.execute(httpPost, boolean.class);
    }

    private static JSONObject toVector(List<Document> document) {
        List<String> ids = new ArrayList<>();
        List<Map<String, String>> metadatas = new ArrayList<>();
        List<List<Float>> embeddings = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        for (Document doc : document) {
            ids.add(doc.getId());
            metadatas.add(doc.getMetadata());
//            embeddings.add(doc.getEmbedding());
            documents.add(doc.getDocument());
        }
        JSONObject object = new JSONObject();
        object.put("ids", ids);
        object.put("metadata", metadatas);
        object.put("documents", documents);
        return object;
    }
}
