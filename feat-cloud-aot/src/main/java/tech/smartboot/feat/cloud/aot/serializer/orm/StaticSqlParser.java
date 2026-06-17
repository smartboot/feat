
package tech.smartboot.feat.cloud.aot.serializer.orm;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析静态 SQL 中的 #{...} 与 ${...} 占位符。
 *
 * <p>对于 #{...} 生成 JDBC 问号占位符；对于 ${...} 生成运行时字符串拼接表达式。</p>
 */
public final class StaticSqlParser {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("(#\\{|\\$\\{)\\s*([a-zA-Z0-9_.]+)\\b[^}]*\\}");

    private StaticSqlParser() {
    }

    /**
     * 将原始 SQL 转换为可嵌入生成代码的 Java 字符串表达式。
     *
     * @param sql    原始 SQL，可能包含 #{...} / ${...}
     * @param method 当前方法，用于解析参数表达式
     * @return Java 表达式，运行时求值为最终 SQL 字符串
     */
    public static String toSqlExpression(String sql, ExecutableElement method) {
        List<ParameterMetadata> paramInfos = ParameterResolver.resolve(method);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(sql);
        StringBuilder expr = new StringBuilder();
        int last = 0;
        boolean first = true;
        while (matcher.find()) {
            String before = JdbcCodeEmitter.escapeSql(sql.substring(last, matcher.start()));
            String type = matcher.group(1);
            String name = matcher.group(2);
            if (first) {
                expr.append('"').append(before).append('"');
                first = false;
            } else {
                expr.append(" + \"").append(before).append('"');
            }
            if ("#{".equals(type)) {
                expr.append(" + \"?\"");
            } else {
                expr.append(" + String.valueOf(").append(ParameterResolver.toExpression(name, paramInfos)).append(')');
            }
            last = matcher.end();
        }
        String after = JdbcCodeEmitter.escapeSql(sql.substring(last));
        if (first) {
            expr.append('"').append(after).append('"');
        } else if (!after.isEmpty()) {
            expr.append(" + \"").append(after).append('"');
        }
        return expr.toString();
    }

    /**
     * 提取 #{...} 占位符名称（用于后续 setObject 绑定）。
     */
    public static List<String> extractParameters(String sql) {
        return FeatSqlParser.parse(sql).getParameters();
    }
}
