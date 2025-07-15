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

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
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
