package tech.smartboot.feat.cloud.aot.orm.script;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL Script Tokenizer
 *
 * 支持：
 * <script>
 * <if>
 * <where>
 * <trim>
 * <foreach>
 *
 * @author 三刀
 */
public final class SqlTokenizer {

    private static final Pattern ATTR_PATTERN =
            Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]*)\"");

    private SqlTokenizer() {
    }

    public static List<Token> tokenize(String script) {

        List<Token> tokens = new ArrayList<Token>();

        int pos = 0;

        while (pos < script.length()) {

            int tagStart = script.indexOf('<', pos);

            if (tagStart == -1) {

                addText(
                        tokens,
                        script.substring(pos)
                );

                break;
            }

            if (tagStart > pos) {

                addText(
                        tokens,
                        script.substring(pos, tagStart)
                );
            }

            int tagEnd = script.indexOf('>', tagStart);

            if (tagEnd == -1) {
                throw new IllegalStateException(
                        "invalid script, tag not closed"
                );
            }

            String tag =
                    script.substring(
                            tagStart + 1,
                            tagEnd
                    ).trim();

            parseTag(tokens, tag);

            pos = tagEnd + 1;
        }

        return tokens;
    }

    private static void addText(
            List<Token> tokens,
            String text) {

        if (text == null ||
                text.trim().isEmpty()) {
            return;
        }

        tokens.add(
                new Token(
                        TokenType.TEXT,
                        text
                )
        );
    }

    private static void parseTag(
            List<Token> tokens,
            String tag) {

        if ("script".equals(tag)
                || "/script".equals(tag)) {
            return;
        }

        if ("if".equals(tag)) {
            throw new IllegalStateException(
                    "if tag requires test attribute"
            );
        }

        if (tag.startsWith("if ")) {

            Token token =
                    new Token(
                            TokenType.IF_START,
                            null
                    );

            parseAttributes(tag, token);

            tokens.add(token);
            return;
        }

        if ("/if".equals(tag)) {

            tokens.add(
                    new Token(
                            TokenType.IF_END,
                            null
                    )
            );
            return;
        }

        if ("where".equals(tag)) {

            tokens.add(
                    new Token(
                            TokenType.WHERE_START,
                            null
                    )
            );
            return;
        }

        if ("/where".equals(tag)) {

            tokens.add(
                    new Token(
                            TokenType.WHERE_END,
                            null
                    )
            );
            return;
        }

        if (tag.startsWith("trim")) {

            Token token =
                    new Token(
                            TokenType.TRIM_START,
                            null
                    );

            parseAttributes(tag, token);

            tokens.add(token);
            return;
        }

        if ("/trim".equals(tag)) {

            tokens.add(
                    new Token(
                            TokenType.TRIM_END,
                            null
                    )
            );
            return;
        }

        if (tag.startsWith("foreach")) {

            Token token =
                    new Token(
                            TokenType.FOREACH_START,
                            null
                    );

            parseAttributes(tag, token);

            tokens.add(token);
            return;
        }

        if ("/foreach".equals(tag)) {

            tokens.add(
                    new Token(
                            TokenType.FOREACH_END,
                            null
                    )
            );
            return;
        }

        throw new IllegalStateException(
                "unsupported tag: <" + tag + ">"
        );
    }

    private static void parseAttributes(
            String tag,
            Token token) {

        Matcher matcher =
                ATTR_PATTERN.matcher(tag);

        while (matcher.find()) {

            token.getAttributes().put(
                    matcher.group(1),
                    matcher.group(2)
            );
        }
    }
}