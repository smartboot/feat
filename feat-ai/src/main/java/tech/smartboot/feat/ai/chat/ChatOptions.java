/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.Options;
import tech.smartboot.feat.ai.chat.entity.Function;
import tech.smartboot.feat.ai.chat.provider.OpenAiProvider;
import tech.smartboot.feat.ai.chat.provider.Provider;
import tech.smartboot.feat.core.client.HttpOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 聊天选项类，继承自Options，用于配置聊天模型的各种参数
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatOptions extends Options {
    /**
     * 系统提示信息
     */
    private String system;

    /**
     * 功能函数映射，用于存储可用的AI功能函数
     */
    private final Map<String, Function> functions = new HashMap<>();

    /**
     * extra body parameters for model-specific options
     */
    private JSONObject extraBody;

    /**
     * 模型名称
     */
    private String model;

    /**
     * API 规范类型，默认为 OPENAI
     */
    private java.util.function.Function<ChatOptions, Provider> provider = OpenAiProvider::new;


    /**
     * 设置基础URL
     *
     * @param baseUrl 基础URL
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions baseUrl(String baseUrl) {
        super.baseUrl(baseUrl);
        return this;
    }

    /**
     * 设置模型供应商
     *
     * @param model 模型供应商
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions model(String model) {
        this.model = model;
        return this;
    }

    /**
     * 设置API密钥
     *
     * @param apiKey API密钥
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions apiKey(String apiKey) {
        super.apiKey(apiKey);
        return this;
    }

    /**
     * 获取功能函数映射
     *
     * @return 功能函数映射
     */
    public Map<String, Function> functions() {
        return functions;
    }

    /**
     * 添加功能函数
     *
     * @param function 功能函数
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions addFunction(Function function) {
        this.functions.put(function.getName(), function);
        return this;
    }

    /**
     * 获取系统提示信息
     *
     * @return 系统提示信息
     */
    public String getSystem() {
        return system;
    }

    /**
     * 设置系统提示信息
     *
     * @param system 系统提示信息
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions system(String system) {
        this.system = system;
        return this;
    }

    /**
     * 设置调试模式
     *
     * @param debug 是否开启调试模式
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions debug(boolean debug) {
        super.debug(debug);
        return this;
    }

    @SafeVarargs
    public final ChatOptions extraBody(Consumer<JSONObject>... consumer) {
        if (extraBody == null) {
            this.extraBody = new JSONObject();
        }
        for (Consumer<JSONObject> c : consumer) {
            c.accept(this.extraBody);
        }
        return this;
    }

    /**
     * 获取 extra body 参数映射
     *
     * @return 参数映射
     */
    public JSONObject getExtraBody() {
        return extraBody;
    }

    /**
     * 获取模型供应商
     *
     * @return 模型供应商
     */
    public String getModel() {
        return model;
    }

    /**
     * 获取 API 规范类型
     *
     * @return API 规范类型
     */
    java.util.function.Function<ChatOptions, Provider> getProvider() {
        return provider;
    }

    /**
     * 设置 API 规范类型
     *
     * @param provider API 规范类型
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions specProvider(java.util.function.Function<ChatOptions, Provider> provider) {
        this.provider = provider;
        return this;
    }

    public ChatOptions httpOptions(Consumer<HttpOptions> httpOptions) {
        super.httpOptions(httpOptions);
        return this;
    }
}
