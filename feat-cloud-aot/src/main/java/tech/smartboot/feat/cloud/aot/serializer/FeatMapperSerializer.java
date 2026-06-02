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

import tech.smartboot.feat.cloud.annotation.orm.Select;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;

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
//        int i = 0;
//        for (Element se : element.getEnclosedElements()) {
//            printWriter.print("\t\t\t" + Method.class.getName() + " " + se.getSimpleName() + (i++) + " =" + element.getSimpleName() + ".class.getDeclaredMethod(\"" + se.getSimpleName() + "\"");
//            for (VariableElement param : ((ExecutableElement) se).getParameters()) {
//                printWriter.print(",");
//                if (param.asType().toString().startsWith("java.util.List")) {
//                    printWriter.print("java.util.List.class");
//                } else {
//                    printWriter.print(param.asType().toString() + ".class");
//                }
//
//            }
//            printWriter.append(");");
//        }
//        i = 0;
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
//            printWriter.append(headBlank(1)).println("try (SqlSession " + sessionName + " = factory.openSession(true)) {");
//            printWriter.print(headBlank(2));
//            if (!"void".equals(returnType)) {
//                printWriter.print("return ");
//            }
////            printWriter.print("(" + returnType + ")new " + MapperMethod.class.getName() + "(" + element.getSimpleName() + ".class," + se.getSimpleName() + (i++) + "," + sessionName + ".getConfiguration()).execute(" + sessionName + ",new Object[]{");
//            printWriter.print(sessionName + ".getMapper(" + element.getSimpleName() + ".class)." + se.getSimpleName() + "(");
//            first = true;
//            for (VariableElement param : ((ExecutableElement) se).getParameters()) {
//                if (first) {
//                    first = false;
//                } else {
//                    printWriter.print(", ");
//                }
//                printWriter.print(param.getSimpleName().toString());
//            }
//            printWriter.println(");");
////            printWriter.println("});");
//            printWriter.append(headBlank(1)).println("}");
            printWriter.println("\t\t\t}");
            printWriter.println();
        }
        printWriter.println("\t\t};");
        String beanName = element.getSimpleName().toString().substring(0, 1).toLowerCase() + element.getSimpleName().toString().substring(1);
        printWriter.println("\t\tapplicationContext.addBean(\"" + beanName + "\", bean);");
    }

    @Override
    public void serializeAutowired() {
        super.serializeAutowired();
//        printWriter.println("\t\tfactory = applicationContext.getBean(\"sessionFactory\");");
//        //addMapper
//        if (JSONPath.eval(config, "$.feat.mybatis.path") != null) {
//            printWriter.println("\t\tfactory.getConfiguration().addMapper(" + element.getSimpleName() + ".class);");
//        }
    }

    private void serialSelect(PrintWriter printWriter, Element se, Select select) {
        String sql = select.value();

    }
}
