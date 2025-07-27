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
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.cloud.AbstractServiceLoader;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.router.Router;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * @author 三刀
 * @version v1.0 7/27/25
 */
public class DefaultMcpServerSerializer implements Serializer {
    private static final String PACKAGE = "tech.smartboot.feat.sandao";
    private static final String CLASS_NAME = "FeatDefaultMcpServerAptLoader";
    private final PrintWriter printWriter;

    public DefaultMcpServerSerializer(ProcessingEnvironment processingEnv, String config) throws IOException {
        FileObject preFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, PACKAGE, CLASS_NAME + ".java");
        File f = new File(preFileObject.toUri());
        if (f.exists()) {
            f.delete();
        }

        JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(PACKAGE + "." + CLASS_NAME);
        Writer writer = javaFileObject.openWriter();
        printWriter = new PrintWriter(writer);
    }

    public void serializeImport() {
        printWriter.println("import " + AbstractServiceLoader.class.getName() + ";");
        printWriter.println("import " + ApplicationContext.class.getName() + ";");
        printWriter.println("import " + Router.class.getName() + ";");
        printWriter.println("import " + JSONObject.class.getName() + ";");
        printWriter.println("import " + McpServer.class.getName() + ";");
        printWriter.println("import com.alibaba.fastjson2.JSON;");
    }

    @Override
    public void serializeProperty() {
        printWriter.println("\tprivate McpServer bean;");
    }

    @Override
    public void serializeLoadBean() {
        printWriter.println("\t\tbean = new McpServer(); ");
        printWriter.println("\t\tapplicationContext.addBean(\"_mcpServer\", bean);");
    }

    @Override
    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    @Override
    public String className() {
        return CLASS_NAME;
    }

    @Override
    public String packageName() {
        return PACKAGE;
    }
}
