package tech.smartboot.feat.ai.vector.expression;

import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
    public void build(JSONObject object, Convert convert) {
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