package tech.smartboot.feat.ai.vector.milvus;

import tech.smartboot.feat.ai.embedding.EmbeddingModel;

public class MilvusOptions {
    private boolean debug;
    protected EmbeddingModel embeddingModel;
    private String defaultDatabase = "default";
    private String token;

    public MilvusOptions debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public String defaultDatabase() {
        return defaultDatabase;
    }

    public MilvusOptions defaultDatabase(String defaultDatabase) {
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

    public MilvusOptions embeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        return this;
    }

    public String getToken() {
        return token;
    }

    public MilvusOptions setToken(String token) {
        this.token = token;
        return this;
    }
}
