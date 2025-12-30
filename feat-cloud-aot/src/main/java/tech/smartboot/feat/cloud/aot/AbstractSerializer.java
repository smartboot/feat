/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.cloud.AbstractCloudService;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.Value;
import tech.smartboot.feat.cloud.aot.value.FeatValueSerializer;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.router.Router;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 7/23/25
 */
abstract class AbstractSerializer implements Serializer {
    protected final String config;
    protected final PrintWriter printWriter;
    protected final Element element;
    private final String packageName;
    private final String className;
    protected ProcessingEnvironment processingEnv;


    public AbstractSerializer(ProcessingEnvironment processingEnv, String config, Element element) throws IOException {
        this.config = config;
        this.element = element;
        this.packageName = element.getEnclosingElement().asType().toString();
        if (FeatUtils.isBlank(packageName)) {
            throw new FeatException("Compilation for class " + element.getSimpleName() + " with an empty package is not supported. Please declare a valid package (e.g., 'com.example.service') for the class.");
        }
        this.className = element.getSimpleName() + "CloudService";

        this.processingEnv = processingEnv;

        FileObject preFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, packageName, className + ".java");
        File f = new File(preFileObject.toUri());
        if (f.exists()) {
            f.delete();
        }

        JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(packageName + "." + className);
        Writer writer = javaFileObject.openWriter();
        printWriter = new PrintWriter(writer);
    }


    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    public void serializeImport() {
        printWriter.println("import " + AbstractCloudService.class.getName() + ";");
        printWriter.println("import " + ApplicationContext.class.getName() + ";");
        printWriter.println("import " + Router.class.getName() + ";");
        printWriter.println("import " + JSONObject.class.getName() + ";");
        printWriter.println("import com.alibaba.fastjson2.JSON;");
    }

    public void serializeProperty() {
        printWriter.println("\tprivate " + element.getSimpleName() + " bean;");
    }

    public void serializeLoadBean() {
        printWriter.println("\t\tbean = new " + element.getSimpleName() + "(); ");
        serializerValueSetter();
    }

    @Override
    public void serializeLoadMethodBean() {
        //初始化通过方法实例化的bean
        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (!Bean.class.getName().equals(mirror.getAnnotationType().toString())) {
                    continue;
                }
                printWriter.print("\t\tapplicationContext.addBean(\"" + se.getSimpleName() + "\", bean." + se.getSimpleName() + "(");
                boolean first = true;
                for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                    if (first) {
                        first = false;
                    } else {
                        printWriter.print(", ");
                    }
                    printWriter.print("loadBean(\"" + param.getSimpleName() + "\", applicationContext)");
                }
                printWriter.println("));");
            }
        }
    }

    public void serializeAutowired() {
        List<Element> autowiredFields = new ArrayList<>();
        for (Element field : element.getEnclosedElements()) {
            if (field.getAnnotation(Autowired.class) == null) {
                continue;
            }
            //McpServer特殊处理
            if (field.asType().toString().equals(McpServer.class.getName())) {
                continue;
            }
            autowiredFields.add(field);
        }
        for (Element field : autowiredFields) {
            String name = field.getSimpleName().toString();
            name = name.substring(0, 1).toUpperCase() + name.substring(1);

            //判断是否存在setter方法
            boolean hasSetter = false;
            for (Element se : element.getEnclosedElements()) {
                if (!("set" + name).equals(se.getSimpleName().toString())) {
                    continue;
                }
                List<? extends VariableElement> list = ((ExecutableElement) se).getParameters();
                if (list.size() != 1) {
                    continue;
                }
                VariableElement param = list.get(0);
                if (!param.asType().toString().equals(field.asType().toString())) {
                    continue;
                }
                hasSetter = true;
            }
            if (hasSetter) {
                printWriter.append("\t\tbean.set").append(name).append("(loadBean(\"").append(field.getSimpleName()).append("\", applicationContext));\n");
            } else {
                printWriter.append("\t\treflectAutowired(bean, \"").append(String.valueOf(field.getSimpleName())).append("\", loadBean(\"").append(field.getSimpleName().toString()).append("\", applicationContext));\n");
            }
        }
    }

    public void serializerValueSetter() {
        element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Value.class) != null).forEach(field -> {

            Value value = field.getAnnotation(Value.class);
            String paramName = value.value();
            String defaultValue = null;
            if (FeatUtils.isBlank(paramName)) {
                paramName = "$." + field.getSimpleName();
            } else if (FeatUtils.startsWith(paramName, "${") && FeatUtils.endsWith(paramName, "}")) {
                paramName = paramName.substring(2, paramName.length() - 1);
                int index = paramName.indexOf(":");
                if (index != -1) {
                    defaultValue = paramName.substring(index + 1);
                    paramName = paramName.substring(0, index);
                }
                StringBuilder sb = new StringBuilder("$");
                for (String s : paramName.split("\\.")) {
                    sb.append("['").append(s).append("']");
                }
                paramName = sb.toString();
            } else {
                throw new FeatException("the value of Value on " + field.getEnclosingElement().getSimpleName() + "@" + field.getSimpleName() + " is not allowed to be empty.");
            }
            Object paramValue = JSONPath.eval(config, paramName);
            if (defaultValue == null && paramValue == null) {
                return;
            }
            paramValue = paramValue == null ? defaultValue : paramValue;

            String name = field.getSimpleName().toString();

            //判断是否存在setter方法
            boolean hasSetter = false;
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            for (Element se : field.getEnclosingElement().getEnclosedElements()) {
                if (!("set" + name).equals(se.getSimpleName().toString())) {
                    continue;
                }
                List<? extends VariableElement> list = ((ExecutableElement) se).getParameters();
                if (list.size() != 1) {
                    continue;
                }
                VariableElement param = list.get(0);
                if (!param.asType().toString().equals(field.asType().toString())) {
                    continue;
                }
                hasSetter = true;
            }
            if (!hasSetter) {
                System.err.println("compiler err: no setter method for field[ " + field.getSimpleName() + " ] in class[ " + field.getEnclosingElement() + " ]");
                throw new FeatException("compiler err: no setter method for field[ " + field.getSimpleName() + " ] in class[ " + field.getEnclosingElement() + " ]");
            }
            printWriter.append("\t\tbean.set").append(name).append("(").append(FeatValueSerializer.serialize(field, paramValue)).append(");\n");
        });
    }

    public void serializePostConstruct() {
        for (Element se : element.getEnclosedElements()) {
            if (se.getAnnotation(PostConstruct.class) != null) {
                printWriter.println("\t\tbean." + se.getSimpleName() + "();");
            }
        }
    }

    public void serializeDestroy() {
        for (Element se : element.getEnclosedElements()) {
            if (se.getAnnotation(PreDestroy.class) != null) {
                printWriter.println("\t\tbean." + se.getSimpleName() + "();");
            }
        }
    }

    public String packageName() {
        return packageName;
    }

    public String className() {
        return className;
    }

    protected String getFeatYamlValue(String name) {
        Object val = JSONPath.eval(config, name);
        return val == null ? "" : val.toString();
    }
}
