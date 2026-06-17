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

import tech.smartboot.feat.cloud.annotation.orm.Result;
import tech.smartboot.feat.cloud.annotation.orm.Results;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 负责生成结果集与 Java Bean 的映射代码。
 */
public final class ResultMappingGenerator {
    private ResultMappingGenerator() {
    }

    public static void emitBeanMapping(PrintWriter printWriter, TypeMirror componentType, Results results, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        List<Element> fields = BeanResultMapper.extractFields(componentType);
        Map<String, Result> resultMap = ResultMapRegistry.toResultMap(results);

        for (Element field : fields) {
            String fieldName = field.getSimpleName().toString();
            Result result = resultMap.get(fieldName);
            if (isNested(result)) {
                continue;
            }
            TypeMirror fieldType = resolveFieldType(field, result);
            if (!JdbcTypeMapping.isScalar(fieldType)) {
                continue;
            }
            printWriter.println("\t\t\t\t\t\t\tint _" + fieldName + "Index = -1;");
        }

        printWriter.println("\t\t\t\t\t\t\tjava.sql.ResultSetMetaData _metaData = _rs.getMetaData();");
        printWriter.println("\t\t\t\t\t\t\tfor (int _i = 1; _i <= _metaData.getColumnCount(); _i++) {");
        printWriter.println("\t\t\t\t\t\t\t\tString _columnLabel = _metaData.getColumnLabel(_i);");
        for (Element field : fields) {
            String fieldName = field.getSimpleName().toString();
            Result result = resultMap.get(fieldName);
            if (isNested(result)) {
                continue;
            }
            TypeMirror fieldType = resolveFieldType(field, result);
            if (!JdbcTypeMapping.isScalar(fieldType)) {
                continue;
            }
            String columnLabel = fieldName;
            if (result != null) {
                columnLabel = result.column();
            }
            printWriter.println("\t\t\t\t\t\t\t\tif (_" + fieldName + "Index == -1 && \"" + columnLabel + "\".equalsIgnoreCase(_columnLabel)) {");
            printWriter.println("\t\t\t\t\t\t\t\t\t_" + fieldName + "Index = _i;");
            printWriter.println("\t\t\t\t\t\t\t\t\tcontinue;");
            printWriter.println("\t\t\t\t\t\t\t\t}");
        }
        printWriter.println("\t\t\t\t\t\t\t}");
    }

    public static void emitBeanPopulation(PrintWriter printWriter, TypeMirror componentType, Results results, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        List<Element> fields = BeanResultMapper.extractFields(componentType);
        Map<String, Result> resultMap = ResultMapRegistry.toResultMap(results);
        printWriter.println("\t\t\t\t\t\t\t\t" + componentType.toString() + " _result = new " + componentType.toString() + "();");
        for (Element field : fields) {
            String fieldName = field.getSimpleName().toString();
            Result result = resultMap.get(fieldName);
            if (result != null && isNested(result)) {
                emitNestedPopulation(printWriter, field, result, mapperElement, processingEnv);
                continue;
            }
            TypeMirror fieldType = resolveFieldType(field, result);
            if (!JdbcTypeMapping.isScalar(fieldType)) {
                continue;
            }
            String setter = "set" + capitalize(fieldName);
            String rsMethod = JdbcTypeMapping.resultSetGetter(fieldType);
            TypeMirror typeHandler = resolveTypeHandler(result);
            String rsExpr;
            if (typeHandler != null) {
                rsExpr = JdbcCodeEmitter.typeHandlerResultExpr(fieldType, typeHandler.toString(), "_" + fieldName + "Index");
            } else if (rsMethod == null) {
                throw new RuntimeException("不支持的字段类型: " + fieldType);
            } else {
                rsExpr = buildResultGetter("_" + fieldName + "Index", rsMethod, fieldType);
            }
            printWriter.println("\t\t\t\t\t\t\t\tif (_" + fieldName + "Index != -1) {");
            printWriter.println("\t\t\t\t\t\t\t\t\t_result." + setter + "(" + rsExpr + ");");
            printWriter.println("\t\t\t\t\t\t\t\t}");
        }
    }

    private static boolean isNested(Result result) {
        if (result == null) {
            return false;
        }
        return !result.one().select().isEmpty() || !result.many().select().isEmpty();
    }

    private static TypeMirror resolveFieldType(Element field, Result result) {
        TypeMirror type = field.asType();
        if (result != null) {
            TypeMirror overrideType = resolveJavaType(result);
            if (overrideType != null) {
                type = overrideType;
            }
        }
        return type;
    }

    private static void emitNestedPopulation(PrintWriter printWriter, Element field, Result result, TypeElement mapperElement, ProcessingEnvironment processingEnv) {
        String fieldName = field.getSimpleName().toString();
        String setter = "set" + capitalize(fieldName);
        String select = result.one().select().isEmpty() ? result.many().select() : result.one().select();
        if (select.isEmpty()) {
            throw new RuntimeException("@One / @Many 必须指定 select");
        }
        String column = result.column();
        if (select.contains(".")) {
            emitCrossMapperNestedPopulation(printWriter, fieldName, setter, select, column, processingEnv);
        } else {
            ExecutableElement targetMethod = findMethod(mapperElement, select);
            if (targetMethod == null) {
                throw new RuntimeException("嵌套查询方法不存在: " + select);
            }
            if (targetMethod.getParameters().size() != 1) {
                throw new RuntimeException("嵌套查询方法必须只有一个参数: " + select);
            }
            String paramType = targetMethod.getParameters().get(0).asType().toString();
            String returnType = targetMethod.getReturnType().toString();
            printWriter.println("\t\t\t\t\t\t\t" + paramType + " _nestedParam" + fieldName + " = _rs.getObject(\"" + column + "\", " + paramType + ".class);");
            printWriter.println("\t\t\t\t\t\t\t" + returnType + " _nestedValue" + fieldName + " = this." + select + "(_nestedParam" + fieldName + ");");
            printWriter.println("\t\t\t\t\t\t\t_result." + setter + "(_nestedValue" + fieldName + ");");
        }
    }

    private static void emitCrossMapperNestedPopulation(PrintWriter printWriter, String fieldName, String setter,
                                                        String select, String column, ProcessingEnvironment processingEnv) {
        int dot = select.lastIndexOf('.');
        String namespace = select.substring(0, dot);
        String methodName = select.substring(dot + 1);
        TypeElement targetMapper = processingEnv.getElementUtils().getTypeElement(namespace);
        if (targetMapper == null) {
            throw new RuntimeException("跨 Mapper 嵌套查询找不到目标 Mapper: " + namespace);
        }
        ExecutableElement targetMethod = findMethod(targetMapper, methodName);
        if (targetMethod == null) {
            throw new RuntimeException("跨 Mapper 嵌套查询方法不存在: " + select);
        }
        if (targetMethod.getParameters().size() != 1) {
            throw new RuntimeException("跨 Mapper 嵌套查询方法必须只有一个参数: " + select);
        }
        TypeMirror paramType = targetMethod.getParameters().get(0).asType();
        TypeMirror returnType = targetMethod.getReturnType();
        String rawParamType = processingEnv.getTypeUtils().erasure(paramType).toString();
        String beanName = lowerFirst(targetMapper.getSimpleName().toString());
        String mapperType = targetMapper.getQualifiedName().toString();
        printWriter.println("\t\t\t\t\t\t\t" + rawParamType + " _nestedParam" + fieldName + " = _rs.getObject(\"" + column + "\", " + rawParamType + ".class);");
        printWriter.println("\t\t\t\t\t\t\t" + returnType + " _nestedValue" + fieldName + " = ((" + mapperType + ") applicationContext.getBean(\"" + beanName + "\"))." + methodName + "(_nestedParam" + fieldName + ");");
        printWriter.println("\t\t\t\t\t\t\t_result." + setter + "(_nestedValue" + fieldName + ");");
    }

    private static ExecutableElement findMethod(TypeElement mapperElement, String methodName) {
        return findMethod(mapperElement, methodName, -1);
    }

    private static ExecutableElement findMethod(TypeElement mapperElement, String methodName, int paramCount) {
        List<ExecutableElement> matches = new ArrayList<>();
        for (Element e : mapperElement.getEnclosedElements()) {
            if (e.getKind() != javax.lang.model.element.ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) e;
            if (method.getSimpleName().toString().equals(methodName)) {
                if (paramCount < 0 || method.getParameters().size() == paramCount) {
                    matches.add(method);
                }
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        for (ExecutableElement method : matches) {
            if (!method.getReturnType().getKind().equals(TypeKind.VOID)) {
                return method;
            }
        }
        return matches.get(0);
    }

    private static String buildResultGetter(String indexExpr, String rsMethod, TypeMirror javaType) {
        if ("getObject".equals(rsMethod)) {
            return "_rs.getObject(" + indexExpr + ", " + javaType.toString() + ".class)";
        }
        return "_rs." + rsMethod + "(" + indexExpr + ")";
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static TypeMirror resolveJavaType(Result result) {
        try {
            result.javaType();
        } catch (MirroredTypeException e) {
            TypeMirror type = e.getTypeMirror();
            if (!"void".equals(type.toString())) {
                return type;
            }
        }
        return null;
    }

    private static TypeMirror resolveTypeHandler(Result result) {
        if (result == null) {
            return null;
        }
        try {
            result.typeHandler();
        } catch (MirroredTypeException e) {
            TypeMirror type = e.getTypeMirror();
            if (!"void".equals(type.toString())) {
                return type;
            }
        }
        return null;
    }
}
