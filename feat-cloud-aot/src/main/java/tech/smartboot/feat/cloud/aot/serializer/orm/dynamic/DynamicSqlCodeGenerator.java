/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *  and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer.orm.dynamic;

import tech.smartboot.feat.cloud.annotation.orm.Delete;
import tech.smartboot.feat.cloud.annotation.orm.Insert;
import tech.smartboot.feat.cloud.annotation.orm.MapKey;
import tech.smartboot.feat.cloud.annotation.orm.SelectKey;
import tech.smartboot.feat.cloud.annotation.orm.Options;
import tech.smartboot.feat.cloud.annotation.orm.ResultSetType;
import tech.smartboot.feat.cloud.annotation.orm.StatementType;
import tech.smartboot.feat.cloud.annotation.orm.Results;
import tech.smartboot.feat.cloud.annotation.orm.Select;
import tech.smartboot.feat.cloud.annotation.orm.Update;
import tech.smartboot.feat.cloud.aot.serializer.orm.GeneratedKeyResolver;
import tech.smartboot.feat.cloud.aot.serializer.orm.JdbcCodeEmitter;
import tech.smartboot.feat.cloud.aot.serializer.orm.ResultMapRegistry;
import tech.smartboot.feat.cloud.aot.serializer.orm.SelectKeyGenerator;
import tech.smartboot.feat.cloud.aot.serializer.orm.JdbcTypeMapping;
import tech.smartboot.feat.cloud.aot.serializer.orm.ParameterMetadata;
import tech.smartboot.feat.cloud.aot.serializer.orm.ParameterResolver;
import tech.smartboot.feat.cloud.aot.serializer.orm.ResultMappingGenerator;
import tech.smartboot.feat.cloud.aot.serializer.orm.dynamic.SqlFragmentRegistry;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * 将 {@link SqlNode} AST 生成为运行时 JDBC 代码。
 *
 * <p>核心改进：不再在运行时通过 {@code FeatSqlContext.eval(...)} 解析 test 表达式，
 * 而是在 APT 阶段把 {@code <if test="...">}、{@code <bind>}、占位符等直接翻译为
 * Java 的 if-else、本地变量和参数引用，从而消除运行时的表达式解析与反射取值。</p>
 */
public final class DynamicSqlCodeGenerator {


    private DynamicSqlCodeGenerator() {
    }

    public static boolean isDynamic(String sql) {
        if (sql == null) {
            return false;
        }
        String trimmed = sql.trim();
        return trimmed.regionMatches(true, 0, "<script>", 0, 8) || trimmed.regionMatches(true, 0, "<sql", 0, 4) || trimmed.regionMatches(true, 0, "<include", 0, 8);
    }

    public static void generateSelect(PrintWriter printWriter, ExecutableElement method, Select select, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        Map<String, List<SqlNode>> fragments = SqlFragmentRegistry.build(mapperElement);
        SqlNode root = SqlFragmentRegistry.expandIncludes(DynamicSqlParser.parse(select.value()), fragments);
        Map<String, Results> resultMapRegistry = ResultMapRegistry.build(mapperElement);
        Results results = ResultMapRegistry.resolve(method, select, resultMapRegistry);
        generateSelect(printWriter, method, select.value(), results, extractResultType(select), mapperElement, processingEnv);
    }

    public static void generateSelect(PrintWriter printWriter, ExecutableElement method, String sql, Results results,
                                      TypeMirror explicitResultType, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        Map<String, List<SqlNode>> fragments = SqlFragmentRegistry.build(mapperElement);
        SqlNode root = SqlFragmentRegistry.expandIncludes(DynamicSqlParser.parse(sql), fragments);
        TypeMirror declaredReturn = method.getReturnType();
        boolean isList = declaredReturn.toString().startsWith("java.util.List");
        TypeMirror componentType = declaredReturn;
        if (isList) {
            List<? extends TypeMirror> typeArgs = ((DeclaredType) declaredReturn).getTypeArguments();
            if (typeArgs.size() != 1) {
                throw new RuntimeException("List未指定泛型");
            }
            componentType = typeArgs.get(0);
        }
        if (explicitResultType != null) {
            componentType = explicitResultType;
        }

        Options options = method.getAnnotation(Options.class);
        int timeout = options == null ? 0 : options.timeout();
        int fetchSize = options == null ? 0 : options.fetchSize();
        ResultSetType resultSetType = options == null ? ResultSetType.FORWARD_ONLY : options.resultSetType();
        StatementType statementType = options == null ? StatementType.PREPARED : options.statementType();

        final TypeMirror finalComponentType = componentType;
        final Results finalResults = results;
        MapKey mapKey = method.getAnnotation(MapKey.class);
        if (mapKey != null && isMap(declaredReturn)) {
            generateBody(printWriter, processingEnv, method, root, timeout, fetchSize, resultSetType, statementType, false, null, () -> {
                emitKeyedMapMapping(printWriter, declaredReturn, finalComponentType, finalResults, processingEnv, mapKey.value(), statementType, 5, mapperElement);
            });
            return;
        }
        generateBody(printWriter, processingEnv, method, root, timeout, fetchSize, resultSetType, statementType, false, null, () -> {
            if (JdbcTypeMapping.isScalar(finalComponentType)) {
                emitScalarMapping(printWriter, declaredReturn, finalComponentType, isList, statementType, 5);
            } else if (isMap(finalComponentType)) {
                emitMapMapping(printWriter, declaredReturn, finalComponentType, isList, statementType, 5);
            } else {
                emitBeanMapping(printWriter, declaredReturn, finalComponentType, isList, finalResults, statementType, 5, mapperElement, processingEnv);
            }
        });
    }

    public static void generateInsert(PrintWriter printWriter, ExecutableElement method, Insert insert, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
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
        generateInsert(printWriter, method, insert.value(), generatedKeys, keyProperty, keyColumn, mapperElement, processingEnv);
    }

    public static void generateInsert(PrintWriter printWriter, ExecutableElement method, String sql, boolean generatedKeys,
                                      String keyProperty, String keyColumn, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        Map<String, List<SqlNode>> fragments = SqlFragmentRegistry.build(mapperElement);
        SqlNode root = SqlFragmentRegistry.expandIncludes(DynamicSqlParser.parse(sql), fragments);
        Options options = method.getAnnotation(Options.class);
        int timeout = options == null ? 0 : options.timeout();
        int fetchSize = options == null ? 0 : options.fetchSize();
        ResultSetType resultSetType = options == null ? ResultSetType.FORWARD_ONLY : options.resultSetType();
        StatementType statementType = options == null ? StatementType.PREPARED : options.statementType();

        final boolean useKeys = generatedKeys;
        final String keyProp = keyProperty;
        final String keyCol = keyColumn;
        final SelectKey selectKey = method.getAnnotation(SelectKey.class);

        generateBody(printWriter, processingEnv, method, root, timeout, fetchSize, resultSetType, statementType, useKeys, keyCol, () -> {
            if (selectKey != null && selectKey.before()) {
                SelectKeyGenerator.emit(printWriter, method, selectKey, 5);
            }
            printWriter.println(tabs(5) + "int _count = " + JdbcCodeEmitter.dynamicExecuteUpdateExpr(statementType, useKeys, keyCol) + ";");
            if (useKeys && !keyProp.isEmpty()) {
                printWriter.println(tabs(5) + "try (java.sql.ResultSet _keys = _ps.getGeneratedKeys()) {");
                printWriter.println(tabs(6) + "if (_keys.next()) {");
                GeneratedKeyResolver.GeneratedKey key = GeneratedKeyResolver.resolveKey(method, ParameterResolver.resolve(method), keyProp);
                if (key != null && key.getSetter() != null) {
                    printWriter.println(tabs(7) + key.getSetter() + "(" + JdbcCodeEmitter.generatedKeyGetter("_keys", key.getType(), keyCol) + ");");
                }
                printWriter.println(tabs(6) + "}");
                printWriter.println(tabs(5) + "}");
            }
            if (selectKey != null && !selectKey.before()) {
                SelectKeyGenerator.emit(printWriter, method, selectKey, 5);
            }
            if (!"void".equals(method.getReturnType().toString())) {
                printWriter.println(tabs(5) + "return _count;");
            }
        });
    }

    public static void generateUpdate(PrintWriter printWriter, ExecutableElement method, Update update, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        generateUpdate(printWriter, method, update.value(), mapperElement, processingEnv);
    }

    public static void generateUpdate(PrintWriter printWriter, ExecutableElement method, String sql, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        Map<String, List<SqlNode>> fragments = SqlFragmentRegistry.build(mapperElement);
        SqlNode root = SqlFragmentRegistry.expandIncludes(DynamicSqlParser.parse(sql), fragments);
        Options options = method.getAnnotation(Options.class);
        int timeout = options == null ? 0 : options.timeout();
        int fetchSize = options == null ? 0 : options.fetchSize();
        ResultSetType resultSetType = options == null ? ResultSetType.FORWARD_ONLY : options.resultSetType();
        StatementType statementType = options == null ? StatementType.PREPARED : options.statementType();
        generateBody(printWriter, processingEnv, method, root, timeout, fetchSize, resultSetType, statementType, false, null, () -> {
            printWriter.println(tabs(5) + "int _count = " + JdbcCodeEmitter.dynamicExecuteUpdateExpr(statementType, false, null) + ";");
            if (!"void".equals(method.getReturnType().toString())) {
                printWriter.println(tabs(5) + "return _count;");
            }
        });
    }

    public static void generateDelete(PrintWriter printWriter, ExecutableElement method, Delete delete, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        generateDelete(printWriter, method, delete.value(), mapperElement, processingEnv);
    }

    public static void generateDelete(PrintWriter printWriter, ExecutableElement method, String sql, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        Map<String, List<SqlNode>> fragments = SqlFragmentRegistry.build(mapperElement);
        SqlNode root = SqlFragmentRegistry.expandIncludes(DynamicSqlParser.parse(sql), fragments);
        Options options = method.getAnnotation(Options.class);
        int timeout = options == null ? 0 : options.timeout();
        int fetchSize = options == null ? 0 : options.fetchSize();
        ResultSetType resultSetType = options == null ? ResultSetType.FORWARD_ONLY : options.resultSetType();
        StatementType statementType = options == null ? StatementType.PREPARED : options.statementType();
        generateBody(printWriter, processingEnv, method, root, timeout, fetchSize, resultSetType, statementType, false, null, () -> {
            printWriter.println(tabs(5) + "int _count = " + JdbcCodeEmitter.dynamicExecuteUpdateExpr(statementType, false, null) + ";");
            if (!"void".equals(method.getReturnType().toString())) {
                printWriter.println(tabs(5) + "return _count;");
            }
        });
    }

    private static void generateBody(PrintWriter printWriter, ProcessingEnvironment processingEnv, ExecutableElement method, SqlNode root, int timeout, int fetchSize, ResultSetType resultSetType, StatementType statementType, boolean generatedKeys, String keyColumn, Runnable executor) {
        List<ParameterMetadata> paramInfos = ParameterResolver.resolve(method);
        DynamicScope scope = new DynamicScope(paramInfos);

        printWriter.println(tabs(5) + "StringBuilder _sql = new StringBuilder();");
        printWriter.println(tabs(5) + "java.util.List<Object> _params = new java.util.ArrayList<>();");
        DynamicSqlNodeGenerator.generateNode(printWriter, root, 5, scope, "_sql", "_params", new NameGenerator(), processingEnv);

        String prepareArgs = JdbcCodeEmitter.statementPrepareArgs(statementType, generatedKeys, keyColumn, resultSetType);
        String stmtClass = JdbcCodeEmitter.statementClass(statementType);
        String prepareMethod = JdbcCodeEmitter.prepareMethod(statementType);
        if (statementType == StatementType.STATEMENT) {
            printWriter.println(tabs(5) + "try (" + stmtClass + " _ps = _connection." + prepareMethod + "(" + prepareArgs + ")) {");
        } else {
            printWriter.println(tabs(5) + "try (" + stmtClass + " _ps = _connection." + prepareMethod + "(_sql.toString()" + prepareArgs + ")) {");
        }
        if (timeout > 0) {
            printWriter.println(tabs(6) + "_ps.setQueryTimeout(" + timeout + ");");
        }
        if (fetchSize > 0) {
            printWriter.println(tabs(6) + "_ps.setFetchSize(" + fetchSize + ");");
        }
        if (statementType != StatementType.STATEMENT) {
            printWriter.println(tabs(6) + "for (int _i = 0; _i < _params.size(); _i++) {");
            printWriter.println(tabs(7) + "_ps.setObject(_i + 1, _params.get(_i));");
            printWriter.println(tabs(6) + "}");
        }
        executor.run();
        printWriter.println(tabs(5) + "}");
    }

    private static void emitScalarMapping(PrintWriter printWriter, TypeMirror declaredReturn,
                                          TypeMirror componentType, boolean isList, StatementType statementType, int indent) {
        String rsMethod = JdbcTypeMapping.resultSetGetter(componentType);
        if (rsMethod == null) {
            throw new RuntimeException("不支持的返回类型: " + componentType);
        }
        printWriter.println(tabs(indent) + "try (java.sql.ResultSet _rs = " + JdbcCodeEmitter.dynamicExecuteQueryExpr(statementType) + ") {");
        if (isList) {
            printWriter.println(tabs(indent + 1) + declaredReturn + " _list = new java.util.ArrayList<>();");
            printWriter.println(tabs(indent + 1) + "while (_rs.next()) {");
            printWriter.println(tabs(indent + 2) + "_list.add(" + buildResultGetter("1", rsMethod, componentType) + ");");
            printWriter.println(tabs(indent + 1) + "}");
            printWriter.println(tabs(indent + 1) + "return _list;");
        } else {
            printWriter.println(tabs(indent + 1) + "if (_rs.next()) {");
            printWriter.println(tabs(indent + 2) + "return " + buildResultGetter("1", rsMethod, componentType) + ";");
            printWriter.println(tabs(indent + 1) + "}");
            printWriter.println(tabs(indent + 1) + "return " + (componentType.getKind().isPrimitive()
                    ? JdbcTypeMapping.primitiveDefault(componentType) : "null") + ";");
        }
        printWriter.println(tabs(indent) + "}");
    }

    private static boolean isMap(TypeMirror type) {
        return type.toString().startsWith("java.util.Map");
    }

    private static void emitMapMapping(PrintWriter printWriter, TypeMirror declaredReturn,
                                       TypeMirror componentType, boolean isList, StatementType statementType, int indent) {
        if (isList) {
            printWriter.println(tabs(indent) + declaredReturn + " _list = new java.util.ArrayList<>();");
        }
        printWriter.println(tabs(indent) + "try (java.sql.ResultSet _rs = " + JdbcCodeEmitter.dynamicExecuteQueryExpr(statementType) + ") {");
        printWriter.println(tabs(indent + 1) + "java.sql.ResultSetMetaData _metaData = _rs.getMetaData();");
        if (isList) {
            printWriter.println(tabs(indent + 1) + "while (_rs.next()) {");
            printWriter.println(tabs(indent + 2) + "java.util.Map<String, Object> _result = new java.util.HashMap<>();");
            printWriter.println(tabs(indent + 2) + "for (int _i = 1; _i <= _metaData.getColumnCount(); _i++) {");
            printWriter.println(tabs(indent + 3) + "_result.put(_metaData.getColumnLabel(_i), _rs.getObject(_i));");
            printWriter.println(tabs(indent + 2) + "}");
            printWriter.println(tabs(indent + 2) + "_list.add(_result);");
            printWriter.println(tabs(indent + 1) + "}");
            printWriter.println(tabs(indent + 1) + "return _list;");
        } else {
            printWriter.println(tabs(indent + 1) + "if (_rs.next()) {");
            printWriter.println(tabs(indent + 2) + "java.util.Map<String, Object> _result = new java.util.HashMap<>();");
            printWriter.println(tabs(indent + 2) + "for (int _i = 1; _i <= _metaData.getColumnCount(); _i++) {");
            printWriter.println(tabs(indent + 3) + "_result.put(_metaData.getColumnLabel(_i), _rs.getObject(_i));");
            printWriter.println(tabs(indent + 2) + "}");
            printWriter.println(tabs(indent + 2) + "return _result;");
            printWriter.println(tabs(indent + 1) + "}");
            printWriter.println(tabs(indent + 1) + "return null;");
        }
        printWriter.println(tabs(indent) + "}");
    }

    private static void emitBeanMapping(PrintWriter printWriter, TypeMirror declaredReturn,
                                        TypeMirror componentType, boolean isList, Results results, StatementType statementType, int indent, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        if (isList) {
            printWriter.println(tabs(indent) + declaredReturn + " _list = new java.util.ArrayList<>();");
        }
        printWriter.println(tabs(indent) + "try (java.sql.ResultSet _rs = " + JdbcCodeEmitter.dynamicExecuteQueryExpr(statementType) + ") {");
        ResultMappingGenerator.emitBeanMapping(printWriter, componentType, results, mapperElement, processingEnv);
        printWriter.println(tabs(indent + 1) + (isList ? "while (_rs.next()) {" : "if (_rs.next()) {"));
        ResultMappingGenerator.emitBeanPopulation(printWriter, componentType, results, mapperElement, processingEnv);
        if (isList) {
            printWriter.println(tabs(indent + 2) + "_list.add(_result);");
            printWriter.println(tabs(indent + 1) + "}");
            printWriter.println(tabs(indent + 1) + "return _list;");
        } else {
            printWriter.println(tabs(indent + 2) + "return _result;");
            printWriter.println(tabs(indent + 1) + "}");
            printWriter.println(tabs(indent + 1) + "return null;");
        }
        printWriter.println(tabs(indent) + "}");
    }

    private static void emitKeyedMapMapping(PrintWriter printWriter, TypeMirror declaredReturn,
                                            TypeMirror componentType, Results results, ProcessingEnvironment processingEnv,
                                            String keyProperty, StatementType statementType, int indent, TypeElement mapperElement) {
        List<? extends TypeMirror> typeArgs = ((DeclaredType) declaredReturn).getTypeArguments();
        if (typeArgs.size() != 2) {
            throw new RuntimeException("@MapKey 方法必须声明 Map<K, V> 泛型");
        }
        TypeMirror valueType = typeArgs.get(1);
        if (JdbcTypeMapping.isScalar(valueType)) {
            throw new RuntimeException("@MapKey 暂不支持标量 value 类型");
        }
        printWriter.println(tabs(indent) + declaredReturn + " _map = new java.util.HashMap<>();");
        printWriter.println(tabs(indent) + "try (java.sql.ResultSet _rs = " + JdbcCodeEmitter.dynamicExecuteQueryExpr(statementType) + ") {");
        ResultMappingGenerator.emitBeanMapping(printWriter, valueType, results, mapperElement, processingEnv);
        printWriter.println(tabs(indent + 1) + "while (_rs.next()) {");
        ResultMappingGenerator.emitBeanPopulation(printWriter, valueType, results, mapperElement, processingEnv);
        printWriter.println(tabs(indent + 2) + "_map.put(_result.get" + capitalize(keyProperty) + "(), _result);");
        printWriter.println(tabs(indent + 1) + "}");
        printWriter.println(tabs(indent) + "}");
        printWriter.println(tabs(indent) + "return _map;");
    }

    private static String buildResultGetter(String indexExpr, String rsMethod, TypeMirror javaType) {
        if ("getObject".equals(rsMethod)) {
            return "_rs.getObject(" + indexExpr + ", " + javaType.toString() + ".class)";
        }
        return "_rs." + rsMethod + "(" + indexExpr + ")";
    }

    private static TypeMirror extractResultType(Select select) {
        try {
            select.resultType();
        } catch (MirroredTypeException e) {
            TypeMirror type = e.getTypeMirror();
            if (!"void".equals(type.toString())) {
                return type;
            }
        }
        return null;
    }

    private static String tabs(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("\t");
        }
        return sb.toString();
    }

    private static String escape(String value) {
        return JdbcCodeEmitter.escapeSql(value);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
