package tech.smartboot.feat.ai.vector;

import tech.smartboot.feat.ai.vector.expression.Expression;

public class SearchRequest {
    /**
     * 匹配document中的内容
     */
    private String query;
    /**
     * 匹配metadata
     */
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

    public void setExpression(String expression) {

    }
}
