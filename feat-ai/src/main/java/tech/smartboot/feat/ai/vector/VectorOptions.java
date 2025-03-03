/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

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
