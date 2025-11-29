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

/**
 * 函数类，表示AI模型可以调用的函数定义
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Function {
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
    private FunctionParameters parameters;

    /**
     * 构造函数
     *
     * @param name 函数名称
     */
    public Function(String name) {
        this.name = name;
        this.parameters = new FunctionParameters();
    }

    /**
     * 静态工厂方法创建Function实例
     *
     * @param name 函数名称
     * @return Function实例
     */
    public static Function of(String name) {
        return new Function(name);
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
    public Function description(String description) {
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
    public Function addIntParam(String name, String description) {
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
    public Function addIntParam(String name, String description, boolean required) {
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
    public Function addDoubleParam(String name, String description, boolean required) {
        return addParam(name, description, ParameterProperty.TYPE_DOUBLE, required);
    }

    /**
     * 添加双精度浮点数类型的参数（必填）
     *
     * @param name        参数名称
     * @param description 参数描述
     * @return 当前Function实例，用于链式调用
     */
    public Function addDoubleParam(String name, String description) {
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
    public Function addStringParam(String name, String description, boolean required) {
        return addParam(name, description, ParameterProperty.TYPE_STRING, required);
    }

    /**
     * 添加字符串类型的参数（必填）
     *
     * @param name        参数名称
     * @param description 参数描述
     * @return 当前Function实例，用于链式调用
     */
    public Function addStringParam(String name, String description) {
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
    public Function addParam(String name, String description, String type, boolean required) {
        parameters.addParameter(name, description, type, required);
        return this;
    }

    /**
     * 获取函数参数定义
     *
     * @return 函数参数定义
     */
    public FunctionParameters getParameters() {
        return parameters;
    }

    /**
     * 设置函数参数定义
     *
     * @param parameters 函数参数定义
     */
    public void setParameters(FunctionParameters parameters) {
        this.parameters = parameters;
    }
}