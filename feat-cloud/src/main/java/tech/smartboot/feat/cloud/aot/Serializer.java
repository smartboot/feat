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
import tech.smartboot.feat.cloud.AbstractServiceLoader;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.aot.value.FeatYamlValueSerializer;
import tech.smartboot.feat.router.Router;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version v1.0 7/23/25
 */
abstract class Serializer {
    protected FeatYamlValueSerializer yamlValueSerializer;
    protected PrintWriter printWriter;
    protected final Element element;

    public Serializer(FeatYamlValueSerializer yamlValueSerializer, Element element) {
        this.yamlValueSerializer = yamlValueSerializer;
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    public void setPrintWriter(PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    void serializeImport() {
        printWriter.println("import " + AbstractServiceLoader.class.getName() + ";");
        printWriter.println("import " + ApplicationContext.class.getName() + ";");
        printWriter.println("import " + Router.class.getName() + ";");
        printWriter.println("import " + JSONObject.class.getName() + ";");
        printWriter.println("import com.alibaba.fastjson2.JSON;");
    }

    void serializeProperty() {
        printWriter.println("\tprivate " + element.getSimpleName() + " bean;");
    }

    void serializeLoadBean() {
        printWriter.println("\t\tbean = new " + element.getSimpleName() + "(); ");
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

    void serializeAutowired() {
        List<Element> autowiredFields = new ArrayList<>();
        for (Element field : element.getEnclosedElements()) {
            if (field.getAnnotation(Autowired.class) != null) {
                autowiredFields.add(field);
            }
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
                printWriter.append("\t\treflectAutowired(bean, \"").append(field.getSimpleName().toString()).append("\", applicationContext);\n");
            }
        }
        printWriter.append(yamlValueSerializer.generateValueSetter(element));
    }

    void serializeRouter() throws IOException {
    }

    void serializeBytePool() {

    }

    final void serializePostConstruct() {
        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (PostConstruct.class.getName().equals(mirror.getAnnotationType().toString())) {
                    printWriter.println("\t\tbean." + se.getSimpleName() + "();");
                }
            }
        }
    }

    final void serializeDestroy() {
        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (PreDestroy.class.getName().equals(mirror.getAnnotationType().toString())) {
                    printWriter.println("\t\tbean." + se.getSimpleName() + "();");
                }
            }
        }
    }
}
