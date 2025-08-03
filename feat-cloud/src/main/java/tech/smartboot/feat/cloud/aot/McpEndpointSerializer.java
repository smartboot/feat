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
import tech.smartboot.feat.cloud.AsyncResponse;
import tech.smartboot.feat.cloud.annotation.Autowired;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author 三刀
 * @version v1.0 7/20/25
 */
final class McpEndpointSerializer implements Serializer {
    private final ProcessingEnvironment processingEnv;
    private final List<Element> toolMethods;
    private final List<Element> promptMethods;
    private final List<Element> resourceMethods;
    private final McpServerOption mcpEndpoint;
    private final PrintWriter printWriter;
    private final Element element;


    public McpEndpointSerializer(ProcessingEnvironment processingEnv, McpServerOption option, Element element, PrintWriter printWriter) throws IOException {
        this.processingEnv = processingEnv;
        this.element = element;
        this.printWriter = printWriter;
        mcpEndpoint = option;
        toolMethods = element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Tool.class) != null).collect(Collectors.toList());
        promptMethods = element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Prompt.class) != null).collect(Collectors.toList());
        resourceMethods = element.getEnclosedElements().stream().filter(e -> e.getAnnotation(Resource.class) != null).collect(Collectors.toList());
        if (option.isDefault && (FeatUtils.isEmpty(toolMethods) && FeatUtils.isEmpty(promptMethods) && FeatUtils.isEmpty(resourceMethods))) {
            option.enable = false;
        }
    }

    @Override
    public void serializeImport() {
        if (!mcpEndpoint.enable) {
            return;
        }
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
    }

    @Override
    public void serializeProperty() {
        if (!mcpEndpoint.enable) {
            return;
        }
        if (mcpEndpoint.isDefault) {
            printWriter.println("\tprivate McpServer mcpServer = null;");
        } else {
            printWriter.println("\tprivate McpServer mcpServer = new McpServer();");
        }

    }

    /**
     * 只处理McpServer注入
     */
    public void serializeAutowired() {
        if (!mcpEndpoint.enable) {
            return;
        }
        if (mcpEndpoint.isDefault) {
            printWriter.println("\t\tmcpServer = loadBean(\"_mcpServer\", applicationContext);");
        }

        element.getEnclosedElements().stream().filter(field -> field.getAnnotation(Autowired.class) != null && field.asType().toString().equals(McpServer.class.getName())).forEach(field -> {
            String name = field.getSimpleName().toString();
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
                if (!param.asType().toString().equals(field.asType().toString())) {
                    continue;
                }
                hasSetter = true;
            }
            if (hasSetter) {
                printWriter.append("\t\tbean.set").append(name).append("(mcpServer);\n");
            } else {
                printWriter.append("\t\treflectAutowired(bean, \"").append(field.getSimpleName().toString()).append("\", mcpServer);\n");
            }
        });
    }

    @Override
    public void serializePostConstruct() {
        if (!mcpEndpoint.enable) {
            return;
        }
        if (!mcpEndpoint.isDefault && (FeatUtils.isNotEmpty(toolMethods) || FeatUtils.isNotEmpty(promptMethods))) {
            printWriter.println("\t\tmcpServer.getOptions()");
            //配置McpOptions
            if (FeatUtils.isNotBlank(this.mcpEndpoint.streamableEndpoint)) {
                printWriter.append("\t\t\t\t.setMcpEndpoint(\"").append(this.mcpEndpoint.streamableEndpoint).println("\")");
            }
            if (FeatUtils.isNotBlank(this.mcpEndpoint.sseEndpoint)) {
                printWriter.append("\t\t\t\t.setSseEndpoint(\"").append(this.mcpEndpoint.sseEndpoint).println("\")");
            }
            if (FeatUtils.isNotBlank(this.mcpEndpoint.sseMessageEndpoint)) {
                printWriter.append("\t\t\t\t.setSseMessageEndpoint(\"").append(this.mcpEndpoint.sseMessageEndpoint).println("\")");
            }
            if (this.mcpEndpoint.toolEnable) {
                printWriter.println("\t\t\t\t.toolEnable()");
            }
            if (this.mcpEndpoint.resourceEnable) {
                printWriter.println("\t\t\t\t.resourceEnable()");
            }
            if (this.mcpEndpoint.loggingEnable) {
                printWriter.println("\t\t\t\t.loggingEnable()");
            }
            if (this.mcpEndpoint.promptsEnable) {
                printWriter.println("\t\t\t\t.promptsEnable()");
            }
            printWriter.println("\t\t\t\t.getImplementation()");
            printWriter.append("\t\t\t\t.setName(\"").append(this.mcpEndpoint.name).println("\")");
            printWriter.append("\t\t\t\t.setTitle(\"").append(this.mcpEndpoint.title).println("\")");
            printWriter.append("\t\t\t\t.setVersion(\"").append(this.mcpEndpoint.version).println("\");");
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
                printWriter.append(".ofText(\"");
            } else {
                printWriter.append(".ofBinary(\"");
            }
            printWriter.append(prompt.uri()).append("\", \"").append(prompt.name());
            if (FeatUtils.isNotBlank(prompt.mimeType())) {
                printWriter.append("\",\"").append(prompt.mimeType()).println("\", \"\")");
            } else {
                printWriter.append("\", null, \"\")");
            }


            if (FeatUtils.isNotBlank(prompt.description())) {
                printWriter.append("\t\t\t\t\t.description(\"").append(prompt.description()).println("\")");
            }

            //doAction
            serializeResourceAction(printWriter, promptMethod, "");
            printWriter.println("\t\t\t);");
            printWriter.println("\t\t}");
        }
        printWriter.println("\t\tif (applicationContext.getOptions().devMode()) {");
        printWriter.println("\t\t\tprintlnMcp(mcpServer);");
        printWriter.println("\t\t}");
    }

    @Override
    public void serializeRouter() {
        //注册Router
        if (FeatUtils.isNotBlank(mcpEndpoint.streamableEndpoint)) {
            printWriter.println("\t\tapplicationContext.getRouter().route(\"" + mcpEndpoint.streamableEndpoint + "\", mcpServer.mcpHandler());");
        }
        if (FeatUtils.isNotBlank(mcpEndpoint.sseEndpoint)) {
            printWriter.println("\t\tapplicationContext.getRouter().route(\"" + mcpEndpoint.sseEndpoint + "\", mcpServer.sseHandler());");
        }
        if (FeatUtils.isNotBlank(mcpEndpoint.sseMessageEndpoint)) {
            printWriter.println("\t\tapplicationContext.getRouter().route(\"" + mcpEndpoint.sseMessageEndpoint + "\", mcpServer.sseMessageHandler());");
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
        } else if (AsyncResponse.class.getName().equals(returnType.toString())) {
            printWriter.println("\t\t\t\tresult.getFuture().thenAccept(rst -> {");
            printWriter.println("\t\t\t\t\tctx.getFuture().complete(ToolResult.ofStructuredContent(JSONObject.parseObject(JSON.toJSONString(rst))));");
            printWriter.println("\t\t\t\t});");
        } else if (CompletableFuture.class.getName().equals(((DeclaredType) returnType).asElement().toString())) {
            printWriter.println("\t\t\t\tresult.thenAccept(rst -> {");
            printWriter.println("\t\t\t\t\tctx.getFuture().complete(ToolResult.ofStructuredContent(JSONObject.parseObject(JSON.toJSONString(rst))));");
            printWriter.println("\t\t\t\t});");
        } else if (!returnType.toString().startsWith("java.")) {
            printWriter.println("\t\t\t\treturn ToolResult.ofStructuredContent(JSONObject.parseObject(JSON.toJSONString(result)));");
        } else {
            throw new FeatException("unSupport returnType:" + returnType + " please check [" + element.toString() + "@" + toolMethod.getSimpleName() + "]");
        }

        printWriter.println("\t\t\t});");
    }

    private void serializeOutputSchema(PrintWriter printWriter, TypeMirror returnType) {
//        if (returnType.getKind().isPrimitive()) {
//            printWriter.println("\t\t\ttool.outputSchema(" + ServerTool.class.getSimpleName() + ".stringProperty(\"result\", \"返回结果\"));");
//        } else {
//            printWriter.println("\t\t\ttool.outputSchema(" + ServerTool.class.getSimpleName() + ".stringProperty(\"result\", \"返回结果\"));");
//        }
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
            String paramType = param.asType().toString();
            if (paramType.equals("java.lang.String")) {
                if (required) {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".requiredStringProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                } else {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".stringProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                }
                inputParams.append("ctx.getArguments().getString(\"").append(param.getSimpleName()).append("\"), ");
            } else if (param.asType().getKind().isPrimitive()) {
                if (boolean.class.getName().equals(paramType)) {
                    if (required) {
                        printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".requiredBoolProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                    } else {
                        printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".boolProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                    }
                } else {
                    if (required) {
                        printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".requiredNumberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                    } else {
                        printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".numberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                    }
                }
                inputParams.append("ctx.getArguments().get").append(paramType.substring(0, 1).toUpperCase()).append(paramType.substring(1)).append("Value(\"").append(param.getSimpleName()).append("\"), ");
            } else if (Long.class.getName().equals(paramType)) {
                if (required) {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".requiredNumberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                } else {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".numberProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                }
                inputParams.append("ctx.getArguments().getLong(\"").append(param.getSimpleName()).append("\"), ");
            } else if (Boolean.class.getName().equals(paramType)) {
                if (required) {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".requiredBoolProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                } else {
                    printWriter.println("\t\t\ttool.inputSchema(" + ServerTool.class.getSimpleName() + ".boolProperty(\"" + param.getSimpleName().toString() + "\", \"" + description + "\"));");
                }
                inputParams.append("ctx.getArguments().getBoolean(\"").append(param.getSimpleName()).append("\"), ");
            } else {
                throw new FeatException("unSupport paramType:" + param.asType().toString() + " please check [" + element.toString() + "@" + toolMethod.getSimpleName() + "]");
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
                printWriter.println("\t\t\t\t\t\treturn PromptMessage.ofImage(" + RoleEnum.class.getSimpleName() + "." + prompt.role() + ", result, \"" + prompt.mimeType() + "\");");
            } else if (prompt.type() == PromptType.AUDIO) {
                printWriter.println("\t\t\t\t\t\treturn PromptMessage.ofAudio(" + RoleEnum.class.getSimpleName() + "." + prompt.role() + ", result, \"" + prompt.mimeType() + "\");");
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

    static class McpServerOption {
        boolean enable = true;
        /**
         * 是否为默认MCP服务
         */
        boolean isDefault;
        /**
         * MCP服务名称
         * 对应MCP协议中服务的name字段
         * 默认值："feat-mcp-server"
         */
        String name;


        /**
         * MCP服务标题
         * 对应MCP协议中服务的title字段
         * 默认值："Feat MCP Server"
         */
        String title;


        /**
         * MCP服务版本
         * 对应MCP协议中服务的version字段
         * 默认值：Feat.VERSION
         */
        String version;

        /**
         * SSE端点地址
         * 用于建立SSE连接的端点URL路径
         * 对应MCP协议中的SSE通信机制
         */
        String sseEndpoint;

        /**
         * SSE消息端点地址
         * 用于发送SSE消息的端点URL路径
         * 对应MCP协议中的SSE消息传递机制
         */
        String sseMessageEndpoint;


        /**
         * 流式传输端点地址
         * 用于支持流式数据传输的端点URL路径
         * 对应MCP协议中的流式传输机制
         */
        String streamableEndpoint;

        /**
         * 资源功能开关
         * 控制是否启用MCP资源(resources/list)功能
         * 默认值：true(启用)
         *
         * @see <a href="https://modelcontextprotocol.io/specification#resources">MCP Resources</a>
         */
        boolean resourceEnable;

        /**
         * 工具功能开关
         * 控制是否启用MCP工具(tools/list, tools/call)功能
         * 默认值：true(启用)
         *
         * @see <a href="https://modelcontextprotocol.io/specification#tools">MCP Tools</a>
         */
        boolean toolEnable;


        /**
         * 提示词功能开关
         * 控制是否启用MCP提示词(prompts/list)功能
         * 默认值：true(启用)
         *
         * @see <a href="https://modelcontextprotocol.io/specification#prompts">MCP Prompts</a>
         */
        boolean promptsEnable;


        /**
         * 日志功能开关
         * 控制是否启用MCP日志(logging)功能
         * 默认值：true(启用)
         *
         * @see <a href="https://modelcontextprotocol.io/specification#logging">MCP Logging</a>
         */
        boolean loggingEnable;

        void init(McpEndpoint mcpEndpoint) {
            this.isDefault = false;
            this.name = mcpEndpoint.name();
            this.title = mcpEndpoint.title();
            this.version = mcpEndpoint.version();
            this.sseEndpoint = mcpEndpoint.sseEndpoint();
            this.sseMessageEndpoint = mcpEndpoint.sseMessageEndpoint();
            this.resourceEnable = mcpEndpoint.resourceEnable();
            this.toolEnable = mcpEndpoint.toolEnable();
            this.promptsEnable = mcpEndpoint.promptsEnable();
            this.loggingEnable = mcpEndpoint.loggingEnable();
        }
    }
}
