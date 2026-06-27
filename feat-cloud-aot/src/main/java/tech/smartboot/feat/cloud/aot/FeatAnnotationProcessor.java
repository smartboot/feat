/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot;

import com.alibaba.fastjson2.JSONObject;
import org.apache.ibatis.annotations.Mapper;
import org.yaml.snakeyaml.Yaml;
import tech.smartboot.feat.cloud.AbstractCloudService;
import tech.smartboot.feat.cloud.CloudService;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.cloud.aot.license.LicenseLoader;
import tech.smartboot.feat.cloud.aot.serializer.BeanSerializer;
import tech.smartboot.feat.cloud.aot.serializer.CloudOptionsSerializer;
import tech.smartboot.feat.cloud.aot.serializer.ControllerSerializer;
import tech.smartboot.feat.cloud.aot.serializer.DefaultMcpServerSerializer;
import tech.smartboot.feat.cloud.aot.serializer.MapperSerializer;
import tech.smartboot.feat.cloud.aot.serializer.openapi.ApiDocSerializer;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.router.Router;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 该注解表示该处理器支持的 Java 源代码版本

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class FeatAnnotationProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(Bean.class.getCanonicalName());
        types.add(Autowired.class.getCanonicalName());
        types.add(Controller.class.getCanonicalName());
        types.add(Mapper.class.getCanonicalName());
        types.add(McpEndpoint.class.getCanonicalName());
        return types;
    }

    FileObject serviceFile;
    PrintWriter serviceWrite;
    private Throwable exception = null;
    private final Map<String, String> configs = new LinkedHashMap<>();
    private ApiDocSerializer apiDocSerializer;
    private boolean generated;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            this.configs.putAll(loadFeatConfigs(processingEnv));
            //清理原service文件。
            serviceFile = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + CloudService.class.getName());
            File file = new File(serviceFile.toUri());
            if (file.isFile()) {
                System.out.println("delete service file: " + serviceFile.toUri() + " " + (file.delete() ? "success" : "fail"));
            }
            serviceFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + CloudService.class.getName());
            serviceWrite = new PrintWriter(serviceFile.openWriter());
            // 初始化 API 文档生成器
            apiDocSerializer = new ApiDocSerializer(processingEnv);
            System.out.println("processor init: " + this);
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (generated || annotations.isEmpty()) {
            return false;
        }
        System.out.println("annotation process: " + this + " ,annotations: " + annotations);

        for (Element element : roundEnv.getElementsAnnotatedWith(McpEndpoint.class)) {
            if (element.getAnnotation(Controller.class) == null) {
                throw new FeatException("@McpEndpoint can only be used with @Controller!");
            }
        }

        try {
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                processProfile(roundEnv, entry.getKey(), entry.getValue());
            }
            serviceWrite.flush();
            generated = true;
        } catch (Throwable e) {
            exception = e;
        } finally {
            serviceWrite.close();
        }

        // 生成 OpenAPI 文档
//        try {
//            apiDocSerializer.generateOpenApiDoc(licenseLoader.getLicense());
//        } catch (Throwable e) {
//            exception = e;
//        }


        if (exception != null) {
            exception.printStackTrace();
            throw new FeatException("编译失败！请根据提示修复错误，或者联系开发者：https://gitee.com/smartboot/feat/issues");
        }
        // 如果不希望后续的处理器继续处理这些注解，返回 true，否则返回 false
        return false;
    }

    private void processProfile(RoundEnvironment roundEnv, String env, String config) throws Throwable {
        String suffix = environmentSuffix(env);
        List<BeanUnit> services = new java.util.ArrayList<>();
        LicenseLoader licenseLoader = new LicenseLoader(config);

        for (Element element : roundEnv.getElementsAnnotatedWith(Bean.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                try {
                    BeanSerializer serializer = new BeanSerializer(processingEnv, config, element, suffix);
                    services.add(new BeanUnit(createAptLoader(serializer), serializer.order()));
                } catch (Throwable e) {
                    exception = e;
                }
            }
        }

        boolean rootMcpEnable = false;
        for (Element element : roundEnv.getElementsAnnotatedWith(Controller.class)) {
            try {
//                if (element.getAnnotation(McpEndpoint.class) != null) {
//                    throw new FeatException("@Controller and @McpEndpoint cannot be used together!");
//                }
                ControllerSerializer serializer = new ControllerSerializer(processingEnv, config, element, suffix);
                if (serializer.rootMcpEnable()) {
                    rootMcpEnable = true;
                }
                services.add(new BeanUnit(createAptLoader(serializer), serializer.order()));
                // 收集 API 文档信息
                if (FeatUtils.isBlank(env)) {
                    apiDocSerializer.addController(element);
                }
            } catch (Throwable e) {
                exception = e;
            }
        }
        //
        if (rootMcpEnable) {
            try {
                services.add(new BeanUnit(createAptLoader(new DefaultMcpServerSerializer(processingEnv, suffix)), 0));
            } catch (Throwable e) {
                exception = e;
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Mapper.class)) {
            try {
                MapperSerializer serializer = new MapperSerializer(processingEnv, config, element, suffix);
                services.add(new BeanUnit(createAptLoader(serializer), serializer.order()));
            } catch (Throwable e) {
                exception = e;
            }
        }

        services.sort(Comparator.comparingInt(o -> o.order));
        List<String> list = services.stream().map(beanUnit -> beanUnit.name).collect(Collectors.toList());
        String cloudApplicationClass = createAptLoader(new CloudOptionsSerializer(processingEnv, config, list, licenseLoader.getLicense(), env, suffix, configs.size() > 1));
        serviceWrite.println(cloudApplicationClass);
    }

    private String createAptLoader(Serializer serializer) throws IOException {
        String fullClassName = serializer.packageName() + "." + serializer.className();
        PrintWriter printWriter = serializer.getPrintWriter();
        if (FeatUtils.isNotBlank(serializer.packageName())) {
            printWriter.println("package " + serializer.packageName() + ";");
        }

        printWriter.println();
        serializer.serializeImport();
        printWriter.println();
        //添加版权注释
        printWriter.println("/**");
        printWriter.println(" * Please do not modify the currently generated code, otherwise it will be overwritten!");
        printWriter.println(" * Copyright (c) 2022-" + Year.now().getValue() + " smartboot.tech All Rights Reserved.");
        printWriter.println(" *");
        printWriter.println(" * @Description: " + serializer.className());
        printWriter.println(" * @Author: 三刀 zhengjunweimail@163.com");
        printWriter.println(" * @Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        printWriter.println(" */");
        printWriter.println("public class " + serializer.className() + " extends " + AbstractCloudService.class.getSimpleName() + " {");
        printWriter.println();
        serializer.serializeProperty();
        printWriter.println();

        printWriter.println("\tpublic void loadBean(ApplicationContext applicationContext) throws Throwable {");
        serializer.serializeLoadBean();
        printWriter.println("\t}");

        printWriter.println("\tpublic void loadMethodBean(ApplicationContext applicationContext) throws Throwable {");
        serializer.serializeLoadMethodBean();
        printWriter.println("\t}");

        printWriter.println();
        printWriter.println("\tpublic void autowired(ApplicationContext applicationContext) throws Throwable {");
        serializer.serializeAutowired();
        printWriter.println("\t}");
        printWriter.println();

        printWriter.println("\tpublic void router(ApplicationContext applicationContext, " + Router.class.getSimpleName() + " router) {");
        serializer.serializeRouter();
        printWriter.println("\t}");
        printWriter.println();
        serializer.serializeBytePool();

        printWriter.println();
        printWriter.println("\tpublic void destroy() throws Throwable {");
        serializer.serializeDestroy();
        printWriter.println("\t}");
        printWriter.println();
        printWriter.println("\tpublic void postConstruct(ApplicationContext applicationContext) throws Throwable {");
        serializer.serializePostConstruct();
        printWriter.println("\t}");
        printWriter.println("}");
        printWriter.close();
        return fullClassName;
    }

    private Map<String, String> loadFeatConfigs(ProcessingEnvironment processingEnv) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        JSONObject defaultConfig = loadYamlConfig(processingEnv, Arrays.asList("feat.yml", "feat.yaml"));
        result.put("", defaultConfig.toJSONString());

        FileObject featYaml = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "feat.yml");
        File rootDir = new File(featYaml.toUri()).getParentFile();
        if (rootDir != null && rootDir.isDirectory()) {
            File[] files = rootDir.listFiles(file -> {
                String name = file.getName();
                return file.isFile() && name.matches("feat-[A-Za-z0-9]+\\.ya?ml");
            });
            if (files != null) {
                Arrays.sort(files, Comparator
                        .comparingInt((File file) -> file.getName().endsWith(".yml") ? 0 : 1)
                        .thenComparing(File::getName));
                for (File file : files) {
                    String env = parseEnv(file.getName());
                    if (FeatUtils.isBlank(env) || result.containsKey(env)) {
                        continue;
                    }
                    JSONObject config = new JSONObject();
                    config.putAll(defaultConfig);
                    config.putAll(loadYamlConfig(file));
                    result.put(env, config.toJSONString());
                }
            }
        }
        return result;
    }

    private JSONObject loadYamlConfig(ProcessingEnvironment processingEnv, List<String> filenames) throws IOException {
        FileObject featYaml = null;
        for (String filename : filenames) {
            try {
                featYaml = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", filename);
                if (featYaml != null && new File(featYaml.toUri()).isFile()) {
                    break;
                }
            } catch (IOException ignored) {
            }
            featYaml = null;
        }
        if (featYaml == null) {
            return new JSONObject();
        }

        Yaml yaml = new Yaml();
        try (java.io.InputStream inputStream = featYaml.openInputStream()) {
            Object data = yaml.load(inputStream);
            return data == null ? new JSONObject() : JSONObject.from(data);
        }
    }

    private JSONObject loadYamlConfig(File file) throws IOException {
        Yaml yaml = new Yaml();
        try (java.io.InputStream inputStream = new java.io.FileInputStream(file)) {
            Object data = yaml.load(inputStream);
            return data == null ? new JSONObject() : JSONObject.from(data);
        }
    }

    private String parseEnv(String filename) {
        if ("feat.yml".equals(filename) || "feat.yaml".equals(filename)) {
            return "";
        }
        if (filename.startsWith("feat-") && filename.endsWith(".yml")) {
            return filename.substring(5, filename.length() - 4);
        }
        if (filename.startsWith("feat-") && filename.endsWith(".yaml")) {
            return filename.substring(5, filename.length() - 5);
        }
        throw new FeatException("非法的 feat 配置文件名: " + filename);
    }

    private String environmentSuffix(String env) {
        if (FeatUtils.isBlank(env)) {
            return "";
        }
        return env.substring(0, 1).toUpperCase() + env.substring(1);
    }

    static class BeanUnit {
        String name;
        int order;

        public BeanUnit(String name, int order) {
            this.name = name;
            this.order = order;
        }
    }
}
