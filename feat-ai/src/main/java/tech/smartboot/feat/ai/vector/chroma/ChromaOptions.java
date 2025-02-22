package tech.smartboot.feat.ai.vector.chroma;

import tech.smartboot.feat.ai.embedding.EmbeddingModel;

public class ChromaOptions {
    public static final String API_VERSION_1 = "v1";
    public static final String API_VERSION_2 = "v2";
    private boolean debug;
    protected EmbeddingModel embeddingModel;
    private String apiVersion = API_VERSION_2;
    private String defaultTenant = "default_tenant";
    private String defaultDatabase = "default_database";

    public ChromaOptions debug(boolean debug) {
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

    public ChromaOptions defaultTenant(String defaultTenant) {
        this.defaultTenant = defaultTenant;
        return this;
    }

    public String defaultDatabase() {
        return defaultDatabase;
    }

    public ChromaOptions defaultDatabase(String defaultDatabase) {
        this.defaultDatabase = defaultDatabase;
        return this;
    }

    public final EmbeddingModel embeddingModel() {
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel not set");
        }
        return embeddingModel;
    }

    public final boolean isDebug() {
        return debug;
    }

    public ChromaOptions embeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        return this;
    }
}
