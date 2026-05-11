/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 函数类，表示AI模型可以调用的函数定义
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Tool {
    /**
     * 函数名称
     */
    private String name;

    /**
     * 函数描述
     */
    private String description;

    /**
     * 函数参数定义
     */
    private Parameters parameters;

    /**
     * 构造函数
     *
     * @param name 函数名称
     */
    public Tool(String name) {
        this.name = name;
        this.parameters = new Parameters();
    }

    /**
     * 静态工厂方法创建Function实例
     *
     * @param name 函数名称
     * @return Function实例
     */
    public static Tool of(String name) {
        return new Tool(name);
    }

    /**
     * 获取函数名称
     *
     * @return 函数名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置函数名称
     *
     * @param name 函数名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取函数描述
     *
     * @return 函数描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置函数描述
     *
     * @param description 函数描述
     * @return 当前Function实例，用于链式调用
     */
    public Tool description(String description) {
        this.description = description;
        return this;
    }

    /**
     * 添加整数类型的参数（必填）
     *
     * @param name        参数名称
     * @param description 参数描述
     * @return 当前Function实例，用于链式调用
     */
    public Tool addIntParam(String name, String description) {
        return addIntParam(name, description, true);
    }

    /**
     * 添加整数类型的参数
     *
     * @param name        参数名称
     * @param description 参数描述
     * @param required    是否必填
     * @return 当前Function实例，用于链式调用
     */
    public Tool addIntParam(String name, String description, boolean required) {
        return addParam(name, description, ParameterProperty.TYPE_INTEGER, required);
    }

    /**
     * 添加双精度浮点数类型的参数
     *
     * @param name        参数名称
     * @param description 参数描述
     * @param required    是否必填
     * @return 当前Function实例，用于链式调用
     */
    public Tool addDoubleParam(String name, String description, boolean required) {
        return addParam(name, description, ParameterProperty.TYPE_DOUBLE, required);
    }

    /**
     * 添加双精度浮点数类型的参数（必填）
     *
     * @param name        参数名称
     * @param description 参数描述
     * @return 当前Function实例，用于链式调用
     */
    public Tool addDoubleParam(String name, String description) {
        return addDoubleParam(name, description, true);
    }

    /**
     * 添加字符串类型的参数
     *
     * @param name        参数名称
     * @param description 参数描述
     * @param required    是否必填
     * @return 当前Function实例，用于链式调用
     */
    public Tool addStringParam(String name, String description, boolean required) {
        return addParam(name, description, ParameterProperty.TYPE_STRING, required);
    }

    /**
     * 添加字符串类型的参数（必填）
     *
     * @param name        参数名称
     * @param description 参数描述
     * @return 当前Function实例，用于链式调用
     */
    public Tool addStringParam(String name, String description) {
        return addStringParam(name, description, true);
    }

    /**
     * 添加参数
     *
     * @param name        参数名称
     * @param description 参数描述
     * @param type        参数类型
     * @param required    是否必填
     * @return 当前Function实例，用于链式调用
     */
    public Tool addParam(String name, String description, String type, boolean required) {
        parameters.addParameter(name, description, type, required);
        return this;
    }

    /**
     * 获取函数参数定义
     *
     * @return 函数参数定义
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * 设置函数参数定义
     *
     * @param parameters 函数参数定义
     */
    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    /**
     * 参数属性类，定义函数参数的具体属性
     *
     * @author 三刀 zhengjunweimail@163.com
     * @version v1.0.0
     */
    public static class ParameterProperty {
        /**
         * 字符串类型常量
         */
        public static final String TYPE_STRING = "string";

        /**
         * 整数类型常量
         */
        public static final String TYPE_INTEGER = "integer";

        /**
         * 双精度浮点数类型常量
         */
        public static final String TYPE_DOUBLE = "float";

        /**
         * 参数类型
         */
        private String type;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 构造函数
         *
         * @param type        参数类型
         * @param description 参数描述
         */
        public ParameterProperty(String type, String description) {
            this.type = type;
            this.description = description;
        }

        /**
         * 获取参数类型
         *
         * @return 参数类型
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
         * 获取参数描述
         *
         * @return 参数描述
         */
        public String getDescription() {
            return description;
        }

        /**
         * 设置参数描述
         *
         * @param description 参数描述
         */
        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 函数参数类，定义函数的参数结构和属性
     *
     * @author 三刀 zhengjunweimail@163.com
     * @version v1.0.0
     */
    public static class Parameters {
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
}