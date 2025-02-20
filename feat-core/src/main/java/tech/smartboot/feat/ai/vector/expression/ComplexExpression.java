package tech.smartboot.feat.ai.vector.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ComplexExpression extends Expression {
    private final List<Expression> expressions = new ArrayList<>();

    public ComplexExpression(boolean and, Expression left, Expression right) {
        super(and ? ExpressionType.AND : ExpressionType.OR);
        expressions.add(left);
        expressions.add(right);
    }


    public List<Expression> getExpressions() {
        return expressions;
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
    public void build(Consumer<Filter> consumer) {
        super.build(consumer);
    }
}