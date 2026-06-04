/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer;

import tech.smartboot.feat.cloud.annotation.orm.Result;
import tech.smartboot.feat.cloud.annotation.orm.Results;
import tech.smartboot.feat.cloud.annotation.orm.Select;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 三刀
 * @version v1.0 7/23/25
 */
public final class FeatMapperSerializer extends AbstractSerializer {
    public FeatMapperSerializer(ProcessingEnvironment processingEnv, String config, Element element) throws IOException {
        super(processingEnv, config, element);
    }

    @Override
    public void serializeImport() {
        printWriter.println("import " + DataSource.class.getName() + ";");
        printWriter.println("import " + Connection.class.getName() + ";");
        printWriter.println("import " + PreparedStatement.class.getName() + ";");
        printWriter.println("import " + ResultSet.class.getName() + ";");
        printWriter.println("import " + ResultSetMetaData.class.getName() + ";");
        printWriter.println("import " + ArrayList.class.getName() + ";");
        super.serializeImport();
    }

    @Override
    public void serializeProperty() {
        printWriter.println("\tprivate DataSource dataSource;");
        super.serializeProperty();
    }

    @Override
    public void serializeLoadBean() {
        StringBuilder sessionName = new StringBuilder("session");
        printWriter.println("\t\tbean = new " + element.getSimpleName() + "() { ");
        for (Element se : element.getEnclosedElements()) {
            String returnType = ((ExecutableElement) se).getReturnType().toString();
            printWriter.print("\t\t\tpublic " + returnType + " " + se.getSimpleName() + "(");
            boolean first = true;
            for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                if (first) {
                    first = false;
                } else {
                    printWriter.print(",");
                }
                printWriter.print(param.asType().toString() + " " + param.getSimpleName());
                //当参数名称与session相同，则调整参数名称
                if (sessionName.toString().equals(param.getSimpleName().toString())) {
                    sessionName.append("$");
                }
            }
            printWriter.println(") {");
            printWriter.println("\t\t\t\ttry{");
            printWriter.println("\t\t\t\t\tConnection connection=dataSource.getConnection();");
            Select select = se.getAnnotation(Select.class);
            if (select != null) {
                serialSelect(printWriter, se, select);
            }
            printWriter.println("\t\t\t\t} catch (Exception e) {");
            printWriter.println("\t\t\t\t\tthrow new RuntimeException(e);");
            printWriter.println("\t\t\t\t}");
            printWriter.println("\t\t\t}");
            printWriter.println();
        }
        printWriter.println("\t\t};");
        String beanName = element.getSimpleName().toString().substring(0, 1).toLowerCase() + element.getSimpleName().toString().substring(1);
        printWriter.println("\t\tapplicationContext.addBean(\"" + beanName + "\", bean);");
    }


    private void serialSelect(PrintWriter printWriter, Element se, Select select) {
        String sql = select.value();
        TypeMirror returnType = ((ExecutableElement) se).getReturnType();
        boolean isList = false;
        if (returnType.toString().startsWith("java.util.List")) {
            if (((DeclaredType) returnType).getTypeArguments().size() != 1) {
                throw new RuntimeException("List未指定泛型");
            }
            printWriter.println("\t\t\t\t\t\t" + returnType + " list = new ArrayList<>();");
            returnType = ((DeclaredType) returnType).getTypeArguments().get(0);
            isList = true;
        }
        //提取字段
        List<Element> fields = new ArrayList<>();
        for (Element e : ((DeclaredType) returnType).asElement().getEnclosedElements()) {
            if (e.getKind() != ElementKind.FIELD) {
                continue;
            }
            Set<Modifier> modifiers = e.getModifiers();
            if (modifiers.size() != 1 && !modifiers.contains(Modifier.PRIVATE)) {
                continue;
            }
            fields.add(e);
            printWriter.println("\t\t\t\t\tint " + e.getSimpleName() + "Index = -1;");
        }
        //解析Results注解
        Map<String, Result> resultMap = new HashMap<>();
        Results results = se.getAnnotation(Results.class);
        if (results != null) {
            for (Result result : results.value()) {
                resultMap.put(result.property(), result);
            }
        }


        printWriter.println("\t\t\t\t\tPreparedStatement ps = connection.prepareStatement(\"" + sql + "\");");

        int i = 1;
        for (String param : parseSqlParam(sql)) {
            printWriter.println("\t\t\t\t\tps.setObject(" + i++ + ", " + param + ");");
        }

        printWriter.println("\t\t\t\t\tResultSet rs = ps.executeQuery();");

        //识别列索引
        printWriter.println("\t\t\t\t\tResultSetMetaData metaData = rs.getMetaData();");
        printWriter.println("\t\t\t\t\tfor (int i = 1; i <= metaData.getColumnCount(); i++) {");
        printWriter.println("\t\t\t\t\t\tString columnLabel = metaData.getColumnLabel(i);");
        for (Element field : fields) {
            String columnLabel = field.getSimpleName().toString();
            if (resultMap.containsKey(columnLabel)) {
                columnLabel = resultMap.get(columnLabel).column();
            }
            printWriter.println("\t\t\t\t\t\tif (" + field.getSimpleName() + "Index == -1 && \"" + columnLabel + "\".equals(columnLabel)) {");
            printWriter.println("\t\t\t\t\t\t\t" + field.getSimpleName() + "Index = i;");
            printWriter.println("\t\t\t\t\t\t\tcontinue;");
            printWriter.println("\t\t\t\t\t\t}");
        }
        printWriter.println("\t\t\t\t\t}");


        if (isList) {
            printWriter.println("\t\t\t\t\twhile (rs.next()) {");
        } else {
            printWriter.println("\t\t\t\t\tif (rs.next()) {");
        }

        //创建结果对象
        printWriter.println("\t\t\t\t\t\t" + returnType.toString() + " result = new " + returnType.toString() + "();");
        for (Element field : fields) {
            String fieldName = field.getSimpleName().toString();
            String method = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            String rsMethod = "";
            switch (field.asType().toString()) {
                case "boolean":
                    rsMethod = "getBoolean";
                    break;
                case "byte":
                    rsMethod = "getByte";
                    break;
                case "short":
                    rsMethod = "getShort";
                    break;
                case "int":
                    rsMethod = "getInt";
                    break;
                case "long":
                    rsMethod = "getLong";
                    break;
                case "float":
                    rsMethod = "getFloat";
                    break;
                case "double":
                    rsMethod = "getDouble";
                    break;
                case "java.lang.String":
                    rsMethod = "getString";
                    break;
                case "java.util.Date":
                    rsMethod = "getDate";
                    break;
            }
            printWriter.println("\t\t\t\t\t\tif (" + fieldName + "Index != -1) {");
            printWriter.println("\t\t\t\t\t\t\tresult." + method + "(rs." + rsMethod + "(" + field.getSimpleName() + "Index));");
            printWriter.println("\t\t\t\t\t\t}");
        }


        if (isList) {
            printWriter.println("\t\t\t\t\t\tlist.add(result);");
        } else {
            printWriter.println("\t\t\t\t\t\treturn result;");
        }
        printWriter.println("\t\t\t\t\t}");

        if (isList) {
            printWriter.println("\t\t\t\t\treturn list;");
        } else {
            printWriter.println("\t\t\t\t\treturn null;");
        }
    }

    /***
     * 解析SQL中的条件自动
     * @param sql
     * @return
     */
    private List<String> parseSqlParam(String sql) {
        return Collections.emptyList();
    }


}
