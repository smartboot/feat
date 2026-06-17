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

import javax.annotation.processing.ProcessingEnvironment;
import tech.smartboot.feat.cloud.annotation.orm.MapKey;
import tech.smartboot.feat.cloud.annotation.orm.Arg;
import tech.smartboot.feat.cloud.annotation.orm.ConstructorArgs;
import tech.smartboot.feat.cloud.annotation.orm.Select;
import tech.smartboot.feat.cloud.annotation.orm.Results;
import tech.smartboot.feat.cloud.annotation.orm.Options;
import tech.smartboot.feat.cloud.annotation.orm.StatementType;
import tech.smartboot.feat.cloud.aot.serializer.orm.ConstructorMappingGenerator;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * 生成 @Select 方法的 JDBC 实现。
 */
public final class SelectGenerator {
    private SelectGenerator() {
    }

    public static void generate(PrintWriter printWriter, ExecutableElement method, Select select, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        Map<String, Results> resultMapRegistry = ResultMapRegistry.build(mapperElement);
        Results results = ResultMapRegistry.resolve(method, select, resultMapRegistry);
        generate(printWriter, method, select.value(), results, extractResultType(select), mapperElement, processingEnv);
    }

    public static void generate(PrintWriter printWriter, ExecutableElement method, String sql, Results results,
                                TypeMirror explicitResultType, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
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

        SqlText sqlText = FeatSqlParser.parse(sql);
        List<ParameterMetadata> paramInfos = ParameterResolver.resolve(method);
        String jdbcSql = JdbcCodeEmitter.escapeSql(sqlText.getSql());

        Options options = method.getAnnotation(Options.class);
        int timeout = options == null ? 0 : options.timeout();
        int fetchSize = options == null ? 0 : options.fetchSize();
        StatementType statementType = options == null ? StatementType.PREPARED : options.statementType();

        MapKey mapKey = method.getAnnotation(MapKey.class);
        if (mapKey != null && isMapType(declaredReturn)) {
        generateKeyedMap(printWriter, declaredReturn, jdbcSql, sqlText.getParameters(), paramInfos, results, timeout, fetchSize, statementType, mapKey.value(), mapperElement, processingEnv);
            return;
        }

        ConstructorArgs constructorArgs = method.getAnnotation(ConstructorArgs.class);
       if (constructorArgs != null && constructorArgs.value().length > 0) {
        generateConstructorBean(printWriter, declaredReturn, componentType, isList, jdbcSql, sqlText.getParameters(), paramInfos, constructorArgs, timeout, fetchSize, statementType, mapperElement, processingEnv);
           return;
       }

        if (JdbcTypeMapping.isScalar(componentType)) {
            generateScalar(printWriter, declaredReturn, componentType, isList, jdbcSql, sqlText.getParameters(), paramInfos, timeout, fetchSize, statementType);
            return;
        }
        if (isMapType(componentType)) {
            generateMap(printWriter, declaredReturn, componentType, isList, jdbcSql, sqlText.getParameters(), paramInfos, timeout, fetchSize, statementType);
            return;
        }

        generateBean(printWriter, declaredReturn, componentType, isList, jdbcSql, sqlText.getParameters(), paramInfos, results, timeout, fetchSize, statementType, mapperElement, processingEnv);
    }

    private static boolean isMapType(TypeMirror type) {
        return type.toString().startsWith("java.util.Map");
    }

    private static void generateScalar(PrintWriter printWriter, TypeMirror declaredReturn,
                                       TypeMirror componentType, boolean isList,
                                       String jdbcSql, List<String> parameters,
                                       List<ParameterMetadata> paramInfos, int timeout, int fetchSize, StatementType statementType) {
        String rsMethod = JdbcTypeMapping.resultSetGetter(componentType);
        if (rsMethod == null) {
            throw new RuntimeException("不支持的返回类型: " + componentType);
        }
        if (isList) {
            printWriter.println("\t\t\t\t\t" + declaredReturn + " _list = new java.util.ArrayList<>();");
        }
        if (statementType != StatementType.STATEMENT) {
            JdbcCodeEmitter.emitParameterBindings(printWriter, parameters, paramInfos);
        }
        if (timeout > 0) {
            printWriter.println("\t\t\t\t\t_ps.setQueryTimeout(" + timeout + ");");
        }
        JdbcCodeEmitter.emitFetchSize(printWriter, fetchSize, 5);
        printWriter.println("\t\t\t\t\ttry (java.sql.ResultSet _rs = " + JdbcCodeEmitter.executeQueryExpr(statementType, "_sql") + ") {");
        if (isList) {
            printWriter.println("\t\t\t\t\t\twhile (_rs.next()) {");
            printWriter.println("\t\t\t\t\t\t\t_list.add(" + buildResultGetter("1", rsMethod, componentType) + ");");
            printWriter.println("\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\treturn _list;");
        } else {
            printWriter.println("\t\t\t\t\t\tif (_rs.next()) {");
            printWriter.println("\t\t\t\t\t\t\treturn " + buildResultGetter("1", rsMethod, componentType) + ";");
            printWriter.println("\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t}");
            if (componentType.getKind().isPrimitive()) {
                printWriter.println("\t\t\t\t\treturn " + JdbcTypeMapping.primitiveDefault(componentType) + ";");
            } else {
                printWriter.println("\t\t\t\t\treturn null;");
            }
        }
    }

    private static void generateMap(PrintWriter printWriter, TypeMirror declaredReturn,
                                    TypeMirror componentType, boolean isList,
                                    String jdbcSql, List<String> parameters,
                                    List<ParameterMetadata> paramInfos, int timeout, int fetchSize, StatementType statementType) {
        if (isList) {
            printWriter.println("\t\t\t\t\t" + declaredReturn + " _list = new java.util.ArrayList<>();");
        }
        if (statementType != StatementType.STATEMENT) {
            JdbcCodeEmitter.emitParameterBindings(printWriter, parameters, paramInfos);
        }
        if (timeout > 0) {
            printWriter.println("\t\t\t\t\t_ps.setQueryTimeout(" + timeout + ");");
        }
        JdbcCodeEmitter.emitFetchSize(printWriter, fetchSize, 5);
        printWriter.println("\t\t\t\t\ttry (java.sql.ResultSet _rs = " + JdbcCodeEmitter.executeQueryExpr(statementType, "_sql") + ") {");
        printWriter.println("\t\t\t\t\t\tjava.sql.ResultSetMetaData _metaData = _rs.getMetaData();");
        if (isList) {
            printWriter.println("\t\t\t\t\t\twhile (_rs.next()) {");
            printWriter.println("\t\t\t\t\t\t\tjava.util.Map<String, Object> _result = new java.util.HashMap<>();");
            printWriter.println("\t\t\t\t\t\t\tfor (int _i = 1; _i <= _metaData.getColumnCount(); _i++) {");
            printWriter.println("\t\t\t\t\t\t\t\t_result.put(_metaData.getColumnLabel(_i), _rs.getObject(_i));");
            printWriter.println("\t\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t\t\t_list.add(_result);");
            printWriter.println("\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t\treturn _list;");
        } else {
            printWriter.println("\t\t\t\t\t\tif (_rs.next()) {");
            printWriter.println("\t\t\t\t\t\t\tjava.util.Map<String, Object> _result = new java.util.HashMap<>();");
            printWriter.println("\t\t\t\t\t\t\tfor (int _i = 1; _i <= _metaData.getColumnCount(); _i++) {");
            printWriter.println("\t\t\t\t\t\t\t\t_result.put(_metaData.getColumnLabel(_i), _rs.getObject(_i));");
            printWriter.println("\t\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t\t\treturn _result;");
            printWriter.println("\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t\treturn null;");
        }
        printWriter.println("\t\t\t\t\t}");
    }

    private static void generateKeyedMap(PrintWriter printWriter, TypeMirror declaredReturn,
                                         String jdbcSql,
                                         List<String> parameters, List<ParameterMetadata> paramInfos,
                                         Results results, int timeout, int fetchSize, StatementType statementType, String keyProperty, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        List<? extends TypeMirror> typeArgs = ((DeclaredType) declaredReturn).getTypeArguments();
        if (typeArgs.size() != 2) {
            throw new RuntimeException("@MapKey 方法必须声明 Map<K, V> 泛型");
        }
        TypeMirror keyType = typeArgs.get(0);
        TypeMirror valueType = typeArgs.get(1);
        if (JdbcTypeMapping.isScalar(valueType)) {
            throw new RuntimeException("@MapKey 暂不支持标量 value 类型");
        }
        printWriter.println("\t\t\t\t\t" + declaredReturn + " _map = new java.util.HashMap<>();");
        if (statementType != StatementType.STATEMENT) {
            JdbcCodeEmitter.emitParameterBindings(printWriter, parameters, paramInfos);
        }
        if (timeout > 0) {
            printWriter.println("\t\t\t\t\t_ps.setQueryTimeout(" + timeout + ");");
        }
        JdbcCodeEmitter.emitFetchSize(printWriter, fetchSize, 5);
        printWriter.println("\t\t\t\t\ttry (java.sql.ResultSet _rs = " + JdbcCodeEmitter.executeQueryExpr(statementType, "_sql") + ") {");
        ResultMappingGenerator.emitBeanMapping(printWriter, valueType, results, mapperElement, processingEnv);
        printWriter.println("\t\t\t\t\t\twhile (_rs.next()) {");
        ResultMappingGenerator.emitBeanPopulation(printWriter, valueType, results, mapperElement, processingEnv);
        printWriter.println("\t\t\t\t\t\t\t_map.put(_result.get" + capitalize(keyProperty) + "(), _result);");
        printWriter.println("\t\t\t\t\t\t}");
        printWriter.println("\t\t\t\t\t}");
        printWriter.println("\t\t\t\t\treturn _map;");
    }

    private static void generateBean(PrintWriter printWriter, TypeMirror declaredReturn,
                                    TypeMirror componentType, boolean isList,
                                    String jdbcSql, List<String> parameters,
                                    List<ParameterMetadata> paramInfos, Results results, int timeout, int fetchSize, StatementType statementType, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        if (isList) {
            printWriter.println("\t\t\t\t\t" + declaredReturn + " _list = new java.util.ArrayList<>();");
        }
        if (statementType != StatementType.STATEMENT) {
            JdbcCodeEmitter.emitParameterBindings(printWriter, parameters, paramInfos);
        }
        if (timeout > 0) {
            printWriter.println("\t\t\t\t\t_ps.setQueryTimeout(" + timeout + ");");
        }
        JdbcCodeEmitter.emitFetchSize(printWriter, fetchSize, 5);
        printWriter.println("\t\t\t\t\ttry (java.sql.ResultSet _rs = " + JdbcCodeEmitter.executeQueryExpr(statementType, "_sql") + ") {");
        ResultMappingGenerator.emitBeanMapping(printWriter, componentType, results, mapperElement, processingEnv);
        if (isList) {
            printWriter.println("\t\t\t\t\t\twhile (_rs.next()) {");
        } else {
            printWriter.println("\t\t\t\t\t\tif (_rs.next()) {");
        }
        ResultMappingGenerator.emitBeanPopulation(printWriter, componentType, results, mapperElement, processingEnv);
        if (isList) {
            printWriter.println("\t\t\t\t\t\t\t_list.add(_result);");
            printWriter.println("\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\treturn _list;");
        } else {
            printWriter.println("\t\t\t\t\t\t\treturn _result;");
            printWriter.println("\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\treturn null;");
        }
    }

    private static void generateConstructorBean(PrintWriter printWriter, TypeMirror declaredReturn,
                                               TypeMirror componentType, boolean isList,
                                               String jdbcSql, List<String> parameters,
                                               List<ParameterMetadata> paramInfos,
                                               ConstructorArgs constructorArgs, int timeout, int fetchSize, StatementType statementType, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        if (isList) {
            printWriter.println("\t\t\t\t\t" + declaredReturn + " _list = new java.util.ArrayList<>();");
        }
        if (statementType != StatementType.STATEMENT) {
            JdbcCodeEmitter.emitParameterBindings(printWriter, parameters, paramInfos);
        }
        if (timeout > 0) {
            printWriter.println("\t\t\t\t\t_ps.setQueryTimeout(" + timeout + ");");
        }
        JdbcCodeEmitter.emitFetchSize(printWriter, fetchSize, 5);
        printWriter.println("\t\t\t\t\ttry (java.sql.ResultSet _rs = " + JdbcCodeEmitter.executeQueryExpr(statementType, "_sql") + ") {");
        ConstructorMappingGenerator.emitConstructorMapping(printWriter, componentType, constructorArgs, 5, mapperElement, processingEnv);
        if (isList) {
            printWriter.println("\t\t\t\t\t\twhile (_rs.next()) {");
        } else {
            printWriter.println("\t\t\t\t\t\tif (_rs.next()) {");
        }
        ConstructorMappingGenerator.emitConstructorPopulation(printWriter, componentType, constructorArgs, 5, mapperElement, processingEnv);
        if (isList) {
            printWriter.println("\t\t\t\t\t\t\t_list.add(_result);");
            printWriter.println("\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\treturn _list;");
        } else {
            printWriter.println("\t\t\t\t\t\t\treturn _result;");
            printWriter.println("\t\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\t}");
            printWriter.println("\t\t\t\t\treturn null;");
        }
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

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
