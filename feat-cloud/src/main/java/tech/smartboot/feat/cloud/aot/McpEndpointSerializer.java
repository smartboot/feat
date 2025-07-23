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

import tech.smartboot.feat.ai.mcp.enums.PromptType;
import tech.smartboot.feat.ai.mcp.enums.RoleEnum;
import tech.smartboot.feat.ai.mcp.model.PromptMessage;
import tech.smartboot.feat.ai.mcp.model.ToolResult;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.ai.mcp.server.model.ServerPrompt;
import tech.smartboot.feat.ai.mcp.server.model.ServerResource;
import tech.smartboot.feat.ai.mcp.server.model.ServerTool;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.cloud.annotation.mcp.Param;
import tech.smartboot.feat.cloud.annotation.mcp.Prompt;
import tech.smartboot.feat.cloud.annotation.mcp.Resource;
import tech.smartboot.feat.cloud.annotation.mcp.Tool;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 三刀
 * @version v1.0 7/20/25
 */
final class McpEndpointSerializer extends AbstractSerializer {
    private final ProcessingEnvironment processingEnv;
    private final List<Element> toolMethods;
    private final List<Element> promptMethods;
    private final List<Element> resourceMethods;
    private final McpEndpoint controller;

    public McpEndpointSerializer(ProcessingEnvironment processingEnv, String config, Element element) throws IOException {
        super(processingEnv, config, element);
        this.processingEnv = processingEnv;
        controller = element.getAnnotation(McpEndpoint.class);
        toolMethods = element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Tool.class) != null).collect(Collectors.toList());
        promptMethods = element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Prompt.class) != null).collect(Collectors.toList());
        resourceMethods = element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Resource.class) != null).collect(Collectors.toList());
    }

    @Override
    public void serializeImport() {
        printWriter.println("import " + McpServer.class.getName() + ";");
        if (FeatUtils.isNotEmpty(promptMethods)) {
            printWriter.println("import " + ServerPrompt.class.getName() + ";");
            printWriter.println("import " + tech.smartboot.feat.ai.mcp.model.Prompt.class.getName() + ";");
            printWriter.println("import " + RoleEnum.class.getName() + ";");
            printWriter.println("import " + PromptMessage.class.getName() + ";");
        }
        if (FeatUtils.isNotEmpty(toolMethods)) {
            printWriter.println("import " + ServerTool.class.getName() + ";");
            printWriter.println("import " + ToolResult.class.getName() + ";");
        }
        if (FeatUtils.isNotEmpty(resourceMethods)) {
            printWriter.println("import " + ServerResource.class.getName() + ";");
        }
        super.serializeImport();
    }

    @Override
    public void serializeProperty() {
        printWriter.println("\tprivate McpServer mcpServer = new McpServer();");
        printWriter.println("\tprivate " + element.getSimpleName() + " bean = new " + element.getSimpleName() + "();");
    }

    @Override
    public void serializeLoadBean() {

    }

    public void serializeAutowired() {
        super.serializeAutowired();

        if (FeatUtils.isNotEmpty(toolMethods) || FeatUtils.isNotEmpty(promptMethods)) {
//            if (controller == null) {
//                throw new FeatException("@Tool is only supported for use in classes marked with the @McpEndpoint annotation!");
//            }
            printWriter.println("\t\tmcpServer.getOptions()");
            //配置McpOptions
            if (FeatUtils.isNotBlank(controller.mcpStreamableEndpoint())) {
                printWriter.append("\t\t\t\t.setMcpEndpoint(\"").append(controller.mcpStreamableEndpoint()).println("\")");
            }
            if (FeatUtils.isNotBlank(controller.mcpSseEndpoint())) {
                printWriter.append("\t\t\t\t.setSseEndpoint(\"").append(controller.mcpSseEndpoint()).println("\")");
            }
            if (FeatUtils.isNotBlank(controller.mcpSseMessageEndpoint())) {
                printWriter.append("\t\t\t\t.setSseMessageEndpoint(\"").append(controller.mcpSseMessageEndpoint()).println("\")");
            }
            if (controller.toolEnable()) {
                printWriter.println("\t\t\t\t.toolEnable()");
            }
            if (controller.resourceEnable()) {
                printWriter.println("\t\t\t\t.resourceEnable()");
            }
            if (controller.loggingEnable()) {
                printWriter.println("\t\t\t\t.loggingEnable()");
            }
            if (controller.promptsEnable()) {
                printWriter.println("\t\t\t\t.promptsEnable()");
            }
            printWriter.println("\t\t\t\t.getImplementation()");
            printWriter.append("\t\t\t\t.setName(\"").append(controller.name()).println("\")");
            printWriter.append("\t\t\t\t.setTitle(\"").append(controller.title()).println("\")");
            printWriter.append("\t\t\t\t.setVersion(\"").append(controller.version()).println("\");");
        }
        for (Element t : toolMethods) {
            ExecutableElement toolMethod = (ExecutableElement) t;
            Tool tool = toolMethod.getAnnotation(Tool.class);
            String toolName = tool.name();
            if (FeatUtils.isBlank(toolName)) {
                toolName = element + "-" + toolMethod.getSimpleName().toString();
            }
            printWriter.println("\t\t{");
            printWriter.println("\t\t\t" + ServerTool.class.getSimpleName() + " tool = " + ServerTool.class.getSimpleName() + ".of(\"" + toolName + "\");");
            if (FeatUtils.isNotBlank(tool.description())) {
                printWriter.println("\t\t\ttool.description(\"" + tool.description() + "\");");
            }
            //入参
            String inputParams = serializeInputSchema(printWriter, toolMethod);
            //出参
            serializeOutputSchema(printWriter, toolMethod.getReturnType());

            //doAction
            serializeDoAction(printWriter, toolMethod, inputParams);

            printWriter.println("\t\t\tmcpServer.addTool(tool);");
            printWriter.println("\t\t}");
        }

        for (Element t : promptMethods) {
            ExecutableElement promptMethod = (ExecutableElement) t;
            Prompt prompt = promptMethod.getAnnotation(Prompt.class);
            String name = prompt.name();
            if (FeatUtils.isBlank(name)) {
                name = element + "-" + promptMethod.getSimpleName().toString();
            }
            printWriter.println("\t\t{");
            printWriter.append("\t\t\tmcpServer.addPrompt(").append(ServerPrompt.class.getSimpleName()).append(".of(\"").append(name).append("\")");
            if (FeatUtils.isNotBlank(prompt.description())) {
                printWriter.append("\n\t\t\t\t\t.description(\"").append(prompt.description()).append("\")");
            }
            //入参
            String inputParams = serializePromptArguments(printWriter, promptMethod);
            //出参
//            serializeOutputSchema(printWriter, toolMethod.getReturnType());

            //doAction
            serializePromptAction(printWriter, promptMethod, inputParams);
            printWriter.println("\t\t\t);");
            printWriter.println("\t\t}");
        }

        for (Element t : resourceMethods) {
            ExecutableElement promptMethod = (ExecutableElement) t;
            Resource prompt = promptMethod.getAnnotation(Resource.class);
            String name = prompt.name();
            if (FeatUtils.isBlank(name)) {
                name = element + "-" + promptMethod.getSimpleName().toString();
            }
            printWriter.println("\t\t{");
            printWriter.append("\t\t\tmcpServer.addResource(").append(ServerResource.class.getSimpleName());
            if (prompt.isText()) {
                printWriter.append(".ofText(\"").append(prompt.uri()).append("\", \"").append(prompt.name()).println("\", \"\")");
            } else {
                printWriter.append(".ofBinary(\"").append(prompt.uri()).append("\", \"").append(prompt.name()).println("\", \"\")");
            }

            if (FeatUtils.isNotBlank(prompt.description())) {
                printWriter.append("\t\t\t\t\t.description(\"").append(prompt.description()).println("\")");
            }

            //doAction
            serializeResourceAction(printWriter, promptMethod, "");
            printWriter.println("\t\t\t);");
            printWriter.println("\t\t}");
        }
    }

    @Override
    public void serializeRouter() {
        McpEndpoint mcpEndpoint = element.getAnnotation(McpEndpoint.class);
        //注册Router
        if (FeatUtils.isNotBlank(mcpEndpoint.mcpStreamableEndpoint())) {
            printWriter.println("\t\tapplicationContext.getRouter().route(\"" + mcpEndpoint.mcpStreamableEndpoint() + "\", mcpServer.mcpHandler());");
        }
        if (FeatUtils.isNotBlank(mcpEndpoint.mcpSseEndpoint())) {
            printWriter.println("\t\tapplicationContext.getRouter().route(\"" + mcpEndpoint.mcpSseEndpoint() + "\", mcpServer.sseHandler());");
        }
        if (FeatUtils.isNotBlank(mcpEndpoint.mcpSseMessageEndpoint())) {
            printWriter.println("\t\tapplicationContext.getRouter().route(\"" + mcpEndpoint.mcpSseMessageEndpoint() + "\", mcpServer.sseMessageHandler());");
        }
    }

    private void serializeDoAction(PrintWriter printWriter, ExecutableElement toolMethod, String inputParams) {
        TypeMirror returnType = toolMethod.getReturnType();
        printWriter.println("\t\t\ttool.doAction(ctx -> {");
        printWriter.println("\t\t\t\t" + returnType + " result = bean." + toolMethod.getSimpleName() + "(" + inputParams + ");");
        if (String.class.getName().equals(returnType.toString())) {
            printWriter.println("\t\t\t\treturn ToolResult.ofText(result);");
        } else if (returnType.getKind().isPrimitive()) {
            printWriter.println("\t\t\t\treturn ToolResult.ofText(String.valueOf(result));");
        } else if (processingEnv.getTypeUtils().isSubtype(returnType, processingEnv.getElementUtils().getTypeElement(ToolResult.class.getName()).asType())) {
            printWriter.println("\t\t\t\treturn result;");
        } else {
            printWriter.println("\t\t\t\treturn null;");
        }

        printWriter.println("\t\t\t});");
    }

    private void serializeOutputSchema(PrintWriter printWriter, TypeMirror returnType) {
        if (returnType.getKind().isPrimitive()) {
            printWriter.println("\t\t\ttool.outputSchema(" + ServerTool.class.getSimpleName() + ".stringProperty(\"result\", \"返回结果\"));");
        } else {
            printWriter.println("\t\t\ttool.outputSchema(" + ServerTool.class.getSimpleName() + ".stringProperty(\"result\", \"返回结果\"));");
        }
    }

    private String serializeInputSchema(PrintWriter printWriter, ExecutableElement toolMethod) {
        StringBuilder inputParams = new StringBuilder();
        for (VariableElement param : toolMethod.getParameters()) {
            Param toolParam = param.getAnnotation(Param.class);
            String description = "";
            boolean required = false;
            if (toolParam != null) {
                description = toolParam.description();
                required = toolParam.required();
            }
            if (param.asType().toString().equals("java.lang.String")) {
                if (required) {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".requiredStringProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                } else {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".stringProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                }
                inputParams.append("ctx.getArguments().getString(\"").append(param.getSimpleName()).append("\"), ");
            } else if (param.asType().toString().equals("int")) {
                if (required) {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".requiredNumberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                } else {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".numberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                }
                inputParams.append("ctx.getArguments().getIntValue(\"").append(param.getSimpleName()).append("\"), ");
            } else if (param.asType().toString().equals("boolean")) {
                if (required) {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".requiredBoolProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                } else {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".boolProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                }
                inputParams.append("ctx.getArguments().getBooleanValue(\"").append(param.getSimpleName()).append("\"), ");
            }
        }

        return inputParams.length() > 2 ? inputParams.substring(0, inputParams.length() - 2) : "";
    }

    private String serializePromptArguments(PrintWriter printWriter, ExecutableElement toolMethod) {
        StringBuilder inputParams = new StringBuilder();
        for (VariableElement param : toolMethod.getParameters()) {
            Param toolParam = param.getAnnotation(Param.class);
            String description = "";
            boolean required = false;
            if (toolParam != null) {
                description = toolParam.description();
                required = toolParam.required();
            }
            if (required) {
                printWriter.append("\n\t\t\t\t\t.arguments(").append(tech.smartboot.feat.ai.mcp.model.Prompt.class.getSimpleName()).append(".requiredArgument(\"").append(param.getSimpleName().toString()).append("\", \"").append(description).append("\"))");
            } else {
                printWriter.append("\n\t\t\t\t\t.arguments(").append(tech.smartboot.feat.ai.mcp.model.Prompt.class.getSimpleName()).append(".argument(\"").append(param.getSimpleName().toString()).append("\", \"").append(description).append("\"))");
            }
            if (param.asType().toString().equals("java.lang.String")) {
                inputParams.append("ctx.getArguments().getString(\"").append(param.getSimpleName()).append("\"), ");
            } else if (param.asType().toString().equals("int")) {
                inputParams.append("ctx.getArguments().getIntValue(\"").append(param.getSimpleName()).append("\"), ");
            } else if (param.asType().toString().equals("boolean")) {
                inputParams.append("ctx.getArguments().getBooleanValue(\"").append(param.getSimpleName()).append("\"), ");
            } else {
                throw new FeatException("参数类型不支持");
            }
        }

        return inputParams.length() > 2 ? inputParams.substring(0, inputParams.length() - 2) : "";
    }

    private void serializePromptAction(PrintWriter printWriter, ExecutableElement promptMethod, String inputParams) {
        TypeMirror returnType = promptMethod.getReturnType();
        Prompt prompt = promptMethod.getAnnotation(Prompt.class);
        printWriter.println("\n\t\t\t\t\t.doAction(ctx -> {");
        if (String.class.getName().equals(returnType.toString())) {
            printWriter.println("\t\t\t\t\t\tString result = bean." + promptMethod.getSimpleName() + "(" + inputParams + ");");
            if (prompt.type() == PromptType.TEXT) {
                printWriter.println("\t\t\t\t\t\treturn PromptMessage.ofText(" + RoleEnum.class.getSimpleName() + "." + prompt.role() + ", result);");
            } else if (prompt.type() == PromptType.IMAGE) {
                printWriter.println("\t\t\t\t\t\treturn PromptMessage.ofImage(" + RoleEnum.class.getSimpleName() + "." + prompt.role() + ", result, \"" + prompt.mineType() + "\");");
            } else if (prompt.type() == PromptType.AUDIO) {
                printWriter.println("\t\t\t\t\t\treturn PromptMessage.ofAudio(" + RoleEnum.class.getSimpleName() + "." + prompt.role() + ", result, \"" + prompt.mineType() + "\");");
            } else if (prompt.type() == PromptType.EMBEDDED_RESOURCE) {
                throw new FeatException("When the type of the @Prompt annotation is PromptType.EMBEDDED_RESOURCE, the result must be returned via PromptMessage.ofEmbeddedResource. please check [" + element.toString() + "@" + promptMethod.getSimpleName() + "]");
            } else {
                throw new FeatException("不支持的返回类型");
            }
        } else if (returnType.getKind().isPrimitive()) {
            if (prompt.type() != PromptType.TEXT) {
                throw new FeatException("When the return type of [" + element.toString() + "@" + promptMethod.getSimpleName() + "] is " + returnType.getKind() + ", the type of the @Prompt annotation must be PromptType.TEXT.");
            }
            printWriter.println("\t\t\t\t\t\t" + returnType + " result = bean." + promptMethod.getSimpleName() + "(" + inputParams + ");");
            printWriter.println("\t\t\t\t\t\treturn PromptMessage.ofText(" + RoleEnum.class.getSimpleName() + "." + prompt.role() + ", String.valueOf(result));");
        } else if (PromptMessage.class.getName().equals(((DeclaredType) returnType).asElement().toString())) {
            printWriter.println("\t\t\t\t\t\treturn bean." + promptMethod.getSimpleName() + "(" + inputParams + ");");
        } else {
            throw new FeatException("unSupport returnType[" + returnType + "] , please check [" + element.toString() + "@" + promptMethod.getSimpleName() + "]");
        }

        printWriter.println("\t\t\t\t\t})");
    }

    private void serializeResourceAction(PrintWriter printWriter, ExecutableElement promptMethod, String inputParams) {
        TypeMirror returnType = promptMethod.getReturnType();
        Resource resource = promptMethod.getAnnotation(Resource.class);
        printWriter.println("\t\t\t\t\t.doAction(ctx -> {");
        if (String.class.getName().equals(returnType.toString())) {
            printWriter.println("\t\t\t\t\t\treturn bean." + promptMethod.getSimpleName() + "(" + inputParams + ");");
        } else {
            throw new FeatException("unSupport returnType[" + returnType + "] , please check [" + element.toString() + "@" + promptMethod.getSimpleName() + "]");
        }

        printWriter.println("\t\t\t\t\t})");
    }
}
