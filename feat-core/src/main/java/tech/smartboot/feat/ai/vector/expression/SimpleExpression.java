package tech.smartboot.feat.ai.vector.expression;

public class SimpleExpression extends Expression {

    private String key;
    private Object value;

    SimpleExpression(String key) {
        this.key = key;
    }

    public Expression eq(int value) {
        return this;

    }

    public Expression eq(double value) {
        return this;
    }

    public Expression eq(String value) {
        return this;
    }
}