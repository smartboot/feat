package tech.smartboot.feat.ai.vector.expression;

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