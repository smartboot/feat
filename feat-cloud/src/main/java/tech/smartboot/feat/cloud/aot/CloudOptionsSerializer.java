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

import com.alibaba.fastjson2.JSONPath;
import tech.smartboot.feat.cloud.AbstractServiceLoader;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.router.Router;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 5/27/25
 */
final class CloudOptionsSerializer implements Serializer {
    private static final Logger logger = LoggerFactory.getLogger(CloudOptionsSerializer.class);
    private static final String PACKAGE = "tech.smartboot.feat.sandao";
    private static final String CLASS_NAME = "FeatCloudOptionsBeanAptLoader";
    private final String config;
    private final Set<String> availableTypes = new HashSet<>(Arrays.asList(String.class.getName(), int.class.getName()));

    private final PrintWriter printWriter;
    private final List<String> services;

    public CloudOptionsSerializer(ProcessingEnvironment processingEnv, String config, List<String> services) throws Throwable {
        this.config = config;
        this.services = services;

        FileObject preFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, packageName(), className() + ".java");
        File f = new File(preFileObject.toUri());
        if (f.exists()) {
            f.delete();
        }

        JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(packageName() + "." + className());
        Writer writer = javaFileObject.openWriter();
        printWriter = new PrintWriter(writer);
    }

    @Override
    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    @Override
    public String packageName() {
        return PACKAGE;
    }

    @Override
    public String className() {
        return CLASS_NAME;
    }

    public void serializeImport() {
        printWriter.println("import " + AbstractServiceLoader.class.getName() + ";");
        printWriter.println("import " + ApplicationContext.class.getName() + ";");
        printWriter.println("import " + Router.class.getName() + ";");
        for (String service : services) {
            printWriter.println("import " + service + ";");
        }
    }

    @Override
    public void serializeProperty() {
        List<String> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String date = sdf.format(new Date());
        int i = 0;
        for (String service : services) {
            String simpleClass = service.substring(service.lastIndexOf(".") + 1);
            String propertyName = "feat_" + date + "_" + i++;
            list.add(propertyName);
            printWriter.println("\tprivate " + simpleClass + " " + propertyName + " = new " + simpleClass + "();");
        }
        services.clear();
        services.addAll(list);
    }

    @Override
    public void serializeLoadBean() {
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
        for (String service : services) {
            printWriter.append("\t\t").append(service).println(".loadBean(applicationContext);");
        }
    }

    @Override
    public void serializeAutowired() {
        for (String service : services) {
            printWriter.append("\t\t").append(service).println(".autowired(applicationContext);");
        }
    }

    @Override
    public void serializeRouter() throws IOException {
        for (String service : services) {
            printWriter.append("\t\t").append(service).println(".router(applicationContext, router);");
        }
    }

    @Override
    public void serializePostConstruct() {
        for (String service : services) {
            printWriter.append("\t\t").append(service).println(".postConstruct(applicationContext);");
        }
    }

    @Override
    public void serializeDestroy() {
        for (String service : services) {
            printWriter.append("\t\t").append(service).println(".destroy();");
        }
    }
}
