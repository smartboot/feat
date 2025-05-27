/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.serializer;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import org.yaml.snakeyaml.Yaml;
import tech.smartboot.feat.cloud.AbstractServiceLoader;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.cloud.annotation.Value;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.router.Router;

import javax.annotation.processing.ProcessingEnvironment;
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
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 三刀
 * @version v1.0 5/27/25
 */
public class FeatYamlValueSerializer {
    private final String config;
    private boolean exception = false;
    private ProcessingEnvironment processingEnv;
    private final Set<String> availableTypes = new HashSet<>(Arrays.asList(String.class.getName(), int.class.getName()));

    public FeatYamlValueSerializer(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        FileObject featYaml = null;
        for (String filename : Arrays.asList("feat.yml", "feat.yaml")) {
            featYaml = loadFeatYaml(filename);
            if (featYaml != null) {
                break;
            }
        }

        if (featYaml != null && new File(featYaml.toUri()).exists()) {
            try {
                Yaml yaml = new Yaml();
                config = JSONObject.from(yaml.load(featYaml.openInputStream())).toJSONString();
            } catch (Throwable e) {
                throw new FeatException(e);
            }
        } else {
            config = "{}";
        }
        try {
            createServerOptionsBean();
        } catch (Throwable e) {
            System.err.println("FeatYamlValueSerializer create server options bean failed");
            exception = true;
        }

    }

    private void createServerOptionsBean() throws Throwable {
        String loaderName = "FeatCloudOptionsBeanAptLoader";
        JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(loaderName);
        Writer writer = javaFileObject.openWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println("package tech.smartboot.feat.sandao;");
        printWriter.println();
        printWriter.println("import " + AbstractServiceLoader.class.getName() + ";");
        printWriter.println("import " + ApplicationContext.class.getName() + ";");
        printWriter.println("import " + Router.class.getName() + ";");
        printWriter.println();
        printWriter.println("public class " + loaderName + " extends " + AbstractServiceLoader.class.getSimpleName() + " {");
        printWriter.println();


        printWriter.println("\tpublic void loadBean(ApplicationContext applicationContext) throws Throwable {");
        for (Field field : ServerOptions.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (type.isArray()) {
                type = type.getComponentType();
            }
            if (!availableTypes.contains(type.getName())) {
                continue;
            }
            Object obj = JSONPath.eval(config, "$.server." + field.getName());
            if (obj == null) {
                continue;
            }
            printWriter.println("\t\tapplicationContext.getOptions()." + field.getName() + "(" + obj + ");");
        }
        printWriter.println("\t}");

        printWriter.println();
        printWriter.println("\tpublic void autowired(ApplicationContext applicationContext) throws Throwable {");

        printWriter.println("\t}");
        printWriter.println();

        printWriter.println("\tpublic void router(" + Router.class.getSimpleName() + " router) {");

        printWriter.println("\t}");
        printWriter.println();

        printWriter.println();
        printWriter.println("\tpublic void destroy() throws Throwable {");

        printWriter.println("\t}");
        printWriter.println();
        printWriter.println("\tpublic void postConstruct(ApplicationContext applicationContext) throws Throwable {");

        printWriter.println("\t}");
        printWriter.println("}");
        writer.close();
    }

    public String getServiceName() {
        return "tech.smartboot.feat.sandao.FeatCloudOptionsBeanAptLoader";
    }

    private FileObject loadFeatYaml(String filename) {
        try {
            return processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", filename);
        } catch (IOException ignored) {
        }
        return null;
    }

    public String generateValueSetter(Element element) {
        StringBuilder stringBuilder = new StringBuilder();
        element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Value.class) != null).forEach(field -> {
            Value value = field.getAnnotation(Value.class);
            String paramName = value.value();
            String defaultValue = null;
            if (StringUtils.isBlank(paramName)) {
                paramName = field.getSimpleName().toString();
            } else if (StringUtils.startsWith(paramName, "${") && StringUtils.endsWith(paramName, "}")) {
                paramName = paramName.substring(2, paramName.length() - 1);
                int index = paramName.indexOf(":");
                if (index != -1) {
                    defaultValue = paramName.substring(index + 1);
                    paramName = paramName.substring(0, index);
                }
            } else {

                throw new FeatException("the value of Value on " + element.getSimpleName() + "@" + field.getSimpleName() + " is not allowed to be empty.");
            }


            String name = field.getSimpleName().toString();
            String fieldType = field.asType().toString();
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
                if (!param.asType().toString().equals(fieldType)) {
                    continue;
                }
                hasSetter = true;
            }
            Object paramValue = JSONPath.eval(config, "$." + paramName);
            if (defaultValue == null && paramValue == null) {
                return;
            }
            String stringValue = null;
            if (hasSetter) {
                if (String.class.getName().equals(fieldType)) {
                    stringValue = paramValue == null ? defaultValue : paramValue.toString();
                    stringValue = "\"" + stringValue.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"") + "\"";
                } else if (int.class.getName().equals(fieldType)) {
                    try {
                        stringValue = paramValue == null ? defaultValue : paramValue.toString();
                        stringValue = String.valueOf(Integer.parseInt(stringValue));
                    } catch (NumberFormatException e) {
                        System.err.println("compiler err: invalid value [ " + stringValue + " ] for field[ " + field.getSimpleName() + " ] in class[ " + element + " ]");
                        exception = true;
                    }
                } else if ("int[]".equals(fieldType)) {
                    System.out.println(paramValue);
                    JSONArray array = (JSONArray) paramValue;
                    stringValue = "new int[]{";
                    for (int i = 0; i < array.size(); i++) {
                        if (i != 0) {
                            stringValue += ", ";
                        }
                        Object o = array.get(i);
                        if (!(o instanceof Integer)) {
                            System.err.println("compiler err: invalid value [ " + o + " ] for field[ " + field.getSimpleName() + " ] in class[ " + element + " ]");
                            exception = true;
                            break;
                        }
                        stringValue += o.toString();
                    }
                    stringValue += "}";
                } else if ("java.util.List<java.lang.Integer>".equals(fieldType)) {
                    JSONArray array = (JSONArray) paramValue;
                    stringValue = "java.util.Arrays.asList(";
                    for (int i = 0; i < array.size(); i++) {
                        if (i != 0) {
                            stringValue += ", ";
                        }
                        Object o = array.get(i);
                        if (!(o instanceof Integer)) {
                            System.err.println("compiler err: invalid value [ " + o + " ] for field[ " + field.getSimpleName() + " ] in class[ " + element + " ]");
                            exception = true;
                            break;
                        }
                        stringValue += o.toString();
                    }
                    stringValue += ")";
                } else if ("java.lang.String[]".equals(fieldType)) {
                    JSONArray array = (JSONArray) paramValue;
                    stringValue = "new String[]{";
                    for (int i = 0; i < array.size(); i++) {
                        if (i != 0) {
                            stringValue += ", ";
                        }
                        Object o = array.get(i);
                        if (!(o instanceof String)) {
                            System.err.println("compiler err: invalid value [ " + o + " ] for field[ " + field.getSimpleName() + " ] in class[ " + element + " ]");
                            exception = true;
                            break;
                        }
                        stringValue += toString(o.toString());
                    }
                    stringValue += "}";
                } else if ("java.util.List<java.lang.String>".equals(fieldType)) {
                    JSONArray array = (JSONArray) paramValue;
                    stringValue = "java.util.Arrays.asList(";
                    for (int i = 0; i < array.size(); i++) {
                        if (i != 0) {
                            stringValue += ", ";
                        }
                        Object o = array.get(i);
                        if (!(o instanceof String)) {
                            System.err.println("compiler err: invalid value [ " + o + " ] for field[ " + field.getSimpleName() + " ] in class[ " + element + " ]");
                            exception = true;
                            break;
                        }
                        stringValue += toString(o.toString());
                    }
                    stringValue += ")";
                } else {
                    System.err.println("compiler err: unsupported type [ " + fieldType + " ] for field[ " + field.getSimpleName() + " ] in class[ " + element + " ]");
                    exception = true;
                }
                if (exception) {
                    return;
                }
                stringBuilder.append("\t\tbean.set").append(name).append("(").append(stringValue).append(");\n");
            } else {
                System.err.println("compiler err: no setter method found for field[ " + field.getSimpleName() + " ] in class[ " + element + " ]");
                exception = true;
//                stringBuilder.append("\t\treflectAutowired(bean, \"").append(field.getSimpleName().toString()).append("\", applicationContext);\n");
            }
        });
        return stringBuilder.toString();
    }

    private String toString(String str) {
        return "\"" + str.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"") + "\"";
    }

    public boolean isException() {
        return exception;
    }
}
