/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai;

import tech.smartboot.feat.ai.chat.entity.Function;
import tech.smartboot.feat.ai.chat.entity.ResponseFormat;
import tech.smartboot.feat.ai.vendor.GiteeAI;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Options {
    public static final String ENV_API_KEY = "FEATAI_API_KEY";
    /**
     * Gitee AI服务的基础URL
     */
    public static final String AI_VENDOR_GITEE = GiteeAI.BASE_URL;

    /**
     * AI服务的基础URL，优先从环境变量FEATAI_BASE_URL获取，默认使用Gitee AI服务
     */
    private String baseUrl = System.getenv("FEATAI_BASE_URL") != null ? System.getenv("FEAT_AI_BASE_URL") : GiteeAI.BASE_URL;

    /**
     * AI模型名称
     */
    private String model;

    /**
     * API密钥，优先从环境变量FEATAI_API_KEY获取
     */
    private String apiKey = System.getenv(ENV_API_KEY);

    /**
     * 系统提示信息
     */
    private String system;

    /**
     * 是否启用调试模式
     */
    private boolean debug;

    /**
     * 是否忽略不支持的工具
     * 当设置为true时，遇到不支持的工具会跳过而不是抛出异常
     */
    private boolean ignoreUnSupportedTool = false;

    /**
     * 请求头信息映射
     */
    private final Map<String, String> headers = new HashMap<>();

    /**
     * 功能函数映射，用于存储可用的AI功能函数
     */
    private final Map<String, Function> functions = new HashMap<>();

    private ResponseFormat responseFormat;


    public String baseUrl() {
        return baseUrl;
    }

    public Options baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String getModel() {
        return model;
    }

    public Options model(String model) {
        this.model = model;
        return this;
    }

    public Options model(ModelMeta model) {
        return baseUrl(model.getVendor()).model(model.getModel());
    }

    public String getApiKey() {
        return apiKey;
    }

    public Options apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public Map<String, Function> functions() {
        return functions;
    }

    public Options addFunction(Function function) {
        this.functions.put(function.getName(), function);
        return this;
    }

    public String getSystem() {
        return system;
    }

    public Options system(String system) {
        this.system = system;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public Options debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isIgnoreUnSupportedTool() {
        return ignoreUnSupportedTool;
    }

    public Options ignoreUnSupportedTool(boolean ignoreUnSupportedTool) {
        this.ignoreUnSupportedTool = ignoreUnSupportedTool;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Options addHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public Options responseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
        return this;
    }

    public ResponseFormat responseFormat() {
        return responseFormat;
    }
}
