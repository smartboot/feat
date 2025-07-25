/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.vector.expression;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public enum ExpressionType {
    AND, OR, EQ, NE, GT, GTE, LT, LTE, IN, NIN, NOT;

    public static ExpressionType from(String operator) {
        for (ExpressionType type : values()) {
            if (type.name().equalsIgnoreCase(operator)) {
                return type;
            }
        }
        return null;
    }
}