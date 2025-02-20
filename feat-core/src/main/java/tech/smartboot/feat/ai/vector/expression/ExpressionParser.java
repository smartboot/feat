package tech.smartboot.feat.ai.vector.expression;

import java.util.ArrayList;
import java.util.List;

public class ExpressionParser {
    private final String expression;
    private int pos;

    public ExpressionParser(String expression) {
        this.expression = expression;
    }

    public Expression parse() {
        pos = 0;
        skipWhitespace();
        Expression result = parseExpression();
        skipWhitespace();
        if (pos < expression.length()) {
            throw new RuntimeException("Unexpected character at position " + pos);
        }
        return result;
    }

    private Expression parseExpression() {
        Expression left = parseSimpleExpression();
        while (pos < expression.length()) {
            skipWhitespace();
            if (pos + 2 < expression.length() && expression.substring(pos, pos + 3).equals("&& ")) {
                pos += 3;
                left = left.and(parseSimpleExpression());
            } else if (pos + 2 < expression.length() && expression.substring(pos, pos + 3).equals("|| ")) {
                pos += 3;
                left = left.or(parseSimpleExpression());
            } else {
                break;
            }
        }
        return left;
    }

    private Expression parseSimpleExpression() {
        String key = parseIdentifier();
        skipWhitespace();
        String op = parseOperator();
        skipWhitespace();
        Object value = parseValue();
        return new SimpleExpression(ExpressionType.valueOf(op.toUpperCase()), key, value);
    }

    private String parseIdentifier() {
        int start = pos;
        while (pos < expression.length() && Character.isLetterOrDigit(expression.charAt(pos))) {
            pos++;
        }
        if (pos == start) {
            throw new RuntimeException("Expected identifier at position " + pos);
        }
        return expression.substring(start, pos);
    }

    private String parseOperator() {
        if (pos + 2 < expression.length() && expression.startsWith("== ", pos)) {
            pos += 3;
            return "EQ";
        } else if (pos + 2 < expression.length() && expression.startsWith(">= ", pos)) {
            pos += 3;
            return "GTE";
        } else if (pos + 2 < expression.length() && expression.startsWith("<= ", pos)) {
            pos += 3;
            return "LTE";
        } else if (pos + 4 < expression.length() && expression.substring(pos, pos + 5).equals("in [")) {
            pos += 5;
            return "IN";
        } else {
            throw new RuntimeException("Expected operator at position " + pos);
        }
    }

    private Object parseValue() {
        if (expression.charAt(pos) == '\'') {
            return parseString();
        } else if (expression.charAt(pos) == '[') {
            return parseList();
        } else {
            throw new RuntimeException("Expected value at position " + pos);
        }
    }

    private String parseString() {
        pos++;
        int start = pos;
        while (pos < expression.length() && expression.charAt(pos) != '\'') {
            pos++;
        }
        if (pos == start || pos >= expression.length()) {
            throw new RuntimeException("Expected string at position " + pos);
        }
        pos++;
        return expression.substring(start, pos - 1);
    }

    private List<Object> parseList() {
        pos++;
        List<Object> list = new ArrayList<>();
        while (pos < expression.length() && expression.charAt(pos) != ']') {
            list.add(parseString());
            skipWhitespace();
            if (pos < expression.length() && expression.charAt(pos) == ',') {
                pos++;
                skipWhitespace();
            }
        }
        if (pos >= expression.length() || expression.charAt(pos) != ']') {
            throw new RuntimeException("Expected ']' at position " + pos);
        }
        pos++;
        return list;
    }

    private void skipWhitespace() {
        while (pos < expression.length() && Character.isWhitespace(expression.charAt(pos))) {
            pos++;
        }
    }
}
