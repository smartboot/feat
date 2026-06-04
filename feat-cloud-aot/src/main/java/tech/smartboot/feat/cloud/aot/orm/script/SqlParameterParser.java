package tech.smartboot.feat.cloud.aot.orm.script;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlParameterParser {

    private static final Pattern PARAM_PATTERN =
            Pattern.compile(
                    "#\\{\\s*([a-zA-Z0-9_\\.]+)\\s*\\}"
            );

    private SqlParameterParser() {
    }

    public static TextSegment parse(String sql) {

        List<String> params =
                new ArrayList<String>();

        Matcher matcher =
                PARAM_PATTERN.matcher(sql);

        StringBuilder builder =
                new StringBuilder();

        int last = 0;

        while (matcher.find()) {

            builder.append(
                    sql,
                    last,
                    matcher.start()
            );

            builder.append("?");

            params.add(
                    matcher.group(1)
            );

            last = matcher.end();
        }

        builder.append(
                sql.substring(last)
        );

        return new TextSegment(
                builder.toString(),
                params
        );
    }
}