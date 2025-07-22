/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp;

import tech.smartboot.feat.ai.mcp.server.model.ServerTool;
import tech.smartboot.feat.cloud.annotation.mcp.Tool;
import tech.smartboot.feat.cloud.annotation.mcp.ToolParam;
import tech.smartboot.feat.core.common.FeatUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 三刀
 * @version v1.0 7/20/25
 */
public class McpServerSerializer {
    public void serialize(PrintWriter printWriter, Element element) {
        List<Element> toolMethods = element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Tool.class) != null).collect(Collectors.toList());
        if (FeatUtils.isNotEmpty(toolMethods)) {
            printWriter.println("\t\ttech.smartboot.feat.ai.mcp.server.McpServer mcpServer = applicationContext.getBean(\"mcpServer\");");
        }
        for (Element toolMethod : toolMethods) {
            Tool tool = toolMethod.getAnnotation(Tool.class);
            String toolName = tool.name();
            if (FeatUtils.isBlank(toolName)) {
                toolName = element + "-" + toolMethod.getSimpleName().toString();
            }
            printWriter.println("\t\t{");
            printWriter.println("\t\t\t" + ServerTool.class.getName() + " tool = " + ServerTool.class.getName() + ".of(\"" + toolName + "\");");
            if (FeatUtils.isNotBlank(tool.description())) {
                printWriter.println("\t\t\ttool.description(\"" + tool.description() + "\");");
            }
            //入参
            for (VariableElement param : ((ExecutableElement) toolMethod).getParameters()) {
                ToolParam toolParam = param.getAnnotation(ToolParam.class);
                String description = "";
                boolean required = false;
                if (toolParam != null) {
                    description = toolParam.description();
                    required = toolParam.required();
                }
                if (param.asType().toString().equals("java.lang.String")) {
                    if (required) {
                        printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getName() + ".requiredStringProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                    } else {
                        printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getName() + ".stringProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                    }
                } else if (param.asType().toString().equals("int")) {
                    if (required) {
                        printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getName() + ".requiredNumberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                    } else {
                        printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getName() + ".numberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                    }
                }
            }

            printWriter.println("\t\t\tmcpServer.addTool(tool);");
            printWriter.println("\t\t}");
        }

    }
}
