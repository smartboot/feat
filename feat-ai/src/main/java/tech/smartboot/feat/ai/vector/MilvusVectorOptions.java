package tech.smartboot.feat.ai.vector;

import tech.smartboot.feat.ai.embedding.EmbeddingModel;

public class MilvusVectorOptions extends VectorOptions {
    public static final String API_VERSION_1 = "v1";
    public static final String API_VERSION_2 = "v2";

    private String apiVersion = API_VERSION_2;
    private String defaultTenant = "default_tenant";
    private String defaultDatabase = "default_database";
    private String url;

    public MilvusVectorOptions debug(boolean debug) {
        super.debug(debug);
        return this;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public boolean isV1() {
        return API_VERSION_1.equals(apiVersion);
    }

    public String defaultTenant() {
        return defaultTenant;
    }

    public MilvusVectorOptions defaultTenant(String defaultTenant) {
        this.defaultTenant = defaultTenant;
        return this;
    }

    public String defaultDatabase() {
        return defaultDatabase;
    }

    public MilvusVectorOptions defaultDatabase(String defaultDatabase) {
        this.defaultDatabase = defaultDatabase;
        return this;
    }


    public MilvusVectorOptions embeddingModel(EmbeddingModel embeddingModel) {
        super.embeddingModel(embeddingModel);
        return this;
    }

    public String getUrl() {
        return url;
    }

    public MilvusVectorOptions setUrl(String url) {
        this.url = url;
        return this;
    }
}
