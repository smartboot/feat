/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat;

import tech.smartboot.feat.ai.AbstractOptions;
import tech.smartboot.feat.ai.chat.entity.Function;
import tech.smartboot.feat.ai.chat.entity.ResponseFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatOptions extends AbstractOptions {

    /**
     * 系统提示信息
     */
    private String system;


    /**
     * 是否忽略不支持的工具
     * 当设置为true时，遇到不支持的工具会跳过而不是抛出异常
     */
    private boolean ignoreUnSupportedTool = false;


    /**
     * 功能函数映射，用于存储可用的AI功能函数
     */
    private final Map<String, Function> functions = new HashMap<>();

    private ResponseFormat responseFormat;


    public ChatOptions baseUrl(String baseUrl) {
        super.baseUrl(baseUrl);
        return this;
    }


    public ChatOptions model(String model) {
        super.model(model);
        return this;
    }

    public ChatOptions model(ModelVendor model) {
        return baseUrl(model.getBaseUrl()).model(model.getModel());
    }


    public ChatOptions apiKey(String apiKey) {
        super.apiKey(apiKey);
        return this;
    }

    public Map<String, Function> functions() {
        return functions;
    }

    public ChatOptions addFunction(Function function) {
        this.functions.put(function.getName(), function);
        return this;
    }

    public String getSystem() {
        return system;
    }

    public ChatOptions system(String system) {
        this.system = system;
        return this;
    }


    public ChatOptions debug(boolean debug) {
        super.debug(debug);
        return this;
    }

    public boolean isIgnoreUnSupportedTool() {
        return ignoreUnSupportedTool;
    }

    public ChatOptions ignoreUnSupportedTool(boolean ignoreUnSupportedTool) {
        this.ignoreUnSupportedTool = ignoreUnSupportedTool;
        return this;
    }

    public ChatOptions addHeader(String key, String value) {
        super.addHeader(key, value);
        return this;
    }

    public ChatOptions responseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
        return this;
    }

    public ResponseFormat responseFormat() {
        return responseFormat;
    }
}
