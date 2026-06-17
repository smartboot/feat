package tech.smartboot.feat.cloud.aot.serializer.orm.dynamic;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 MyBatis 风格表达式（test/value）在 APT 阶段翻译为 Java 表达式。
 *
 * <p>避免运行时调用 {@code ExpressionEvaluator.eval} 反复解析表达式，
 * 只在生成代码时使用一次解析器。</p>
 */
public final class ExpressionTranslator {

    private static final String EVAL = "tech.smartboot.feat.cloud.jdbc.ExpressionEvaluator";

    private ExpressionTranslator() {
    }

    public static String toBoolean(String expression, DynamicScope scope) {
        if (expression == null || expression.trim().isEmpty()) {
            return "false";
        }
        Parser parser = new Parser(expression, scope);
        Expr expr = parser.parseOr();
        if (!expr.booleanType) {
            expr.text = EVAL + ".isTrue(" + expr.text + ")";
        }
        return expr.text;
    }

    public static String toObject(String expression, DynamicScope scope) {
        if (expression == null || expression.trim().isEmpty()) {
            return "null";
        }
        Parser parser = new Parser(expression, scope);
        return parser.parseOr().text;
    }

    private static final class Expr {
        String text;
        boolean booleanType;

        Expr(String text, boolean booleanType) {
            this.text = text;
            this.booleanType = booleanType;
        }
    }

    private enum TokenType {
        EOF, IDENTIFIER, STRING, NUMBER, TRUE, FALSE, NULL,
        AND, OR, NOT,
        EQ, NEQ, GT, LT, GTE, LTE,
        LPAREN, RPAREN, COMMA, DOT, PLUS, MINUS
    }

    private static final class Token {
        final TokenType type;
        final String text;
        final Object value;

        Token(TokenType type, String text, Object value) {
            this.type = type;
            this.text = text;
            this.value = value;
        }
    }

    private static final class Tokenizer {
        private final String input;
        private int pos;

        Tokenizer(String input) {
            this.input = input;
            this.pos = 0;
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
            pos++;
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
                    if (peek() == '>') {
                        pos++;
                        return new Token(TokenType.NEQ, "<>", null);
                    }
                    return new Token(TokenType.LT, "<", null);
                case ',':
                    return new Token(TokenType.COMMA, ",", null);
                case '.':
                    return new Token(TokenType.DOT, ".", null);
                case '+':
                    return new Token(TokenType.PLUS, "+", null);
                case '-':
                    return new Token(TokenType.MINUS, "-", null);
                case '&':
                    if (peek() == '&') {
                        pos++;
                        return new Token(TokenType.AND, "&&", null);
                    }
                    break;
                case '|':
                    if (peek() == '|') {
                        pos++;
                        return new Token(TokenType.OR, "||", null);
                    }
                    break;
                default:
                    break;
            }
            throw new RuntimeException("Unsupported character in expression: '" + c + "'");
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

    private static final class Parser {
        private final Tokenizer tokenizer;
        private final DynamicScope scope;
        private Token token;

        Parser(String expression, DynamicScope scope) {
            this.tokenizer = new Tokenizer(expression);
            this.scope = scope;
            this.token = tokenizer.next();
        }

        private void next() {
            token = tokenizer.next();
        }

        private boolean check(TokenType type) {
            return token.type == type;
        }

        private boolean match(TokenType... types) {
            for (TokenType type : types) {
                if (token.type == type) {
                    next();
                    return true;
                }
            }
            return false;
        }

        private void expect(TokenType type) {
            if (token.type != type) {
                throw new RuntimeException("Expected " + type + " but got " + token.type);
            }
        }

        Expr parseOr() {
            Expr left = parseAnd();
            while (match(TokenType.OR)) {
                Expr right = parseAnd();
                left = new Expr("(" + left.text + " || " + right.text + ")", true);
            }
            return left;
        }

        Expr parseAnd() {
            Expr left = parseEquality();
            while (match(TokenType.AND)) {
                Expr right = parseEquality();
                left = new Expr("(" + left.text + " && " + right.text + ")", true);
            }
            return left;
        }

        Expr parseEquality() {
            Expr left = parseRelational();
            while (true) {
                if (match(TokenType.EQ)) {
                    Expr right = parseRelational();
                    left = new Expr(EVAL + ".eq(" + left.text + ", " + right.text + ")", true);
                } else if (match(TokenType.NEQ)) {
                    Expr right = parseRelational();
                    left = new Expr("!" + EVAL + ".eq(" + left.text + ", " + right.text + ")", true);
                } else {
                    break;
                }
            }
            return left;
        }

        Expr parseRelational() {
            Expr left = parseAdditive();
            while (true) {
                String helper = null;
                if (match(TokenType.GT)) {
                    helper = "gt";
                } else if (match(TokenType.LT)) {
                    helper = "lt";
                } else if (match(TokenType.GTE)) {
                    helper = "gte";
                } else if (match(TokenType.LTE)) {
                    helper = "lte";
                }
                if (helper == null) {
                    break;
                }
                Expr right = parseAdditive();
                left = new Expr(EVAL + "." + helper + "(" + left.text + ", " + right.text + ")", true);
            }
           return left;
       }

       Expr parseAdditive() {
           Expr left = parseUnary();
            while (token.type == TokenType.PLUS || token.type == TokenType.MINUS) {
                String op = token.type == TokenType.PLUS ? "+" : "-";
                next();
               Expr right = parseUnary();
               left = new Expr("(" + left.text + " " + op + " " + right.text + ")", false);
           }
           return left;
       }

       Expr parseUnary() {
            if (match(TokenType.NOT)) {
                Expr operand = parseUnary();
                return new Expr("!" + operand.text, true);
            }
            if (match(TokenType.MINUS)) {
                Expr operand = parseUnary();
                return new Expr("(-" + operand.text + ")", false);
            }
            return parsePrimary();
        }

       Expr parsePrimary() {
            if (token.type == TokenType.LPAREN) {
                next();
                Expr expr = parseOr();
               expect(TokenType.RPAREN);
               next();
               return new Expr("(" + expr.text + ")", expr.booleanType);
           }
            if (token.type == TokenType.TRUE) {
                next();
                return new Expr("true", true);
           }
            if (token.type == TokenType.FALSE) {
                next();
                return new Expr("false", true);
           }
            if (token.type == TokenType.NULL) {
                next();
                return new Expr("null", false);
           }
            if (token.type == TokenType.NUMBER) {
                String text = token.text;
                next();
                return new Expr(text, false);
           }
            if (token.type == TokenType.STRING) {
                String value = (String) token.value;
                next();
                return new Expr("\"" + escape(value) + "\"", false);
           }
            if (token.type == TokenType.IDENTIFIER) {
                String name = token.text;
                next();
               return parseChain(new Expr(scope.resolveRoot(name), false));
           }
           throw new RuntimeException("Unexpected token: " + token.type);
       }

        Expr parseChain(Expr base) {
            while (match(TokenType.DOT)) {
                expect(TokenType.IDENTIFIER);
                String name = token.text;
                next();
                if (match(TokenType.LPAREN)) {
                    List<String> args = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        args.add(parseOr().text);
                        while (match(TokenType.COMMA)) {
                            args.add(parseOr().text);
                        }
                    }
                    expect(TokenType.RPAREN);
                    next();
                    base = new Expr(base.text + "." + name + "(" + join(args) + ")", false);
                } else {
                    base = new Expr(base.text + ".get" + capitalize(name) + "()", false);
                }
            }
            return base;
        }

        private static String join(List<String> args) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args.get(i));
            }
            return sb.toString();
        }

        private static String capitalize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            return Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }

        private static String escape(String value) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '\"':
                        sb.append("\\\"");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
