package tech.smartboot.feat.cloud.aot.orm.script;

import java.util.HashMap;
import java.util.Map;

public class Token {

    private final TokenType type;

    private final String content;

    private final Map<String, String> attributes =
            new HashMap<String, String>();

    public Token(
            TokenType type,
            String content) {

        this.type = type;
        this.content = content;
    }

    public TokenType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}