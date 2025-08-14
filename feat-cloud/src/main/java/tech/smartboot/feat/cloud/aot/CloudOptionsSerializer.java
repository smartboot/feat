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
import tech.smartboot.feat.cloud.AbstractCloudService;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.cloud.CloudService;
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
    private final String PACKAGE;
    private final String CLASS_NAME;
    private final String config;
    private final Set<String> availableTypes = new HashSet<>(Arrays.asList(String.class.getName(), int.class.getName()));

    private final PrintWriter printWriter;
    private final List<String> services;

    public CloudOptionsSerializer(ProcessingEnvironment processingEnv, String config, List<String> services) throws Throwable {
        this.config = config;
        this.services = services;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String date = sdf.format(new Date());
        PACKAGE = "tech.smartboot.feat.build.v" + date;
        CLASS_NAME = "FeatApplication";

        //清理build目录
        FileObject preFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, packageName(), className() + ".java");
        File buildDir = new File(preFileObject.toUri()).getParentFile().getParentFile();
        deleteBuildDir(buildDir);

        JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(packageName() + "." + className());
        Writer writer = javaFileObject.openWriter();
        printWriter = new PrintWriter(writer);
    }

    private void deleteBuildDir(File dir) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteBuildDir(file);
            }
            file.delete();
        }
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
        printWriter.println("import " + AbstractCloudService.class.getName() + ";");
        printWriter.println("import " + ApplicationContext.class.getName() + ";");
        printWriter.println("import " + Router.class.getName() + ";");
        printWriter.println("import " + CloudService.class.getName() + ";");
        printWriter.println("import " + List.class.getName() + ";");
        printWriter.println("import " + ArrayList.class.getName() + ";");
        for (String service : services) {
            printWriter.println("import " + service + ";");
        }
    }

    @Override
    public void serializeProperty() {
        printWriter.println("\tprivate List<" + CloudService.class.getSimpleName() + "> services = new " + ArrayList.class.getSimpleName() + "(" + services.size() + ");");
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
            String simpleClass = service.substring(service.lastIndexOf(".") + 1);
            printWriter.println("\t\tif (acceptService(applicationContext, \"" + service + "\")) {");
            printWriter.append("\t\t\t").append(CloudService.class.getSimpleName()).append(" service = new ").append(simpleClass).println("();");
            printWriter.println("\t\t\tservice.loadBean(applicationContext);");
            printWriter.println("\t\t\tservices.add(service);");
            printWriter.println("\t\t}");
        }
    }

    @Override
    public void serializeAutowired() {
        printWriter.println("\t\tfor (CloudService service : services) {");
        printWriter.println("\t\t\tservice.autowired(applicationContext);");
        printWriter.println("\t\t}");
    }

    @Override
    public void serializeRouter() throws IOException {
        printWriter.println("\t\tfor (CloudService service : services) {");
        printWriter.println("\t\t\tservice.router(applicationContext, router);");
        printWriter.println("\t\t}");
    }

    @Override
    public void serializePostConstruct() {
        printWriter.println("\t\tfor (CloudService service : services) {");
        printWriter.println("\t\t\tservice.postConstruct(applicationContext);");
        printWriter.println("\t\t}");
    }

    @Override
    public void serializeDestroy() {
        printWriter.println("\t\tfor (CloudService service : services) {");
        printWriter.println("\t\t\tservice.destroy();");
        printWriter.println("\t\t}");
    }
}
