/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 函数参数类，定义函数的参数结构和属性
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class FunctionParameters {
    /**
     * 参数类型，默认为"object"
     */
    private String type = "object";

    /**
     * 参数属性映射，键为参数名，值为参数属性定义
     */
    private Map<String, ParameterProperty> properties = new HashMap<>();

    /**
     * 必填参数集合
     */
    private Set<String> required = new HashSet<>();

    /**
     * 获取参数类型
     *
     * @return 参数类型字符串
     */
    public String getType() {
        return type;
    }

    /**
     * 设置参数类型
     *
     * @param type 参数类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取参数属性映射
     *
     * @return 参数属性映射
     */
    public Map<String, ParameterProperty> getProperties() {
        return properties;
    }

    /**
     * 添加参数定义
     *
     * @param name        参数名称
     * @param description 参数描述
     * @param type        参数类型
     * @param required    是否必填
     */
    public void addParameter(String name, String description, String type, boolean required) {
        this.properties.put(name, new ParameterProperty(type, description));
        if (required) {
            this.required.add(name);
        }
    }

    /**
     * 获取必填参数集合
     *
     * @return 必填参数集合
     */
    public Set<String> getRequired() {
        return required;
    }

    /**
     * 设置必填参数集合
     *
     * @param required 必填参数集合
     */
    public void setRequired(Set<String> required) {
        this.required = required;
    }
}