package tech.smartboot.feat.ai.vector.expression;

import com.alibaba.fastjson2.JSONObject;

public class SimpleExpression extends Expression {

    private final String key;
    private final Object value;

    SimpleExpression(ExpressionType type, String key, Object value) {
        super(type);
        this.key = key;
        this.value = value;
    }

    public Expression eq(int value) {
        return new SimpleExpression(ExpressionType.EQ, key, value);
    }

    public Expression eq(double value) {
        return new SimpleExpression(ExpressionType.EQ, key, value);
    }

    public Expression eq(String value) {
        return new SimpleExpression(ExpressionType.EQ, key, value);
    }


    @Override
    public void build(JSONObject object, Convert convert) {
        convert.build(object, this);
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}