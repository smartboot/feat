package tech.smartboot.feat.cloud.aot.serializer;

import tech.smartboot.feat.cloud.annotation.orm.Options;
import tech.smartboot.feat.cloud.annotation.orm.ResultSetType;
import tech.smartboot.feat.cloud.annotation.orm.StatementType;
import tech.smartboot.feat.cloud.aot.serializer.orm.DeleteGenerator;
import tech.smartboot.feat.cloud.aot.serializer.orm.InsertGenerator;
import tech.smartboot.feat.cloud.aot.serializer.orm.JdbcCodeEmitter;
import tech.smartboot.feat.cloud.aot.serializer.orm.MapperMethodSql;
import tech.smartboot.feat.cloud.aot.serializer.orm.SelectGenerator;
import tech.smartboot.feat.cloud.aot.serializer.orm.StaticSqlParser;
import tech.smartboot.feat.cloud.aot.serializer.orm.UpdateGenerator;
import tech.smartboot.feat.cloud.aot.serializer.orm.dynamic.DynamicSqlCodeGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.io.PrintWriter;
import java.sql.Connection;

/**
 * 生成单个 Feat Mapper 方法的 JDBC 实现。
 *
 * <p>SQL 来源（{@code @Select}/{@code @Insert}/...）统一由 {@link MapperMethodSql} 解析，
 * 本类只负责按静态/动态 SQL 分发给对应生成器，避免单文件膨胀。</p>
 */
public final class FeatMapperMethodGenerator {

    private FeatMapperMethodGenerator() {
    }

    public static void generate(PrintWriter printWriter, ExecutableElement method, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        MapperMethodSql model = MapperMethodSql.resolve(method, mapperElement, processingEnv);
        if (model == null) {
            return;
        }

        Options options = method.getAnnotation(Options.class);
        ResultSetType resultSetType = options == null ? ResultSetType.FORWARD_ONLY : options.resultSetType();
        StatementType statementType = options == null ? StatementType.PREPARED : options.statementType();

        JdbcCodeEmitter.validateConfiguration(model.getSql(), model.isUseGeneratedKeys(), model.getKeyColumn(),
                resultSetType, statementType, model.getKind());

        JdbcCodeEmitter.emitMethodSignature(printWriter, method);

        if (DynamicSqlCodeGenerator.isDynamic(model.getSql())) {
            printWriter.println("\t\t\t\ttry (Connection _connection = dataSource.getConnection()) {");
            switch (model.getKind()) {
                case SELECT:
                    DynamicSqlCodeGenerator.generateSelect(printWriter, method, model.getSql(), model.getResults(),
                            model.getExplicitResultType(), mapperElement, processingEnv);
                    break;
                case INSERT:
                    DynamicSqlCodeGenerator.generateInsert(printWriter, method, model.getSql(), model.isUseGeneratedKeys(),
                            model.getKeyProperty(), model.getKeyColumn(), mapperElement, processingEnv);
                    break;
                case UPDATE:
                    DynamicSqlCodeGenerator.generateUpdate(printWriter, method, model.getSql(), mapperElement, processingEnv);
                    break;
                case DELETE:
                    DynamicSqlCodeGenerator.generateDelete(printWriter, method, model.getSql(), mapperElement, processingEnv);
                    break;
                default:
                    throw new RuntimeException("不支持的 SQL 类型: " + model.getKind());
            }
        } else {
            printWriter.println("\t\t\t\ttry (Connection _connection = dataSource.getConnection();");
            boolean generatedKeys = model.getKind() == MapperMethodSql.Kind.INSERT && model.isUseGeneratedKeys();
            emitStaticSqlBlock(printWriter, method, model.getSql(), generatedKeys, model.getKeyColumn(), resultSetType, statementType);
            switch (model.getKind()) {
                case SELECT:
                    SelectGenerator.generate(printWriter, method, model.getSql(), model.getResults(),
                            model.getExplicitResultType(), mapperElement, processingEnv);
                    break;
                case INSERT:
                    InsertGenerator.generate(printWriter, method, model.getSql(), model.isUseGeneratedKeys(),
                            model.getKeyProperty(), model.getKeyColumn());
                    break;
                case UPDATE:
                    UpdateGenerator.generate(printWriter, method, model.getSql());
                    break;
                case DELETE:
                    DeleteGenerator.generate(printWriter, method, model.getSql());
                    break;
                default:
                    throw new RuntimeException("不支持的 SQL 类型: " + model.getKind());
            }
        }

        JdbcCodeEmitter.emitCatch(printWriter);
        printWriter.println("\t\t\t}");
        printWriter.println();
    }

    private static void emitStaticSqlBlock(PrintWriter printWriter, ExecutableElement method, String sql, boolean generatedKeys,
                                           String keyColumn, ResultSetType resultSetType, StatementType statementType) {
        String sqlExpr = StaticSqlParser.toSqlExpression(sql, method);
        String prepareArgs = JdbcCodeEmitter.statementPrepareArgs(statementType, generatedKeys, keyColumn, resultSetType);
        String stmtClass = JdbcCodeEmitter.statementClass(statementType);
        String prepareMethod = JdbcCodeEmitter.prepareMethod(statementType);
        if (statementType == StatementType.STATEMENT) {
            printWriter.println("\t\t\t\t\t" + stmtClass + " _ps = _connection." + prepareMethod + "(" + prepareArgs + ")) {");
            printWriter.println("\t\t\t\t\tString _sql = " + sqlExpr + ";");
        } else {
            printWriter.println("\t\t\t\t\t" + stmtClass + " _ps = _connection." + prepareMethod + "(" + sqlExpr + prepareArgs + ")) {");
        }
    }
}
