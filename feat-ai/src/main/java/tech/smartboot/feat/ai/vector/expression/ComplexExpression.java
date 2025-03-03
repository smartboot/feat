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

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
class ComplexExpression extends Expression {
    private final List<Expression> expressions = new ArrayList<>();

    public ComplexExpression(boolean and, Expression left, Expression right) {
        super(and ? ExpressionType.AND : ExpressionType.OR);
        expressions.add(left);
        expressions.add(right);
    }

    @Override
    public Expression and(Expression right) {
        if (getType() == ExpressionType.OR) {
            return new ComplexExpression(true, this, right);
        }
        if (right instanceof SimpleExpression) {
            expressions.add(right);
            return this;
        }
        ComplexExpression complexRight = (ComplexExpression) right;
        if (getType() == ExpressionType.AND) {
            expressions.addAll(complexRight.expressions);
            return this;
        }
        return new ComplexExpression(true, this, right);
    }

    @Override
    public Expression or(Expression right) {
        if (getType() == ExpressionType.AND) {
            return new ComplexExpression(false, this, right);
        }
        if (right instanceof SimpleExpression) {
            expressions.add(right);
            return this;
        }
        ComplexExpression complexRight = (ComplexExpression) right;
        if (getType() == ExpressionType.OR) {
            expressions.addAll(complexRight.expressions);
            return this;
        }
        return new ComplexExpression(false, this, right);
    }

    @Override
    public <T> void build(T object, Convert<T> convert) {
        switch (getType()) {
            case AND:
                convert.and(object, expressions);
                break;
            case OR:
                convert.or(object, expressions);
                break;
            default:
                throw new RuntimeException("不支持的表达式类型");
        }
    }
}