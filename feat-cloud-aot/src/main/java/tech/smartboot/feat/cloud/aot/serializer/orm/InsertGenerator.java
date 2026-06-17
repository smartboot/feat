package tech.smartboot.feat.cloud.aot.serializer.orm;

import tech.smartboot.feat.cloud.annotation.orm.Insert;
import tech.smartboot.feat.cloud.annotation.orm.Options;
import tech.smartboot.feat.cloud.annotation.orm.StatementType;
import tech.smartboot.feat.cloud.annotation.orm.SelectKey;

import javax.lang.model.element.ExecutableElement;
import java.io.PrintWriter;
import java.util.List;

/**
 * 生成 @Insert 方法的 JDBC 实现。
 */
public final class InsertGenerator {
    private InsertGenerator() {
    }

    public static void generate(PrintWriter printWriter, ExecutableElement method, Insert insert) {
        Options options = method.getAnnotation(Options.class);

        boolean generatedKeys = insert.useGeneratedKeys();
        String keyProperty = insert.keyProperty();
        String keyColumn = insert.keyColumn();
        if (options != null) {
            if (options.useGeneratedKeys()) {
                generatedKeys = true;
            }
            if (!options.keyProperty().isEmpty()) {
                keyProperty = options.keyProperty();
            }
            if (!options.keyColumn().isEmpty()) {
                keyColumn = options.keyColumn();
            }
        }
        generate(printWriter, method, insert.value(), generatedKeys, keyProperty, keyColumn);
    }

    public static void generate(PrintWriter printWriter, ExecutableElement method, String sql, boolean generatedKeys, String keyProperty, String keyColumn) {
        SqlText sqlText = FeatSqlParser.parse(sql);
        List<ParameterMetadata> paramInfos = ParameterResolver.resolve(method);
        Options options = method.getAnnotation(Options.class);
        int timeout = options == null ? 0 : options.timeout();
        int fetchSize = options == null ? 0 : options.fetchSize();
        StatementType statementType = options == null ? StatementType.PREPARED : options.statementType();

        SelectKey selectKey = method.getAnnotation(SelectKey.class);

        if (selectKey != null && selectKey.before()) {
            SelectKeyGenerator.emit(printWriter, method, selectKey, 5);
        }

        if (statementType != StatementType.STATEMENT) {
            JdbcCodeEmitter.emitParameterBindings(printWriter, sqlText.getParameters(), paramInfos);
        }
        if (timeout > 0) {
            printWriter.println("\t\t\t\t\t_ps.setQueryTimeout(" + timeout + ");");
        }
        JdbcCodeEmitter.emitFetchSize(printWriter, fetchSize, 5);
        printWriter.println("\t\t\t\t\tint _count = " + JdbcCodeEmitter.executeUpdateExpr(statementType, "_sql", generatedKeys, keyColumn) + ";");

        if (generatedKeys && !keyProperty.isEmpty()) {
            printWriter.println("\t\t\t\t\ttry (java.sql.ResultSet _keys = _ps.getGeneratedKeys()) {");
            printWriter.println("\t\t\t\t\t\tif (_keys.next()) {");
            GeneratedKeyResolver.GeneratedKey key = GeneratedKeyResolver.resolveKey(method, paramInfos, keyProperty);
            if (key != null && key.getSetter() != null) {
                printWriter.println("\t\t\t\t\t\t\t" + key.getSetter() + "(" + JdbcCodeEmitter.generatedKeyGetter("_keys", key.getType(), keyColumn) + ");");
            }
            printWriter.println("\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t}");
        }

        if (selectKey != null && !selectKey.before()) {
            SelectKeyGenerator.emit(printWriter, method, selectKey, 5);
        }

        if (!"void".equals(method.getReturnType().toString())) {
            printWriter.println("\t\t\t\t\treturn _count;");
        }
    }
}
