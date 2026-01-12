/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import tech.smartboot.feat.cloud.aot.controller.JsonSerializer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;

/**
 * @author 三刀
 * @version v1.0 7/23/25
 */
public final class MapperSerializer extends AbstractSerializer {
    public MapperSerializer(ProcessingEnvironment processingEnv, String config, Element element) throws IOException {
        super(processingEnv, config, element);
    }

    @Override
    public void serializeImport() {
        printWriter.println("import " + SqlSessionFactory.class.getName() + ";");
        printWriter.println("import " + SqlSession.class.getName() + ";");
        super.serializeImport();
    }

    @Override
    public void serializeProperty() {
        printWriter.println("\tprivate SqlSessionFactory factory;");
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
            printWriter.append(JsonSerializer.headBlank(1)).println("try (SqlSession " + sessionName + " = factory.openSession(true)) {");
            printWriter.print(JsonSerializer.headBlank(2));
            if (!"void".equals(returnType)) {
                printWriter.print("return ");
            }
            printWriter.print(sessionName + ".getMapper(" + element.getSimpleName() + ".class)." + se.getSimpleName() + "(");
            first = true;
            for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                if (first) {
                    first = false;
                } else {
                    printWriter.print(", ");
                }
                printWriter.print(param.getSimpleName().toString());
            }
            printWriter.println(");");
            printWriter.append(JsonSerializer.headBlank(1)).println("}");
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
        printWriter.println("\t\tfactory = applicationContext.getBean(\"sessionFactory\");");
    }
}
