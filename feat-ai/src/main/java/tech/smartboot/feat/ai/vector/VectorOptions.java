package tech.smartboot.feat.ai.vector;

import tech.smartboot.feat.ai.embedding.EmbeddingModel;

public abstract class VectorOptions {
    private boolean debug;
    protected EmbeddingModel embeddingModel;
    private String collectionName;

    public final EmbeddingModel embeddingModel() {
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel not set");
        }
        return embeddingModel;
    }

    public final boolean isDebug() {
        return debug;
    }

    public VectorOptions debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public VectorOptions embeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        return this;
    }

    public VectorOptions collectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }

    public final String getCollectionName() {
        return collectionName;
    }
}
