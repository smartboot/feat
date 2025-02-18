package tech.smartboot.feat.ai.vector.chroma;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.vector.chroma.collection.Collection;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.HttpRest;
import tech.smartboot.feat.core.common.HttpMethod;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Chroma {
    private final HttpClient httpClient;
    private final Options options = new Options();

    public Chroma(String url, Consumer<Options> opt) {
        opt.accept(options);
        this.httpClient = Feat.httpClient(url, opts -> {
            opts.debug(options.isDebug());
        });
    }

    public Chroma(String url) {
        this(url, opt -> {
        });
    }

    /**
     * 获取版本信息
     *
     * @return 版本信息
     */
    public String version() {
        HttpGet httpGet = httpClient.get("/api/v1/version");
        return execute(httpGet, String.class);
    }

    public void createDatabase(String database) {
        createDatabase(options.defaultTenant(), database);
    }

    public void createDatabase(String tenant, String database) {
        Map<String, String> body = new HashMap<>();
        body.put("name", database);
        HttpPost httpPost;
        if (options.isV1()) {
            httpPost = httpClient.post("/api/v1/databases");
            httpPost.addQueryParam("tenant", tenant);
            httpPost.postJson(body);
        } else {
            httpPost = httpClient.post("/api/v2/tenants/" + tenant + "/databases").postJson(body);
        }

        execute(httpPost, String.class);
    }

    public String getDatabase(String database) {
        return getDatabase(options.defaultTenant(), database);
    }

    public String getDatabase(String tenant, String database) {
        HttpGet httpGet;
        if (options.isV1()) {
            httpGet = httpClient.get("/api/v1/databases/" + database);
            httpGet.addQueryParam("tenant", tenant);
        } else {
            httpGet = httpClient.get("/api/v2/tenants/" + tenant + "/databases/" + database);
        }

        execute(httpGet, String.class);
        return "aa";
    }

    public void deleteDatabase(String database) {
        deleteDatabase(options.defaultTenant(), database);
    }

    public void deleteDatabase(String tenant, String database) {
        HttpRest httpRest = httpClient.rest(HttpMethod.DELETE, "/api/v2/tenants/" + tenant + "/databases/" + database);
        execute(httpRest);
    }

    public boolean rest() {
        HttpPost httpPost = httpClient.post("/api/v2/reset");
        String rsp = execute(httpPost, String.class);
        return "true".equals(rsp);
    }

    public Collection createCollection(String collection, Map<String, String> metadata) {
        return createCollection(options.defaultTenant(), options.defaultDatabase(), collection, metadata);
    }

//    public Collection getCollection(String id) {
//        return getCollection(id, Request.of());
//    }
//
//    public Collection getCollection(String id, Request request) {
//        return getCollection(options.defaultTenant(), options.defaultDatabase(), id, request);
//    }
//
//    public Collection getCollection(String tenant, String database, String id, Request request) {
//        HttpPost http = httpClient.post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + id + "/get");
//        http.postJson(request);
//        return execute(http, Collection.class);
//    }

    public Collection getCollection(String name) {
        return getCollection(options.defaultTenant(), options.defaultDatabase(), name);
    }


    public Collection getCollection(String tenant, String database, String name) {
        HttpRest http = httpClient.get("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + name);
        Collection collection = execute(http, Collection.class);
        collection.setHttpClient(httpClient);
        return collection;
    }

    public List<Collection> getCollections(int offset, int limit) {
        return getCollections(options.defaultTenant(), options.defaultDatabase(), offset, limit);
    }

    public List<Collection> getCollections(String tenant, String database, int offset, int limit) {
        HttpGet http = httpClient.get("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections");
        http.addQueryParam("limit", limit);
        http.addQueryParam("offset", offset);
        return JSON.parseArray(execute(http), Collection.class);
    }

    public Collection createCollection(String tenant, String database, String collection, Map<String, String> metadata) {
        HttpPost httpPost = httpClient.post("/api/v2/tenants/" + tenant + "/databases/" + database + "/collections");
        Map<String, Object> body = new HashMap<>();
        body.put("name", collection);
        body.put("metadata", metadata);
        httpPost.postJson(body);
        Collection c = execute(httpPost, Collection.class);
        c.setHttpClient(httpClient);
        return c;
    }

    public static String execute(HttpRest rest) {
        return execute(rest, String.class);
    }

    public static <T> T execute(HttpRest rest, Class<T> clazz) {
        try {
            HttpResponse response = rest.submit().get();
            if (response.statusCode() >= 400) {
                ValidationError err = JSON.parseObject(response.body(), ValidationError.class);
                throw new FeatException(err.getMessage());
            }
            if (clazz == String.class) {
                return (T) response.body();
            }
            return JSON.parseObject(response.body(), clazz);
        } catch (FeatException e) {
            throw e;
        } catch (Exception e) {
            throw new FeatException(e);
        }
    }

    public Options options() {
        return options;
    }
}
