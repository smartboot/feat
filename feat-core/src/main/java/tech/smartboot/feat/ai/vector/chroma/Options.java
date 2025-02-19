package tech.smartboot.feat.ai.vector.chroma;

import tech.smartboot.feat.ai.embedding.EmbeddingModel;

public class Options {
    public static final String API_VERSION_1 = "v1";
    public static final String API_VERSION_2 = "v2";
    private boolean debug;
    private String apiVersion = API_VERSION_2;
    private String defaultTenant = "default_tenant";
    private String defaultDatabase = "default_database";
    private EmbeddingModel embeddingModel;

    public boolean isDebug() {
        return debug;
    }

    public Options debug(boolean debug) {
        this.debug = debug;
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

    public Options defaultTenant(String defaultTenant) {
        this.defaultTenant = defaultTenant;
        return this;
    }

    public String defaultDatabase() {
        return defaultDatabase;
    }

    public Options defaultDatabase(String defaultDatabase) {
        this.defaultDatabase = defaultDatabase;
        return this;
    }

    EmbeddingModel getEmbeddingModel() {
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel not set");
        }
        return embeddingModel;
    }

    public Options embeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        return this;
    }
}
