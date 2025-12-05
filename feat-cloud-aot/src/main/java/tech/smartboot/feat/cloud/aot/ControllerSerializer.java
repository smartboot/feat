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
import tech.smartboot.feat.cloud.AsyncBodyReadUpgrade;
import tech.smartboot.feat.cloud.AsyncResponse;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.InterceptorMapping;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.cloud.aot.controller.JsonSerializer;
import tech.smartboot.feat.core.common.ByteTree;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.RouterHandler;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀
 * @version v1.0 7/23/25
 */
final class ControllerSerializer extends AbstractSerializer {
    private static final int RETURN_TYPE_VOID = 0;
    private static final int RETURN_TYPE_STRING = 1;
    private static final int RETURN_TYPE_OBJECT = 2;
    private static final int RETURN_TYPE_BYTE_ARRAY = 3;
    private final Map<String, String> bytesCache = new HashMap<>();
    private final McpEndpointSerializer mcpEndpointSerializer;
    private final McpEndpointSerializer.McpServerOption mcpServerOption;

    public ControllerSerializer(ProcessingEnvironment processingEnv, String config, Element element) throws IOException {
        super(processingEnv, config, element);
        McpEndpoint mcpEndpoint = element.getAnnotation(McpEndpoint.class);
        mcpServerOption = new McpEndpointSerializer.McpServerOption();
        if (mcpEndpoint != null) {
            mcpServerOption.init(mcpEndpoint);
        } else {
            mcpServerOption.isDefault = true;
            mcpServerOption.enable = !"false".equals(JSONPath.eval(config, "$.server.mcp.server.enable"));
        }
        mcpEndpointSerializer = new McpEndpointSerializer(processingEnv, mcpServerOption, element, getPrintWriter());
    }

    public boolean rootMcpEnable() {
        return mcpServerOption.isDefault && mcpServerOption.enable;
    }

    @Override
    public void serializeImport() {
        mcpEndpointSerializer.serializeImport();
        super.serializeImport();
        printWriter.println("import " + HeaderValue.class.getName() + ";");
        printWriter.println("import " + RouterHandler.class.getName() + ";");
        printWriter.println("import " + HttpEndpoint.class.getName() + ";");
        printWriter.println("import " + IOException.class.getName() + ";");
        printWriter.println("import " + AsyncBodyReadUpgrade.class.getName() + ";");
    }

    @Override
    public void serializeProperty() {
        super.serializeProperty();
        mcpEndpointSerializer.serializeProperty();
    }

    @Override
    public void serializeAutowired() {
        mcpEndpointSerializer.serializeAutowired();
        super.serializeAutowired();
    }

    @Override
    public void serializePostConstruct() {
        super.serializePostConstruct();
        mcpEndpointSerializer.serializePostConstruct();
    }

    @Override
    public void serializeRouter() throws IOException {
        Controller controller = element.getAnnotation(Controller.class);
        //遍历所有方法,获得RequestMapping注解
        String basePath = controller.value();
        if (!FeatUtils.startsWith(basePath, "/")) {
            basePath = "/" + basePath;
        }
        for (Element se : element.getEnclosedElements()) {
            RequestMapping requestMapping = se.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                String requestURL = requestMapping.value();
                if (basePath.endsWith("/") && requestURL.startsWith("/")) {
                    requestURL = basePath + requestURL.substring(1);
                } else if (basePath.endsWith("/") && !requestURL.startsWith("/") || !basePath.endsWith("/") && requestURL.startsWith("/") || requestURL.isEmpty()) {
                    requestURL = basePath + requestURL;
                } else {
                    requestURL = basePath + "/" + requestURL;
                }
                // 将路径参数格式从 "{param}" 转换为 ":param" 格式
                // 例如: 将 /user/{id}/ 转换为 /user/:id/
                requestURL = requestURL.replace("/{", "/:").replace("}/", "/");
                // 处理路径末尾的参数情况，如 /user/{id} 转换为 /user/:id
                if (requestURL.endsWith("}")) {
                    requestURL = requestURL.substring(0, requestURL.length() - 1);
                }

                if (FeatUtils.isBlank(requestURL)) {
                    throw new FeatException("the value of RequestMapping on " + element.getSimpleName() + "@" + se.getSimpleName() + " is not allowed to be empty.");
                }

                StringBuilder routeMethods = new StringBuilder(", new String[]{");
                for (RequestMethod httpMethod : requestMapping.method()) {
                    routeMethods.append("\"").append(httpMethod.name()).append("\", ");
                }
                if (requestMapping.method().length > 0) {
                    routeMethods.setCharAt(routeMethods.length() - 2, '}');
                    routeMethods.setLength(routeMethods.length() - 1);
                } else {
                    routeMethods.setLength(0);
                }

                TypeMirror returnType = ((ExecutableElement) se).getReturnType();
                int returnTypeInt = -1;
                if (returnType.toString().equals("void")) {
                    returnTypeInt = RETURN_TYPE_VOID;
                } else if (String.class.getName().equals(returnType.toString())) {
                    returnTypeInt = RETURN_TYPE_STRING;
                } else if ("byte[]".equals(returnType.toString())) {
                    returnTypeInt = RETURN_TYPE_BYTE_ARRAY;
                } else {
                    returnTypeInt = RETURN_TYPE_OBJECT;
                }

//                printWriter.println("\t\tif (applicationContext.getOptions().devMode()) {");
                printWriter.println("\t\tprintRouter(\"" + requestURL + "\", \"" + element.getSimpleName() + "\", \"" + se.getSimpleName() + "\");");
//                printWriter.println("\t\t}");
                if (!requestURL.contains(":") && !requestURL.contains("*") && requestURL.length() < ByteTree.MAX_DEPTH) {
                    printWriter.println("\t\tapplicationContext.getOptions().getUriByteTree().addNode(\"" + requestURL + "\");");
                }
                boolean async = returnTypeInt == RETURN_TYPE_OBJECT && AsyncResponse.class.getName().equals(returnType.toString());
                if (async) {
                    printWriter.println("\t\trouter.route(\"" + requestURL + "\"" + routeMethods + ", new " + RouterHandler.class.getName() + "()  {");
                    printWriter.println("\t\t\t@Override");
                    printWriter.println("\t\t\tpublic void handle(" + Context.class.getName() + " ctx) throws Throwable {");
                    printWriter.println();
                    printWriter.println("\t\t\t}");
                    printWriter.println();
                    printWriter.println("\t\t\t@Override");
                    printWriter.println("\t\t\tpublic void handle(" + Context.class.getName() + " ctx, " + CompletableFuture.class.getName() + "<Void> completableFuture) throws Throwable {");
                } else {
                    printWriter.println("\t\trouter.route(\"" + requestURL + "\"" + routeMethods + ", new RouterHandler() {");
                    printWriter.println("\t\t\t@Override");
                    printWriter.println("\t\t\tpublic void handle(" + Context.class.getName() + " ctx) throws Throwable {");
                }

                boolean hasBody = false;

                boolean first = true;
                StringBuilder newParams = new StringBuilder();
                StringBuilder params = new StringBuilder();
                int i = 0;
                for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                    if (first) {
                        first = false;
                    } else {
                        params.append(", ");
                    }
                    if (param.asType().toString().equals(HttpRequest.class.getName())) {
                        params.append("ctx.Request");
                    } else if (param.asType().toString().equals(HttpResponse.class.getName())) {
                        params.append("ctx.Response");
                    } else if (param.asType().toString().equals(Session.class.getName())) {
                        params.append("ctx.session()");
                    } else if (param.asType().toString().equals(Context.class.getName())) {
                        params.append("ctx");
                    } else if (param.getAnnotation(PathParam.class) != null) {
                        PathParam pathParam = param.getAnnotation(PathParam.class);
                        params.append("ctx.pathParam(\"").append(pathParam.value()).append("\")");
                    } else {
                        if (i == 0) {
                            hasBody = true;
                            newParams.append("\t\t\t\tJSONObject jsonObject = getParams(ctx.Request);\n");
                        }
                        Param paramAnnotation = param.getAnnotation(Param.class);
                        if (paramAnnotation == null && param.asType().toString().startsWith("java")) {
                            throw new FeatException("the param of " + element.getSimpleName() + "@" + se.getSimpleName() + " is not allowed to be empty.");
                        }
                        newParams.append("\t\t\t\t");
                        if (paramAnnotation != null) {
                            if (param.asType().toString().startsWith(List.class.getName())) {
                                newParams.append(param.asType().toString()).append(" param").append(i).append(" = jsonObject.getObject(\"").append(paramAnnotation.value()).append("\", java.util" + ".List.class);");
                            } else {
                                newParams.append(param.asType().toString()).append(" param").append(i).append(" = jsonObject.getObject(\"").append(paramAnnotation.value()).append("\", ").append(param.asType().toString()).append(".class);");
                            }
                        } else {
                            newParams.append(param.asType().toString()).append(" param").append(i).append(" = jsonObject.to(").append(param.asType().toString()).append(".class);");
                        }
//                                    newParams.append(param.asType().toString()).append(" param").append(i).append("=jsonObject.getObject(").append(param.asType().toString()).append(".class);");
                        params.append("param").append(i);
                        i++;
                    }
//                                printWriter.println("req.getParam(\"" + param.getSimpleName() + "\")");
                }
                if (newParams.length() > 0) {
                    printWriter.println(newParams);
                }

                switch (returnTypeInt) {
                    case RETURN_TYPE_VOID:
                        printWriter.print("\t\t\t\tbean." + se.getSimpleName() + "(");
                        break;
                    case RETURN_TYPE_STRING:
                        printWriter.print("\t\t\t\tString rst = bean." + se.getSimpleName() + "(");
                        break;
                    case RETURN_TYPE_BYTE_ARRAY:
                        printWriter.print("\t\t\t\tbyte[] bytes = bean." + se.getSimpleName() + "(");
                        break;
                    case RETURN_TYPE_OBJECT:
                        printWriter.print("\t\t\t\t" + returnType + " rst = bean." + se.getSimpleName() + "(");
                        break;
                    default:
                        throw new RuntimeException("不支持的返回类型");
                }
                printWriter.append(params).println(");");
                boolean gzip = controller.gzip();
                int gzipThreshold = controller.gzipThreshold();
                if (Boolean.parseBoolean(getFeatYamlValue("$.server.gzip"))) {
                    gzip = true;
                    gzipThreshold = FeatUtils.toInt(getFeatYamlValue("$.server.gzipThreshold"), gzipThreshold);
                }
                switch (returnTypeInt) {
                    case RETURN_TYPE_VOID:
                        break;
                    case RETURN_TYPE_STRING:
                        printWriter.println("\t\t\t\tbyte[] bytes = rst.getBytes(\"UTF-8\"); ");
                    case RETURN_TYPE_BYTE_ARRAY:
                        if (gzip) {
                            printWriter.println("\t\t\t\tif(bytes.length > " + gzipThreshold + ") {");
                            printWriter.println("\t\t\t\t\tbytes = " + FeatUtils.class.getName() + ".gzip(bytes);");
                            printWriter.println("\t\t\t\t\tctx.Response.setHeader(\"Content-Encoding\", \"gzip\");");
                            printWriter.println("\t\t\t\t}");
                        }
                        printWriter.println("\t\t\t\tctx.Response.setContentLength(bytes.length);");
                        printWriter.println("\t\t\t\tctx.Response.write(bytes);");
                        break;
                    case RETURN_TYPE_OBJECT:
                        if (AsyncResponse.class.getName().equals(returnType.toString())) {
                            if (gzip) {
                                printWriter.println("\t\t\t\tgzipResponse(rst, ctx, completableFuture, " + gzipThreshold + ");");
                            } else {
                                printWriter.println("\t\t\t\tresponse(rst, ctx, completableFuture);");
                            }
                        } else if (int.class.getName().equals(returnType.toString())) {
                            printWriter.println("\t\t\t\twriteInt(ctx.Response.getOutputStream(), rst);");
                        } else if (boolean.class.getName().equals(returnType.toString())) {
                            printWriter.println("\t\t\t\tctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);");
                            printWriter.println("\t\t\t\tif (rst) {");
                            printWriter.println("\t\t\t\tctx.Response.setContentLength(4);");
                            printWriter.println("\t\t\t\t} else {");
                            printWriter.println("\t\t\t\tctx.Response.setContentLength(5);");
                            printWriter.println("\t\t\t\t}");
                            printWriter.println("\t\t\t\twriteBool(ctx.Response.getOutputStream(), rst);");
                        } else {
                            printWriter.println("\t\t\t\tjava.io.ByteArrayOutputStream os = getOutputStream();");
                            JsonSerializer jsonSerializer = new JsonSerializer(printWriter);
                            jsonSerializer.serialize(returnType, "rst", 0, null);
                            bytesCache.putAll(jsonSerializer.getByteCache());
                            printWriter.println("\t\t\t\tctx.Response.setContentType(HeaderValue.ContentType.APPLICATION_JSON);");
                            if (gzip) {
                                printWriter.println("\t\t\t\tbyte[] bytes = os.toByteArray();");
                                printWriter.println("\t\t\t\tif (bytes.length > " + gzipThreshold + ") {");
                                printWriter.println("\t\t\t\t\tbytes = " + FeatUtils.class.getName() + ".gzip(bytes);");
                                printWriter.println("\t\t\t\t\tctx.Response.setHeader(\"Content-Encoding\", \"gzip\");");
                                printWriter.println("\t\t\t\t}");
                                printWriter.println("\t\t\t\tctx.Response.setContentLength(bytes.length);");
                                printWriter.println("\t\t\t\tctx.Response.write(bytes);");
                            } else {
                                printWriter.println("\t\t\t\tctx.Response.setContentLength(os.size());");
                                printWriter.println("\t\t\t\tos.writeTo(ctx.Response.getOutputStream());");
                            }
                        }

//                            System.out.println("typeMirror:" + stringBuilder);
//                            printWriter.println("        byte[] bytes=JSON.toJSONBytes(rst); ");
//                            printWriter.println("        ctx.Response.setContentLength(bytes.length);");
//                            printWriter.println("        ctx.Response.write(bytes);");
                        break;
//                        case RETURN_TYPE_OBJECT:
//                            writeJsonObject(writer,returnType);
//                            printWriter.println("        byte[] bytes=JSON.toJSONBytes(rst); ");
//                            printWriter.println("        ctx.Response.setContentLength(bytes.length);");
//                            printWriter.println("        ctx.Response.write(bytes);");
//                            break;
                    default:
                        throw new RuntimeException("不支持的返回类型");
                }
                printWriter.println("\t\t\t}");

                if (hasBody) {
                    printWriter.println("\t\t\t@Override");
                    printWriter.println("\t\t\tpublic void onHeaderComplete(HttpEndpoint request) throws IOException {");
                    printWriter.println("\t\t\t\tbodyAsyncRead(request);");
                    printWriter.println("\t\t\t}");
                }
                printWriter.println("\t\t});");
            }
        }

        if (mcpServerOption.enable) {
            mcpEndpointSerializer.serializeRouter();
        }
        addInterceptor();
    }

    private void addInterceptor() {
        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (!InterceptorMapping.class.getName().equals(mirror.getAnnotationType().toString())) {
                    continue;
                }
                String patterns = "";

                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                    ExecutableElement k = entry.getKey();
                    AnnotationValue v = entry.getValue();
                    if ("value".equals(k.getSimpleName().toString())) {
                        patterns = v.getValue().toString();
                    }
                }
                printWriter.append("\t\trouter.addInterceptors(java.util.Arrays.asList(").append(patterns).append(")").append(", bean." + se.getSimpleName() + "()");
                printWriter.println(");");
            }
        }
    }

    @Override
    public void serializeBytePool() {
        if (!bytesCache.isEmpty()) {
            for (Map.Entry<String, String> entry : bytesCache.entrySet()) {
                printWriter.println("\t" + entry.getValue());
            }
        }
    }
}
