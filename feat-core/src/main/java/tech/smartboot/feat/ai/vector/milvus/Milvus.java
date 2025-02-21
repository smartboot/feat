package tech.smartboot.feat.ai.vector.milvus;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.vector.milvus.collection.CreateRequest;
import tech.smartboot.feat.ai.vector.milvus.response.CollectionListResponse;
import tech.smartboot.feat.ai.vector.milvus.response.DatabaseListResponse;
import tech.smartboot.feat.ai.vector.milvus.response.DefaultResponse;
import tech.smartboot.feat.ai.vector.milvus.response.Response;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Milvus {
    private final HttpClient httpClient;
    private final MilvusOptions options = new MilvusOptions();

    public Milvus(String url, Consumer<MilvusOptions> opt) {
        opt.accept(options);
        this.httpClient = Feat.httpClient(url, opts -> {
            opts.debug(options.isDebug());
        });
    }

    public Milvus(String url) {
        this(url, opt -> {
        });
    }

    public void createDatabase(String database) {
        Map<String, String> body = new HashMap<>();
        body.put("dbName", database);
        HttpPost httpPost = httpClient.post("/v2/vectordb/databases/create");
        httpPost.postJson(body);

        execute(httpPost, DefaultResponse.class);
    }

    /**
     * This operation drops the specified database.
     * @param database
     */
    public void deleteDatabase(String database) {
        HttpPost httpRest = httpClient.post("/v2/vectordb/databases/drop");
        httpRest.postJson(Collections.singletonMap("dbName", database));
        execute(httpRest);
    }

    public List<String> listDatabase() {
        HttpPost httpRest = httpClient.post("/v2/vectordb/databases/list");
        httpRest.postJson(null);
        DatabaseListResponse response = execute(httpRest, DatabaseListResponse.class);
        return response.getData();
    }


    public Collection getCollection(String name) {
        return getCollection(options.defaultDatabase(), name);
    }


    public Collection getCollection(String database, String name) {
        HttpPost http = httpClient.post("/v2/vectordb/collections/has");
        Map<String, String> body = new HashMap<>();
        body.put("dbName", database);
        body.put("collectionName", name);
        http.postJson(body);
        DefaultResponse response = execute(http, DefaultResponse.class);
        if (response.getData() != null && response.getData().getBoolean("has")) {
            return new Collection(name, database, this);
        }
        return null;
    }

    public List<Collection> getCollections() {
        return getCollections(options.defaultDatabase());
    }

    public List<Collection> getCollections(String database) {
        HttpPost http = httpClient.post("/v2/vectordb/collections/list");
        http.postJson(Collections.singletonMap("dbName", database));
        List<String> data = execute(http, CollectionListResponse.class).getData();
        if (Collections.emptyList().equals(data)) {
            return Collections.emptyList();
        }
        List<Collection> collections = new ArrayList<>(data.size());
        for (String name : data) {
            Collection collection = new Collection(name, database, this);
            collections.add(collection);
        }
        return collections;
    }

    public Collection createCollection(CreateRequest request) {
        HttpPost httpPost = httpClient.post("/v2/vectordb/collections/create");
        httpPost.postJson(request);
        execute(httpPost);
        return null;
    }

    public Collection createCollection(String collection) {
        CreateRequest request = new CreateRequest(collection);
        return createCollection(request);
    }

    public static void execute(HttpRest rest) {
        Response response = execute(rest, Response.class);
        if (response.getCode() != 0) {
            throw new FeatException(response.getMessage());
        }
    }

    public static <T extends Response> T execute(HttpRest rest, Class<T> clazz) {
        try {
            HttpResponse response = rest.submit().get();
            if (response.statusCode() >= 400) {
                Response err = JSON.parseObject(response.body(), Response.class);
                throw new FeatException(err.getMessage());
            }
            T t = JSON.parseObject(response.body(), clazz);
            if (t.getCode() != 0) {
                throw new FeatException(t.getMessage());
            }
            return t;
        } catch (FeatException e) {
            throw e;
        } catch (Exception e) {
            throw new FeatException(e);
        }
    }

    HttpClient getHttpClient() {
        return httpClient;
    }

    public MilvusOptions options() {
        return options;
    }
}
