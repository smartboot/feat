/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.serializer.value;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import org.yaml.snakeyaml.Yaml;
import tech.smartboot.feat.cloud.AbstractServiceLoader;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.cloud.annotation.Value;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 三刀
 * @version v1.0 5/27/25
 */
public class FeatYamlValueSerializer {
    private static final Logger logger = LoggerFactory.getLogger(FeatYamlValueSerializer.class);
    public static final String SERVICE_NAME = "tech.smartboot.feat.sandao.FeatCloudOptionsBeanAptLoader";
    private String config;
    private boolean exception = false;
    private final ProcessingEnvironment processingEnv;
    private final Set<String> availableTypes = new HashSet<>(Arrays.asList(String.class.getName(), int.class.getName()));
    private final Map<String, AbstractSerializer> serializers = new HashMap<>();

    {
        serializers.put(int.class.getName(), new IntegerSerializer());
        serializers.put(String.class.getName(), new StringValueSerializer());
        serializers.put("int[]", new IntegerArraySerializer());
        serializers.put("java.util.List<java.lang.Integer>", new IntegerListSerializer());
        serializers.put("java.lang.String[]", new StringArraySerializer());
        serializers.put("java.util.List<java.lang.String>", new StringListSerializer());
    }

    public FeatYamlValueSerializer(ProcessingEnvironment processingEnv, List<String> services) {
        this.processingEnv = processingEnv;
        FileObject featYaml = null;
        for (String filename : Arrays.asList("feat.yml", "feat.yaml")) {
            featYaml = loadFeatYaml(filename);
            if (featYaml != null) {
                break;
            }
        }

        if (featYaml == null) {
            config = "{}";
            return;
        }

        File featFile = new File(featYaml.toUri());
        if (!featFile.exists()) {
            config = "{}";
            return;
        }

        try {
            Yaml yaml = new Yaml();
            config = JSONObject.from(yaml.load(featYaml.openInputStream())).toJSONString();
            createServerOptionsBean();
            services.add(SERVICE_NAME);
        } catch (Throwable e) {
            logger.error("FeatYamlValueSerializer create server options bean failed", e);
            exception = true;
        } finally {
            featFile.delete();
        }
    }

    private void createServerOptionsBean() throws Throwable {
        String loaderName = "FeatCloudOptionsBeanAptLoader";

        FileObject preFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "", loaderName + ".java");
        File f = new File(preFileObject.toUri());
        if (f.exists()) {
            f.delete();
        }

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
                throw new FeatException("the value of Value on " + field.getEnclosingElement().getSimpleName() + "@" + field.getSimpleName() + " is not allowed to be empty.");
            }
            Object paramValue = JSONPath.eval(config, "$." + paramName);
            if (defaultValue == null && paramValue == null) {
                return;
            }
            paramValue = paramValue == null ? defaultValue : paramValue;

            String fieldType = field.asType().toString();
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
                exception = true;
                return;
            }

            AbstractSerializer serializer = serializers.get(fieldType);
            if (serializer != null) {
                try {
                    stringBuilder.append("\t\tbean.set").append(name).append("(").append(serializer.serialize(field, paramValue)).append(");\n");
                } catch (Throwable e) {
                    System.err.println(e.getMessage());
                    exception = true;
                }
            } else {
                System.err.println("compiler err: unsupported type [ " + fieldType + " ] for field[ " + field.getSimpleName() + " ] in class[ " + field.getEnclosingElement() + " ]");
                exception = true;
            }
        });
        return stringBuilder.toString();
    }

    public boolean isException() {
        return exception;
    }
}
