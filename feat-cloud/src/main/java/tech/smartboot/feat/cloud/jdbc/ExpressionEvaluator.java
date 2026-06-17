/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *  and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.jdbc;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 极简 OGNL 风格表达式求值器，供动态 SQL 的 test 属性使用。
 *
 * <p>支持：and/or、==/!=、>/< />=/<=、!、括号、属性访问、字符串/数字/null/true/false。</p>
 */
public class ExpressionEvaluator {

    public static boolean evalBoolean(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        Parser parser = new Parser(new Tokenizer(expression), context);
        Object value = parser.parse();
        return isTrue(value);
    }

   public static Object evalObject(String expression, Map<String, Object> context) {
       if (expression == null || expression.trim().isEmpty()) {
           return null;
       }
       Parser parser = new Parser(new Tokenizer(expression), context);
       return parser.parse();
   }

    private static boolean isTrue0(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        return true;
    }

    /**
     * 预编译动态 SQL 后，在生成的 if-else 代码中直接调用的比较/逻辑辅助方法。
     * 这些方法避免了每次运行时重新解析表达式。
     */
    public static boolean isTrue(Object value) {
        return isTrue0(value);
    }

    public static boolean eq(Object a, Object b) {
        return eq0(a, b);
    }

    public static boolean ne(Object a, Object b) {
        return !eq0(a, b);
    }

    public static boolean gt(Object a, Object b) {
        return compare0(a, b) > 0;
    }

    public static boolean gte(Object a, Object b) {
        return compare0(a, b) >= 0;
    }

    public static boolean lt(Object a, Object b) {
        return compare0(a, b) < 0;
    }

    public static boolean lte(Object a, Object b) {
        return compare0(a, b) <= 0;
    }

    private static boolean eq0(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    private static int compare0(Object a, Object b) {
        if (a == null || b == null) {
            throw new RuntimeException("Cannot compare null values");
        }
        if (a instanceof Number && b instanceof Number) {
            double d1 = ((Number) a).doubleValue();
            double d2 = ((Number) b).doubleValue();
            return Double.compare(d1, d2);
        }
        if (a instanceof Comparable && b instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> ca = (Comparable<Object>) a;
            return ca.compareTo(b);
        }
        throw new RuntimeException("Cannot compare " + a.getClass() + " and " + b.getClass());
    }

   private enum TokenType {
        EOF, IDENTIFIER, STRING, NUMBER, TRUE, FALSE, NULL, COMMA,
        AND, OR, NOT,
        EQ, NEQ, GT, LT, GTE, LTE,
        LPAREN, RPAREN
    }

    private static class Token {
        final TokenType type;
        final String text;
        final Object value;

        Token(TokenType type, String text, Object value) {
            this.type = type;
            this.text = text;
            this.value = value;
        }
    }

    private static class Tokenizer {
        private final String input;
        private int pos;

        Tokenizer(String input) {
            this.input = input;
        }

        Token next() {
            skipWhitespace();
            if (pos >= input.length()) {
                return new Token(TokenType.EOF, "", null);
            }
            char c = input.charAt(pos);
            if (c == '\'' || c == '\"') {
                return readString(c);
            }
            if (Character.isDigit(c) || (c == '-' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1)))) {
                return readNumber();
            }
            if (Character.isLetter(c) || c == '_') {
                return readIdentifier();
            }
            return readOperator(c);
        }

        private Token readString(char quote) {
            StringBuilder sb = new StringBuilder();
            pos++; // skip opening quote
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '\\' && pos + 1 < input.length()) {
                    sb.append(input.charAt(pos + 1));
                    pos += 2;
                    continue;
                }
                if (c == quote) {
                    pos++;
                    break;
                }
                sb.append(c);
                pos++;
            }
            return new Token(TokenType.STRING, sb.toString(), sb.toString());
        }

        private Token readNumber() {
            int start = pos;
            boolean dotSeen = false;
            if (input.charAt(pos) == '-') {
                pos++;
            }
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '.') {
                    if (dotSeen) {
                        break;
                    }
                    dotSeen = true;
                    pos++;
                } else if (Character.isDigit(c)) {
                    pos++;
                } else {
                    break;
                }
            }
            String text = input.substring(start, pos);
            Object value = dotSeen ? Double.parseDouble(text) : Long.parseLong(text);
            return new Token(TokenType.NUMBER, text, value);
        }

        private Token readIdentifier() {
            int start = pos;
            while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
                pos++;
            }
            String text = input.substring(start, pos);
            String lower = text.toLowerCase();
            switch (lower) {
                case "and":
                    return new Token(TokenType.AND, text, null);
                case "or":
                    return new Token(TokenType.OR, text, null);
                case "not":
                    return new Token(TokenType.NOT, text, null);
                case "true":
                    return new Token(TokenType.TRUE, text, Boolean.TRUE);
                case "false":
                    return new Token(TokenType.FALSE, text, Boolean.FALSE);
                case "null":
                    return new Token(TokenType.NULL, text, null);
                default:
                    return new Token(TokenType.IDENTIFIER, text, text);
            }
        }

        private Token readOperator(char c) {
            pos++;
            switch (c) {
                case '(':
                    return new Token(TokenType.LPAREN, "(", null);
                case ')':
                    return new Token(TokenType.RPAREN, ")", null);
                case '!':
                    if (peek() == '=') {
                        pos++;
                        return new Token(TokenType.NEQ, "!=", null);
                    }
                    return new Token(TokenType.NOT, "!", null);
                case '=':
                    if (peek() == '=') {
                        pos++;
                    }
                    return new Token(TokenType.EQ, "==", null);
                case '>':
                    if (peek() == '=') {
                        pos++;
                        return new Token(TokenType.GTE, ">=", null);
                    }
                    return new Token(TokenType.GT, ">", null);
                case '<':
                    if (peek() == '=') {
                        pos++;
                        return new Token(TokenType.LTE, "<=", null);
                    }
                    return new Token(TokenType.LT, "<", null);
                case ',':
                    return new Token(TokenType.COMMA, ",", null);
                case '.':
                    return new Token(TokenType.IDENTIFIER, ".", null);
                case '+':
                    return new Token(TokenType.IDENTIFIER, "+", null);
                case '-':
                    return new Token(TokenType.IDENTIFIER, "-", null);
                default:
                    throw new RuntimeException("Unsupported character in expression: '" + c + "'");
            }
        }

        private char peek() {
            return pos < input.length() ? input.charAt(pos) : '\0';
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }

    private static class Parser {
        private final Tokenizer tokenizer;
        private final Map<String, Object> context;
        private Token token;

        Parser(Tokenizer tokenizer, Map<String, Object> context) {
            this.tokenizer = tokenizer;
            this.context = context;
            this.token = tokenizer.next();
        }

        private void next() {
            token = tokenizer.next();
        }

        private void expect(TokenType type) {
            if (token.type != type) {
                throw new RuntimeException("Expected " + type + " but got " + token.type);
            }
        }

        Object parse() {
            Object result = orExpr();
            expect(TokenType.EOF);
            return result;
        }

       private Object orExpr() {
           Object left = andExpr();
           while (token.type == TokenType.OR) {
               next();
               Object right = andExpr();
                left = isTrue0(left) || isTrue0(right);
           }
           return left;
       }

       private Object andExpr() {
           Object left = equality();
           while (token.type == TokenType.AND) {
               next();
               Object right = equality();
                left = isTrue0(left) && isTrue0(right);
           }
           return left;
       }

       private Object equality() {
           Object left = relational();
           while (token.type == TokenType.EQ || token.type == TokenType.NEQ) {
               TokenType op = token.type;
               next();
               Object right = relational();
                left = op == TokenType.EQ ? eq0(left, right) : !eq0(left, right);
           }
           return left;
       }

       private Object relational() {
           Object left = additive();
           while (token.type == TokenType.GT || token.type == TokenType.LT
                   || token.type == TokenType.GTE || token.type == TokenType.LTE) {
               TokenType op = token.type;
               next();
               Object right = additive();
                int cmp = compare0(left, right);
               switch (op) {
                   case GT:
                       left = cmp > 0;
                       break;
                   case LT:
                       left = cmp < 0;
                       break;
                   case GTE:
                       left = cmp >= 0;
                       break;
                   case LTE:
                       left = cmp <= 0;
                       break;
                   default:
                       break;
               }
           }
           return left;
       }

        private Object additive() {
            Object left = unary();
            while (token.type == TokenType.IDENTIFIER && ("+".equals(token.text) || "-".equals(token.text))) {
                String op = token.text;
                next();
                Object right = unary();
                if ("+".equals(op)) {
                    if (left instanceof String || right instanceof String) {
                        left = String.valueOf(left) + String.valueOf(right);
                    } else if (left instanceof Number && right instanceof Number) {
                        left = ((Number) left).doubleValue() + ((Number) right).doubleValue();
                    } else {
                        throw new RuntimeException("Unsupported operands for +");
                    }
                } else {
                    if (left instanceof Number && right instanceof Number) {
                        left = ((Number) left).doubleValue() - ((Number) right).doubleValue();
                    } else {
                        throw new RuntimeException("Unsupported operands for -");
                    }
                }
            }
            return left;
        }

       private Object unary() {
           if (token.type == TokenType.NOT) {
               next();
                return !isTrue0(unary());
           }
           return primary();
       }

        private Object primary() {
            switch (token.type) {
                case TRUE:
                case FALSE:
                case NULL:
                case STRING:
                case NUMBER: {
                    Object value = token.value;
                    next();
                    return value;
                }
                case IDENTIFIER: {
                    String rootName = token.text;
                    next();
                    return resolveChain(rootName);
                }
                case LPAREN: {
                    next();
                    Object value = orExpr();
                    expect(TokenType.RPAREN);
                    next();
                    return value;
                }
                default:
                    throw new RuntimeException("Unexpected token: " + token.type);
            }
        }

       private boolean eq(Object a, Object b) {
           if (a == null || b == null) {
               return a == b;
           }
           if (a instanceof Number && b instanceof Number) {
               return ((Number) a).doubleValue() == ((Number) b).doubleValue();
           }
           return a.equals(b);
       }

       private int compare(Object a, Object b) {
           if (a == null || b == null) {
               throw new RuntimeException("Cannot compare null values");
           }
           if (a instanceof Number && b instanceof Number) {
               double d1 = ((Number) a).doubleValue();
               double d2 = ((Number) b).doubleValue();
               return Double.compare(d1, d2);
           }
           if (a instanceof Comparable && b instanceof Comparable) {
               @SuppressWarnings("unchecked")
               Comparable<Object> ca = (Comparable<Object>) a;
               return ca.compareTo(b);
           }
           throw new RuntimeException("Cannot compare " + a.getClass() + " and " + b.getClass());
       }

       private Object resolveChain(String rootName) {
            Object value = context.get(rootName);
            while (token.type == TokenType.IDENTIFIER && ".".equals(token.text)) {
                next();
                expect(TokenType.IDENTIFIER);
                String segment = token.text;
                next();
                if (token.type == TokenType.LPAREN) {
                    next();
                    List<Object> args = new ArrayList<>();
                    if (token.type != TokenType.RPAREN) {
                        args.add(orExpr());
                        while (token.type == TokenType.COMMA) {
                            next();
                            args.add(orExpr());
                        }
                    }
                    expect(TokenType.RPAREN);
                    next();
                    value = invokeMethod(value, segment, args);
                } else {
                    value = getProperty(value, segment);
                }
            }
            return value;
        }

        private Object getProperty(Object obj, String property) {
            if (obj == null || property == null || property.isEmpty()) {
                return null;
            }
            if (obj instanceof Map) {
                return ((Map<?, ?>) obj).get(property);
            }
            if (obj.getClass().isArray() && "length".equals(property)) {
                return Array.getLength(obj);
            }
            String getter = "get" + capitalize(property);
            String isGetter = "is" + capitalize(property);
            Method exactMethod = null;
            try {
                for (Method method : obj.getClass().getMethods()) {
                    if (method.getParameterCount() != 0) {
                        continue;
                    }
                    String name = method.getName();
                    if (name.equals(getter) || name.equals(isGetter)) {
                        return method.invoke(obj);
                    }
                    if (name.equals(property)) {
                        exactMethod = method;
                    }
                }
                if (exactMethod != null) {
                    return exactMethod.invoke(obj);
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        private Object invokeMethod(Object obj, String name, List<Object> args) {
            if (obj == null) {
                return null;
            }
            int argc = args.size();
            Method match = null;
            Object[] converted = new Object[argc];
            for (Method method : obj.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != argc) {
                    continue;
                }
                Class<?>[] paramTypes = method.getParameterTypes();
                boolean ok = true;
                for (int i = 0; i < argc; i++) {
                    Object arg = args.get(i);
                    if (arg == null) {
                        if (paramTypes[i].isPrimitive()) {
                            ok = false;
                            break;
                        }
                        converted[i] = null;
                    } else {
                        Object cvt = convert(arg, paramTypes[i]);
                        if (cvt == null && arg != null && !isAssignable(paramTypes[i], arg.getClass())) {
                            ok = false;
                            break;
                        }
                        converted[i] = cvt == null ? arg : cvt;
                    }
                }
                if (ok) {
                    match = method;
                    break;
                }
            }
            if (match == null) {
                return null;
            }
            try {
                return match.invoke(obj, converted);
            } catch (Exception ignored) {
                return null;
            }
        }

        private static Object convert(Object value, Class<?> target) {
            if (value == null) {
                return null;
            }
            if (target.isInstance(value)) {
                return value;
            }
            if (value instanceof Number) {
                Number number = (Number) value;
                if (target == int.class || target == Integer.class) {
                    return number.intValue();
                }
                if (target == long.class || target == Long.class) {
                    return number.longValue();
                }
                if (target == double.class || target == Double.class) {
                    return number.doubleValue();
                }
                if (target == float.class || target == Float.class) {
                    return number.floatValue();
                }
                if (target == short.class || target == Short.class) {
                    return number.shortValue();
                }
                if (target == byte.class || target == Byte.class) {
                    return number.byteValue();
                }
            }
            if (target == String.class) {
                return String.valueOf(value);
            }
            return null;
        }

        private static boolean isAssignable(Class<?> target, Class<?> source) {
            if (target.isAssignableFrom(source)) {
                return true;
            }
            if (!target.isPrimitive()) {
                return false;
            }
            if (target == int.class) return source == Integer.class;
            if (target == long.class) return source == Long.class;
            if (target == double.class) return source == Double.class;
            if (target == float.class) return source == Float.class;
            if (target == boolean.class) return source == Boolean.class;
            if (target == byte.class) return source == Byte.class;
            if (target == short.class) return source == Short.class;
            if (target == char.class) return source == Character.class;
            return false;
        }

        private static String capitalize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            return Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }
    }
}
