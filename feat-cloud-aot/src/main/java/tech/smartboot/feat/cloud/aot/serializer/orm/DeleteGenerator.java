/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *  and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer.orm;

import tech.smartboot.feat.cloud.annotation.orm.Delete;
import tech.smartboot.feat.cloud.annotation.orm.Options;
import tech.smartboot.feat.cloud.annotation.orm.StatementType;

import javax.lang.model.element.ExecutableElement;
import java.io.PrintWriter;
import java.util.List;

/**
 * 生成 @Delete 方法的 JDBC 实现。
 */
public final class DeleteGenerator {
    private DeleteGenerator() {
    }

    public static void generate(PrintWriter printWriter, ExecutableElement method, Delete delete) {
        generate(printWriter, method, delete.value());
    }

    public static void generate(PrintWriter printWriter, ExecutableElement method, String sql) {
        SqlText sqlText = FeatSqlParser.parse(sql);
        List<ParameterMetadata> paramInfos = ParameterResolver.resolve(method);
        Options options = method.getAnnotation(Options.class);
        int timeout = options == null ? 0 : options.timeout();
        int fetchSize = options == null ? 0 : options.fetchSize();
        StatementType statementType = options == null ? StatementType.PREPARED : options.statementType();

        if (statementType != StatementType.STATEMENT) {
            JdbcCodeEmitter.emitParameterBindings(printWriter, sqlText.getParameters(), paramInfos);
        }
        if (timeout > 0) {
            printWriter.println("\t\t\t\t\t_ps.setQueryTimeout(" + timeout + ");");
        }
        JdbcCodeEmitter.emitFetchSize(printWriter, fetchSize, 5);
        printWriter.println("\t\t\t\t\tint _count = " + JdbcCodeEmitter.executeUpdateExpr(statementType, "_sql", false, null) + ";");
        if (!"void".equals(method.getReturnType().toString())) {
            printWriter.println("\t\t\t\t\treturn _count;");
        }
    }
}
