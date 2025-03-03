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

import com.alibaba.fastjson2.JSONObject;

public abstract class Expression {
    private final ExpressionType type;

    Expression(ExpressionType type) {
        this.type = type;
    }

    public static Expression parse(String expression) {
        return new ExpressionParser(expression).parse();
    }

    public static SimpleExpression of(String key) {
        return new SimpleExpression(key);
    }

    public Expression and(Expression right) {
        return new ComplexExpression(true, this, right);
    }

    public Expression or(Expression right) {
        return new ComplexExpression(false, this, right);
    }

    public ExpressionType getType() {
        return type;
    }

    public abstract<T> void build(T object, Convert<T> convert);
}