package tech.smartboot.feat.cloud.aot.orm.script;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * AST Builder
 *
 * Token -> SqlScript
 *
 * @author 三刀
 */
public final class SqlScriptParser {

    private SqlScriptParser() {
    }

    public static void main(String[] args) {
        String sql =
                "<script>\n" +
                        "select * from user\n" +
                        "<where>\n" +
                        "   <if test=\"id != null\">\n" +
                        "       and id = #{id}\n" +
                        "   </if>\n" +
                        "   <if test=\"name != null\">\n" +
                        "       and name = #{name}\n" +
                        "   </if>\n" +
                        "</where>\n" +
                        "</script>";

        SqlScript script = SqlScriptParser.parse(sql);
        System.out.println(script);
    }
    public static SqlScript parse(String script) {

        List<Token> tokens =
                SqlTokenizer.tokenize(script);

        SqlScript sqlScript =
                new SqlScript();

        Deque<ContainerSegment> stack =
                new ArrayDeque<ContainerSegment>();

        for (Token token : tokens) {

            switch (token.getType()) {

                case TEXT:

                    addSegment(
                            stack,
                            sqlScript,
                            SqlParameterParser.parse(
                                    token.getContent()
                            )
                    );
                    break;

                case IF_START:

                    IfSegment ifSegment =
                            new IfSegment(
                                    token.getAttributes()
                                            .get("test")
                            );

                    addSegment(
                            stack,
                            sqlScript,
                            ifSegment
                    );

                    stack.push(ifSegment);

                    break;

                case IF_END:

                    pop(
                            stack,
                            IfSegment.class
                    );

                    break;

                case WHERE_START:

                    WhereSegment where =
                            new WhereSegment();

                    addSegment(
                            stack,
                            sqlScript,
                            where
                    );

                    stack.push(where);

                    break;

                case WHERE_END:

                    pop(
                            stack,
                            WhereSegment.class
                    );

                    break;

                case TRIM_START:

                    TrimSegment trim =
                            new TrimSegment(
                                    token.getAttributes()
                                            .get("prefix"),
                                    token.getAttributes()
                                            .get("suffix"),
                                    token.getAttributes()
                                            .get("prefixOverrides"),
                                    token.getAttributes()
                                            .get("suffixOverrides")
                            );

                    addSegment(
                            stack,
                            sqlScript,
                            trim
                    );

                    stack.push(trim);

                    break;

                case TRIM_END:

                    pop(
                            stack,
                            TrimSegment.class
                    );

                    break;

                case FOREACH_START:

                    ForeachSegment foreach =
                            new ForeachSegment(
                                    token.getAttributes()
                                            .get("collection"),
                                    token.getAttributes()
                                            .get("item"),
                                    token.getAttributes()
                                            .get("open"),
                                    token.getAttributes()
                                            .get("separator"),
                                    token.getAttributes()
                                            .get("close")
                            );

                    addSegment(
                            stack,
                            sqlScript,
                            foreach
                    );

                    stack.push(foreach);

                    break;

                case FOREACH_END:

                    pop(
                            stack,
                            ForeachSegment.class
                    );

                    break;

                default:

                    throw new IllegalStateException(
                            "unsupported token: "
                                    + token.getType()
                    );
            }
        }

        if (!stack.isEmpty()) {

            throw new IllegalStateException(
                    "tag not closed: "
                            + stack.peek()
                                    .getClass()
                                    .getSimpleName()
            );
        }

        return sqlScript;
    }

    private static void addSegment(
            Deque<ContainerSegment> stack,
            SqlScript script,
            SqlSegment segment) {

        if (stack.isEmpty()) {

            script.addSegment(segment);

        } else {

            stack.peek()
                    .addSegment(segment);
        }
    }

    private static void pop(
            Deque<ContainerSegment> stack,
            Class<?> type) {

        if (stack.isEmpty()) {

            throw new IllegalStateException(
                    "unexpected closing tag"
            );
        }

        ContainerSegment segment =
                stack.pop();

        if (!type.isInstance(segment)) {

            throw new IllegalStateException(
                    "tag mismatch, expect "
                            + type.getSimpleName()
                            + " but "
                            + segment.getClass()
                            .getSimpleName()
            );
        }
    }
}