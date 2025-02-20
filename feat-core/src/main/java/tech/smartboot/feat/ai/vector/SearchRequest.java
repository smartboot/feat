package tech.smartboot.feat.ai.vector;

import tech.smartboot.feat.ai.vector.expression.Expression;

public class SearchRequest {
    private String query;
    private Expression expression;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}
