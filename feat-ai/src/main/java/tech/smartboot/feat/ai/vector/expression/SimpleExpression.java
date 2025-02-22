package tech.smartboot.feat.ai.vector.expression;

public class SimpleExpression extends Expression {

    private final String key;
    private final Object value;
    private final Class valueType;

    SimpleExpression(String key) {
        this(null, key, null, null);
    }

    SimpleExpression(ExpressionType type, String key, Object value, Class valueType) {
        super(type);
        this.key = key;
        this.value = value;
        this.valueType = valueType;
    }

    public Expression eq(int value) {
        return new SimpleExpression(ExpressionType.EQ, key, value, int.class);
    }

    public Expression eq(long value) {
        return new SimpleExpression(ExpressionType.EQ, key, value, long.class);
    }

    public Expression eq(double value) {
        return new SimpleExpression(ExpressionType.EQ, key, value, double.class);
    }

    public Expression eq(String value) {
        return new SimpleExpression(ExpressionType.EQ, key, value, String.class);
    }

    public Expression in(String[] values) {
        return new SimpleExpression(ExpressionType.IN, key, value, String.class);
    }

    public Expression in(long[] values) {
        return new SimpleExpression(ExpressionType.IN, key, value, long.class);
    }


    @Override
    public <T> void build(T object, Convert<T> convert) {
        convert.build(object, this);
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public Class getValueType() {
        return valueType;
    }
}