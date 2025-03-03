/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.vector.milvus;

import tech.smartboot.feat.ai.embedding.EmbeddingModel;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
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
