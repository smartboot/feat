 /*
  *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
  *
  *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
  *
  *   Enterprise users are required to use this project reasonably
  *  and legally in accordance with the Apache-2.0 open source agreement
  *  without special permission from the smartboot organization.
  */

 package tech.smartboot.feat.cloud.aot.serializer;


import javax.annotation.processing.ProcessingEnvironment;
 import javax.lang.model.element.Element;
 import javax.lang.model.element.ElementKind;
 import javax.lang.model.element.ExecutableElement;
 import javax.lang.model.element.TypeElement;
 import javax.sql.DataSource;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.Statement;

 /**
  * Feat 原生 Mapper 序列化器。
  *
  * 将 {@link tech.smartboot.feat.cloud.annotation.orm.Mapper} 接口中的
  * {@link tech.smartboot.feat.cloud.annotation.orm.Select}/
  * {@link tech.smartboot.feat.cloud.annotation.orm.Insert}/
  * {@link tech.smartboot.feat.cloud.annotation.orm.Update}/
  * {@link tech.smartboot.feat.cloud.annotation.orm.Delete} 方法通过 APT
  * 转换为纯 JDBC 实现，使用方式与 MyBatis 注解保持一致。
  *
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
        printWriter.println("import " + Statement.class.getName() + ";");
       super.serializeImport();
    }

    @Override
    public void serializeProperty() {
        printWriter.println("\tprivate DataSource dataSource;");
        super.serializeProperty();
    }

    @Override
    public void serializeAutowired() {
        super.serializeAutowired();
        printWriter.println("\t\tdataSource = applicationContext.getBean(\"dataSource\");");
    }

    @Override
    public void serializeLoadBean() {
        printWriter.println("\t\tbean = new " + element.getSimpleName() + "() { ");
           for (Element se : element.getEnclosedElements()) {
               if (se.getKind() != ElementKind.METHOD) {
                   continue;
               }
                FeatMapperMethodGenerator.generate(printWriter, (ExecutableElement) se, (TypeElement) element, processingEnv);
           }
        printWriter.println("\t\t};");
        String beanName = element.getSimpleName().toString().substring(0, 1).toLowerCase() + element.getSimpleName().toString().substring(1);
        printWriter.println("\t\tapplicationContext.addBean(\"" + beanName + "\", bean);");
    }

 }
