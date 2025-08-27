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
import org.apache.ibatis.annotations.Mapper;
import org.yaml.snakeyaml.Yaml;
import tech.smartboot.feat.cloud.AbstractCloudService;
import tech.smartboot.feat.cloud.CloudService;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
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
import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final List<BeanUnit> services = new ArrayList<>();
    private String config;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            this.config = loadFeatYaml(processingEnv);
            serviceFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + CloudService.class.getName());
            serviceWrite = new PrintWriter(serviceFile.openWriter());
            System.out.println("processor init: " + this);
        } catch (Throwable e) {
            throw new FeatException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        System.out.println("annotation process: " + this + " ,annotations: " + annotations);


        for (Element element : roundEnv.getElementsAnnotatedWith(Bean.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                try {
                    createAptLoader(new BeanSerializer(processingEnv, config, element));
                } catch (Throwable e) {
                    exception = e;
                }
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(McpEndpoint.class)) {
            if (element.getAnnotation(Controller.class) == null) {
                throw new FeatException("@McpEndpoint can only be used with @Controller!");
            }
        }

        boolean rootMcpEnable = false;
        for (Element element : roundEnv.getElementsAnnotatedWith(Controller.class)) {
            try {
//                if (element.getAnnotation(McpEndpoint.class) != null) {
//                    throw new FeatException("@Controller and @McpEndpoint cannot be used together!");
//                }
                ControllerSerializer serializer = new ControllerSerializer(processingEnv, config, element);
                if (serializer.rootMcpEnable()) {
                    rootMcpEnable = true;
                }
                createAptLoader(serializer);
            } catch (Throwable e) {
                exception = e;
            }
        }
        //
        if (rootMcpEnable) {
            try {
                createAptLoader(new DefaultMcpServerSerializer(processingEnv, config));
            } catch (Throwable e) {
                exception = e;
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Mapper.class)) {
            try {
                createAptLoader(new MapperSerializer(processingEnv, config, element));
            } catch (Throwable e) {
                exception = e;
            }
        }

        try {
            services.sort(Comparator.comparingInt(o -> o.order));
            List<String> list = new ArrayList<>(services.size());
            for (BeanUnit beanUnit : services) {
                list.add(beanUnit.name);
            }
            services.clear();
            createAptLoader(new CloudOptionsSerializer(processingEnv, config, list));
        } catch (Throwable e) {
            exception = e;
        }
        for (BeanUnit service : services) {
            serviceWrite.println(service.name);
        }
        serviceWrite.flush();

        if (exception != null) {
            exception.printStackTrace();
            throw new FeatException("编译失败！请根据提示修复错误，或者联系开发者：https://gitee.com/smartboot/feat/issues");
        }
        // 如果不希望后续的处理器继续处理这些注解，返回 true，否则返回 false
        return false;
    }

    private <T extends Annotation> void createAptLoader(Serializer serializer) throws IOException {
        //生成service配置
        services.add(new BeanUnit(serializer.packageName() + "." + serializer.className(), serializer.order()));

        PrintWriter printWriter = serializer.getPrintWriter();
        printWriter.println("package " + serializer.packageName() + ";");
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
    }

    private String loadFeatYaml(ProcessingEnvironment processingEnv) throws IOException {
        FileObject featYaml = null;
        for (String filename : Arrays.asList("feat.yml", "feat.yaml")) {
            try {
                featYaml = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", filename);
            } catch (IOException ignored) {
            }
            if (featYaml != null) {
                break;
            }
        }

        if (featYaml == null) {
            return "{}";
        }

        File featFile = new File(featYaml.toUri());
        if (!featFile.exists()) {
            return "{}";
        }

        Yaml yaml = new Yaml();
        return JSONObject.from(yaml.load(featYaml.openInputStream())).toJSONString();
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
