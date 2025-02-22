package tech.smartboot.feat.ai.vector.expression;

public class ExpressionParser {

    private final String expression;
    private int position = 0;

    public ExpressionParser(String expression) {
        this.expression = expression.trim();
    }

    public Expression parse() {
        throw new UnsupportedOperationException("Not supported yet.");
//        return parseExpression();
    }


}