/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai;

import tech.smartboot.feat.core.client.HttpOptions;

import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 8/7/25
 */
public abstract class Options {
    public static final String ENV_API_KEY = "FEAT_AI_API_KEY";
    /**
     * AI服务的基础URL，优先从环境变量FEAT_AI_BASE_URL获取，默认使用Gitee AI服务
     */
    private String baseUrl = System.getenv("FEAT_AI_BASE_URL") != null ? System.getenv("FEAT_AI_BASE_URL") : "https://ai.gitee.com/v1/";


    /**
     * API密钥，优先从环境变量FEATAI_API_KEY获取
     */
    private String apiKey = System.getenv(ENV_API_KEY);

    /**
     * 是否启用调试模式
     */
    private boolean debug;

    private Consumer<HttpOptions> httpOptions;

    public final String baseUrl() {
        return baseUrl;
    }

    public Options baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public final String apiKey() {
        return apiKey;
    }

    public Options apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public final boolean isDebug() {
        return debug;
    }

    public Options debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    protected Options httpOptions(Consumer<HttpOptions> httpOptions) {
        this.httpOptions = httpOptions;
        return this;
    }

    public Consumer<HttpOptions> getHttpOptions() {
        return httpOptions;
    }
}
