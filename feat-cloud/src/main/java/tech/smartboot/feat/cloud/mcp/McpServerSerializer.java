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

import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.ai.mcp.server.model.ServerTool;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.cloud.annotation.mcp.Tool;
import tech.smartboot.feat.cloud.annotation.mcp.ToolParam;
import tech.smartboot.feat.cloud.serializer.value.FeatYamlValueSerializer;
import tech.smartboot.feat.core.common.FeatUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 三刀
 * @version v1.0 7/20/25
 */
public class McpServerSerializer {
    private final FeatYamlValueSerializer yamlValueSerializer;
    public static final String DEFAULT_BEAN_NAME = "mcpServer" + System.nanoTime();

    public McpServerSerializer(FeatYamlValueSerializer yamlValueSerializer) {
        this.yamlValueSerializer = yamlValueSerializer;
    }

    public void serialize(ProcessingEnvironment processingEnv, PrintWriter printWriter, Element element) {
        McpEndpoint controller = element.getAnnotation(McpEndpoint.class);
        List<Element> toolMethods = element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Tool.class) != null).collect(Collectors.toList());
        if (FeatUtils.isNotEmpty(toolMethods)) {
            if (controller != null) {
                printWriter.append("\t\ttech.smartboot.feat.ai.mcp.server.McpServer mcpServer = new tech.smartboot.feat.ai.mcp.server.McpServer(opts -> opts");
                if (FeatUtils.isNotBlank(controller.mcpStreamableEndpoint())) {
                    printWriter.append(".setMcpEndpoint(\"").append(controller.mcpStreamableEndpoint()).append("\")");
                }
                if (FeatUtils.isNotBlank(controller.mcpSseEndpoint())) {
                    printWriter.append(".setSseEndpoint(\"").append(controller.mcpSseEndpoint()).append("\")");
                }
                printWriter.append(".getImplementation().setName(\"").append(controller.name()).append("\").setTitle(\"").append(controller.title()).append("\").setVersion(\"").append(controller.version()).append("\"));\n");

                if (FeatUtils.isNotBlank(controller.mcpStreamableEndpoint())) {
                    printWriter.println("\t\tapplicationContext.getRouter().route(\"" + controller.mcpStreamableEndpoint() + "\", mcpServer.mcpHandler());");
                }
                if (FeatUtils.isNotBlank(controller.mcpSseEndpoint())) {
                    printWriter.println("\t\tapplicationContext.getRouter().route(\"" + controller.mcpSseEndpoint() + "\", mcpServer.sseHandler());");
                }
            } else {
                printWriter.println("\t\ttech.smartboot.feat.ai.mcp.server.McpServer mcpServer = applicationContext.getBean(\"" + DEFAULT_BEAN_NAME + "\");");
            }
        }
        for (Element t : toolMethods) {
            ExecutableElement toolMethod = (ExecutableElement) t;
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
            String inputParams = serializeInputSchema(printWriter, toolMethod);
            //出参
            serializeOutputSchema(printWriter, toolMethod.getReturnType());

            //doAction
            serializeDoAction(processingEnv, printWriter, toolMethod, inputParams);

            printWriter.println("\t\t\tmcpServer.addTool(tool);");
            printWriter.println("\t\t}");
        }

    }

    private void serializeDoAction(ProcessingEnvironment processingEnv, PrintWriter printWriter, ExecutableElement toolMethod, String inputParams) {
        TypeMirror returnType = toolMethod.getReturnType();
        printWriter.println("\t\t\ttool.doAction(ctx -> {");
        printWriter.println("\t\t\t\t" + returnType + " result = bean." + toolMethod.getSimpleName() + "(" + inputParams + ");");
        if (String.class.getName().equals(returnType.toString())) {
            printWriter.println("\t\t\t\treturn tech.smartboot.feat.ai.mcp.model.ToolResult.ofText(result);");
        } else if (returnType.getKind().isPrimitive()) {
            printWriter.println("\t\t\t\treturn tech.smartboot.feat.ai.mcp.model.ToolResult.ofText(String.valueOf(result));");
        } else if (processingEnv.getTypeUtils().isSubtype(returnType, processingEnv.getElementUtils().getTypeElement(ToolResult.class.getName()).asType())) {
            printWriter.println("\t\t\t\treturn result;");
        } else {
            printWriter.println("\t\t\t\treturn null;");
        }

        printWriter.println("\t\t\t});");
    }

    private void serializeOutputSchema(PrintWriter printWriter, TypeMirror returnType) {
        if (returnType.getKind().isPrimitive()) {
            printWriter.println("\t\t\ttool.outputSchema(" + ServerTool.class.getName() + ".stringProperty(\"result\", \"返回结果\"));");
        } else {
            printWriter.println("\t\t\ttool.outputSchema(" + ServerTool.class.getName() + ".stringProperty(\"result\", \"返回结果\"));");
        }
    }

    private String serializeInputSchema(PrintWriter printWriter, ExecutableElement toolMethod) {
        StringBuilder inputParams = new StringBuilder();
        for (VariableElement param : toolMethod.getParameters()) {
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
                inputParams.append("ctx.getArguments().getString(\"").append(param.getSimpleName()).append("\"), ");
            } else if (param.asType().toString().equals("int")) {
                if (required) {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getName() + ".requiredNumberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                } else {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getName() + ".numberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                }
                inputParams.append("ctx.getArguments().getIntValue(\"").append(param.getSimpleName()).append("\"), ");
            } else if (param.asType().toString().equals("boolean")) {
                if (required) {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getName() + ".requiredBoolProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                } else {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getName() + ".boolProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                }
                inputParams.append("ctx.getArguments().getBooleanValue(\"").append(param.getSimpleName()).append("\"), ");
            }
        }

        return inputParams.length() > 2 ? inputParams.substring(0, inputParams.length() - 2) : "";
    }
}
