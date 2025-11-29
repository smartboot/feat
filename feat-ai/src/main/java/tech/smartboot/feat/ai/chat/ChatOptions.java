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

import tech.smartboot.feat.ai.Options;
import tech.smartboot.feat.ai.chat.entity.Function;
import tech.smartboot.feat.ai.chat.entity.ResponseFormat;

import java.util.HashMap;
import java.util.Map;

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
     * 响应格式配置
     */
    private ResponseFormat responseFormat;

    /**
     * 是否不进行思考（直接输出结果）
     */
    private boolean noThink;

    /**
     * 模型供应商
     */
    private ChatModelVendor model;

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
    public ChatOptions model(ChatModelVendor model) {
        this.model = model;
        return baseUrl(model.baseUrl());
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

    /**
     * 添加请求头
     *
     * @param key   请求头键
     * @param value 请求头值
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions addHeader(String key, String value) {
        super.addHeader(key, value);
        return this;
    }

    /**
     * 设置响应格式
     *
     * @param responseFormat 响应格式
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions responseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
        return this;
    }

    /**
     * 获取响应格式
     *
     * @return 响应格式
     */
    public ResponseFormat responseFormat() {
        return responseFormat;
    }

    /**
     * 设置是否不进行思考（直接输出结果）
     *
     * @param noThink 是否不进行思考
     * @return 当前ChatOptions实例，用于链式调用
     */
    public ChatOptions noThink(boolean noThink) {
        this.noThink = noThink;
        return this;
    }

    /**
     * 判断是否不进行思考（直接输出结果）
     *
     * @return 如果不进行思考返回true，否则返回false
     */
    public boolean isNoThink() {
        return noThink;
    }

    /**
     * 获取模型供应商
     *
     * @return 模型供应商
     */
    public ChatModelVendor getModel() {
        return model;
    }
}