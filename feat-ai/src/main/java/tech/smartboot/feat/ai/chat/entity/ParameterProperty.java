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
 * 参数属性类，定义函数参数的具体属性
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ParameterProperty {
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