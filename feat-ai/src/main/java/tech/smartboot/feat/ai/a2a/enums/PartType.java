/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.enums;

/**
 * A2A 消息部分内容类型枚举
 *
 * <p>定义了A2A协议中消息部分可以包含的内容类型。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public enum PartType {
    /**
     * 文本内容
     */
    TEXT("text"),

    /**
     * 文件内容
     */
    FILE("file"),

    /**
     * 结构化数据
     */
    DATA("data"),

    /**
     * 函数调用
     */
    FUNCTION_CALL("function_call"),

    /**
     * 函数调用结果
     */
    FUNCTION_RESPONSE("function_response");

    private final String value;

    PartType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 根据值查找内容类型
     *
     * @param value 类型值
     * @return 对应的PartType枚举，如果未找到则返回null
     */
    public static PartType fromValue(String value) {
        for (PartType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
