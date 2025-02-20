package tech.smartboot.feat.ai.vector.expression;

import com.alibaba.fastjson2.JSONObject;

public abstract class Expression {
    private final ExpressionType type;

    Expression(ExpressionType type) {
        this.type = type;
    }

    public static SimpleExpression of(String key) {
        return new SimpleExpression(null, key, null);
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

    public abstract void build(JSONObject object, Convert convert);
}