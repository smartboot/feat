/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *   without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer.openapi;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.aot.license.License;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.router.Context;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAPI 3.0 文档序列化器
 * <p>
 * 编译时收集所有 Controller 的 API 信息，生成符合 OpenAPI 3.0 规范的 JSON 文档。
 *
 * @author 三刀 zhengjunweimail@163.com
 */
public final class ApiDocSerializer {

    private final ProcessingEnvironment processingEnv;
    private final List<ApiEndpoint> endpoints = new ArrayList<>();
    private final Set<String> schemaTypes = new HashSet<>();
    private static final Pattern GENERIC_TYPE_PATTERN = Pattern.compile("([^<>]+)<(.+)>");

    public ApiDocSerializer(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }


    public void addController(Element controllerElement) {
        Controller controller = controllerElement.getAnnotation(Controller.class);
        String basePath = controller.value();
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        if (basePath.endsWith("/") && basePath.length() > 1) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        String controllerName = controllerElement.getSimpleName().toString();

        for (Element methodElement : controllerElement.getEnclosedElements()) {
            if (!(methodElement instanceof ExecutableElement)) {
                continue;
            }

            RequestMapping requestMapping = methodElement.getAnnotation(RequestMapping.class);
            if (requestMapping == null) {
                continue;
            }

            String methodPath = requestMapping.value();
            String fullPath = buildFullPath(basePath, methodPath);

            ApiEndpoint endpoint = new ApiEndpoint();
            endpoint.setPath(fullPath);
            endpoint.setDescription(buildDescription(controllerElement, methodElement));
            endpoint.setOperationId(buildOperationId(controllerName, methodElement.getSimpleName().toString()));

            // 处理 HTTP 方法
            for (tech.smartboot.feat.cloud.annotation.RequestMethod rm : requestMapping.method()) {
                endpoint.getMethods().add(rm.name().toLowerCase());
            }
            if (endpoint.getMethods().isEmpty()) {
                endpoint.getMethods().add("get");
            }

            // 处理参数
            ExecutableElement executableElement = (ExecutableElement) methodElement;
            handleParameters(endpoint, executableElement);

            // 处理返回类型
            TypeMirror returnType = executableElement.getReturnType();
            endpoint.setResponseType(returnType.toString());
            collectSchemaTypes(returnType);

            endpoints.add(endpoint);
        }
    }

    private String buildFullPath(String basePath, String methodPath) {
        if (FeatUtils.isBlank(methodPath)) {
            return basePath;
        }

        String path = basePath;
        if (!methodPath.startsWith("/")) {
            path += "/";
        }
        path += methodPath;

        // 移除末尾的斜杠（如果不是根路径）
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        // 保持 OpenAPI 规范的路径参数格式 {param}
        return path;
    }

    private String buildDescription(Element controllerElement, Element methodElement) {
        return controllerElement.getSimpleName().toString() + "." + methodElement.getSimpleName().toString();
    }

    private String buildOperationId(String controllerName, String methodName) {
        // 移除 Controller 后缀，转换为驼峰式
        String prefix = controllerName;
        if (prefix.endsWith("Controller")) {
            prefix = prefix.substring(0, prefix.length() - 10);
        }
        return prefix + methodName;
    }

    private void handleParameters(ApiEndpoint endpoint, ExecutableElement executableElement) {
        Types typeUtils = processingEnv.getTypeUtils();
        Elements elementUtils = processingEnv.getElementUtils();

        boolean hasRequestBody = false;

        for (VariableElement param : executableElement.getParameters()) {
            TypeMirror paramType = param.asType();
            String paramTypeName = paramType.toString();

            // 跳过特殊类型参数
            if (isSpecialType(paramTypeName)) {
                continue;
            }

            PathParam pathParam = param.getAnnotation(PathParam.class);
            Param paramAnnotation = param.getAnnotation(Param.class);

            if (pathParam != null) {
                // 路径参数
                ApiParameter parameter = new ApiParameter();
                parameter.setName(pathParam.value());
                parameter.setIn("path");
                parameter.setRequired(true);
                parameter.setType(mapToOpenApiType(paramType));
                parameter.setFormat(mapToOpenApiFormat(paramType));
                endpoint.getParameters().add(parameter);
            } else if (paramAnnotation != null) {
                // 查询参数
                ApiParameter parameter = new ApiParameter();
                String paramName = FeatUtils.isBlank(paramAnnotation.value())
                        ? param.getSimpleName().toString()
                        : paramAnnotation.value();
                parameter.setName(paramName);
                parameter.setIn("query");
                parameter.setRequired(false);
                parameter.setType(mapToOpenApiType(paramType));
                parameter.setFormat(mapToOpenApiFormat(paramType));
                endpoint.getParameters().add(parameter);
                collectSchemaTypes(paramType);
            } else if (!paramTypeName.startsWith("java.")) {
                // 请求体参数（自定义类型）
                if (!hasRequestBody) {
                    ApiParameter parameter = new ApiParameter();
                    parameter.setName(param.getSimpleName().toString());
                    parameter.setIn("body");
                    parameter.setRequired(true);
                    parameter.setType("object");
                    parameter.setRefSchema(extractSimpleClassName(paramTypeName));
                    endpoint.getParameters().add(parameter);
                    hasRequestBody = true;
                }
                collectSchemaTypes(paramType);
            }
        }
    }

    private boolean isSpecialType(String typeName) {
        return typeName.equals(HttpRequest.class.getName())
                || typeName.equals(HttpResponse.class.getName())
                || typeName.equals(Session.class.getName())
                || typeName.equals(Context.class.getName());
    }

    private void collectSchemaTypes(TypeMirror type) {
        if (type == null) {
            return;
        }

        String typeName = type.toString();

        // 跳过 Java 标准库类型
        if (typeName.startsWith("java.lang.") || typeName.startsWith("java.util.")) {
            // 但需要处理泛型参数中的自定义类型
            if (type instanceof DeclaredType) {
                DeclaredType declaredType = (DeclaredType) type;
                for (TypeMirror typeArg : declaredType.getTypeArguments()) {
                    collectSchemaTypes(typeArg);
                }
            }
            return;
        }

        // 跳过基本类型和 void
        if (type.getKind().isPrimitive() || typeName.equals("void")) {
            return;
        }

        schemaTypes.add(typeName);
    }

    private String mapToOpenApiType(TypeMirror type) {
        String typeName = type.toString();

        if (type.getKind().isPrimitive()) {
            switch (type.getKind()) {
                case INT:
                case LONG:
                case SHORT:
                case BYTE:
                    return "integer";
                case DOUBLE:
                case FLOAT:
                    return "number";
                case BOOLEAN:
                    return "boolean";
                case CHAR:
                    return "string";
                default:
                    return "string";
            }
        }

        if (typeName.equals("java.lang.Integer") || typeName.equals("java.lang.Long")
                || typeName.equals("java.lang.Short") || typeName.equals("java.lang.Byte")) {
            return "integer";
        }
        if (typeName.equals("java.lang.Double") || typeName.equals("java.lang.Float")) {
            return "number";
        }
        if (typeName.equals("java.lang.Boolean")) {
            return "boolean";
        }
        if (typeName.equals("java.lang.String") || typeName.equals("java.lang.Character")) {
            return "string";
        }
        if (typeName.startsWith("java.util.List")) {
            return "array";
        }
        if (typeName.startsWith("java.util.Map")) {
            return "object";
        }
        if (typeName.startsWith("java.util.Set")) {
            return "array";
        }
        return "object";
    }

    private String mapToOpenApiFormat(TypeMirror type) {
        String typeName = type.toString();

        if (type.getKind().isPrimitive()) {
            switch (type.getKind()) {
                case INT:
                    return "int32";
                case LONG:
                    return "int64";
                case SHORT:
                    return "int32";
                case BYTE:
                    return "int32";
                case DOUBLE:
                    return "double";
                case FLOAT:
                    return "float";
                default:
                    return null;
            }
        }

        if (typeName.equals("java.lang.Integer") || typeName.equals("java.lang.Short")) {
            return "int32";
        }
        if (typeName.equals("java.lang.Long")) {
            return "int64";
        }
        if (typeName.equals("java.lang.Double")) {
            return "double";
        }
        if (typeName.equals("java.lang.Float")) {
            return "float";
        }
        if (typeName.equals("java.lang.Byte")) {
            return "int32";
        }

        return null;
    }

    private String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null || !fullClassName.contains(".")) {
            return fullClassName;
        }
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    private String extractGenericType(String typeName) {
        Matcher matcher = GENERIC_TYPE_PATTERN.matcher(typeName);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    private RequestBody buildRequestBody(String refSchema) {
        RequestBody requestBody = new RequestBody();
        requestBody.setRequired(true);

        Content content = new Content();
        MediaType mediaType = new MediaType();

        Schema schema = new Schema();
        schema.setRef("#/components/schemas/" + refSchema);
        mediaType.setSchema(schema);
        content.setApplicationJson(mediaType);
        requestBody.setContent(content);

        return requestBody;
    }

    private Content buildContent(String responseType) {
        Content content = new Content();
        MediaType mediaType = new MediaType();
        Schema schema = new Schema();

        if (responseType.startsWith("java.util.List") || responseType.startsWith("java.util.Set")) {
            schema.setType("array");
            String itemType = extractGenericType(responseType);
            if (itemType != null) {
                Schema itemsSchema = new Schema();
                if (isSimpleType(itemType)) {
                    itemsSchema.setType(mapToOpenApiTypeByName(itemType));
                    itemsSchema.setFormat(mapToOpenApiFormatByName(itemType));
                } else {
                    itemsSchema.setRef("#/components/schemas/" + extractSimpleClassName(itemType));
                }
                schema.setItems(itemsSchema);
            }
        } else if (responseType.startsWith("java.util.Map")) {
            schema.setType("object");
            // Map 类型简化处理
        } else if (isSimpleType(responseType)) {
            schema.setType(mapToOpenApiTypeByName(responseType));
            schema.setFormat(mapToOpenApiFormatByName(responseType));
        } else {
            schema.setRef("#/components/schemas/" + extractSimpleClassName(responseType));
        }

        mediaType.setSchema(schema);
        content.setApplicationJson(mediaType);

        return content;
    }

    private boolean isSimpleType(String typeName) {
        return typeName.startsWith("java.lang.") || typeName.startsWith("int")
                || typeName.startsWith("long") || typeName.startsWith("boolean")
                || typeName.startsWith("double") || typeName.startsWith("float")
                || typeName.startsWith("short") || typeName.startsWith("byte")
                || typeName.startsWith("char");
    }

    private String mapToOpenApiTypeByName(String typeName) {
        if (typeName.equals("int") || typeName.equals("java.lang.Integer")
                || typeName.equals("long") || typeName.equals("java.lang.Long")
                || typeName.equals("short") || typeName.equals("java.lang.Short")
                || typeName.equals("byte") || typeName.equals("java.lang.Byte")) {
            return "integer";
        }
        if (typeName.equals("double") || typeName.equals("java.lang.Double")
                || typeName.equals("float") || typeName.equals("java.lang.Float")) {
            return "number";
        }
        if (typeName.equals("boolean") || typeName.equals("java.lang.Boolean")) {
            return "boolean";
        }
        if (typeName.equals("java.lang.String") || typeName.equals("char")
                || typeName.equals("java.lang.Character")) {
            return "string";
        }
        return "object";
    }

    private String mapToOpenApiFormatByName(String typeName) {
        if (typeName.equals("int") || typeName.equals("java.lang.Integer")
                || typeName.equals("short") || typeName.equals("java.lang.Short")) {
            return "int32";
        }
        if (typeName.equals("long") || typeName.equals("java.lang.Long")) {
            return "int64";
        }
        if (typeName.equals("double") || typeName.equals("java.lang.Double")) {
            return "double";
        }
        if (typeName.equals("float") || typeName.equals("java.lang.Float")) {
            return "float";
        }
        return null;
    }

    private void setOperationByMethod(PathItem pathItem, String method, Operation operation) {
        switch (method) {
            case "get":
                pathItem.setGet(operation);
                break;
            case "post":
                pathItem.setPost(operation);
                break;
            case "put":
                pathItem.setPut(operation);
                break;
            case "delete":
                pathItem.setDelete(operation);
                break;
            case "patch":
                pathItem.setPatch(operation);
                break;
            case "options":
                pathItem.setOptions(operation);
                break;
            case "head":
                pathItem.setHead(operation);
                break;
            case "trace":
                pathItem.setTrace(operation);
                break;
            default:
                pathItem.setGet(operation);
        }
    }

    /**
     * 生成 OpenAPI 文档
     */
    public void generateOpenApiDoc(License license) throws IOException {
        if (license == null) {
            return;
        }
        // 生成 OpenAPI JSON
        OpenApiDoc doc = new OpenApiDoc();
        doc.setOpenapi("3.0.3");

        // 设置 info
        Info info = new Info();
        info.setTitle("Feat API");
        info.setVersion("1.0.0");
        info.setDescription("Auto-generated API documentation for Feat framework");

        // 设置授权信息（遵循 OpenAPI 3.0 规范）
        LicenseInfo licenseInfo = new LicenseInfo();
        licenseInfo.setName(license.getName());
        licenseInfo.setUrl("https://smartboot.tech/feat/sponsors/");
        info.setLicense(licenseInfo);
        doc.setInfo(info);

        // 按路径分组构建 paths
        doc.setPaths(new LinkedHashMap<>());
        Map<String, List<ApiEndpoint>> pathMap = new LinkedHashMap<>();
        for (ApiEndpoint endpoint : endpoints) {
            pathMap.computeIfAbsent(endpoint.getPath(), k -> new ArrayList<>()).add(endpoint);
        }

        for (Map.Entry<String, List<ApiEndpoint>> entry : pathMap.entrySet()) {
            PathItem pathItem = new PathItem();
            doc.getPaths().put(entry.getKey(), pathItem);

            for (ApiEndpoint endpoint : entry.getValue()) {
                for (String method : endpoint.getMethods()) {
                    Operation operation = new Operation();
                    operation.setSummary(endpoint.getDescription());
                    operation.setOperationId(endpoint.getOperationId());

                    // 设置参数
                    operation.setParameters(new ArrayList<>());
                    for (ApiParameter param : endpoint.getParameters()) {
                        Parameter parameter = new Parameter();
                        parameter.setName(param.getName());
                        parameter.setIn(param.getIn());
                        parameter.setRequired(param.isRequired());
                        parameter.setDescription("");

                        if ("body".equals(param.getIn())) {
                            // 请求体使用 requestBody
                            operation.setRequestBody(buildRequestBody(param.getRefSchema()));
                        } else {
                            // 查询参数和路径参数使用 schema
                            Schema schema = new Schema();
                            schema.setType(param.getType());
                            if (param.getFormat() != null) {
                                schema.setFormat(param.getFormat());
                            }
                            parameter.setSchema(schema);
                            operation.getParameters().add(parameter);
                        }
                    }

                    // 设置响应
                    operation.setResponses(new LinkedHashMap<>());
                    Response response = new Response();
                    response.setDescription("Successful response");

                    if (!endpoint.getResponseType().equals("void")) {
                        response.setContent(buildContent(endpoint.getResponseType()));
                    }

                    operation.getResponses().put("200", response);

                    // 添加默认错误响应
                    Response errorResponse = new Response();
                    errorResponse.setDescription("Error response");
                    operation.getResponses().put("400", errorResponse);
                    operation.getResponses().put("500", errorResponse);

                    // 根据 HTTP 方法设置对应的操作
                    setOperationByMethod(pathItem, method.toLowerCase(), operation);
                }
            }
        }

        // 设置 components/schemas
        doc.setComponents(new Components());
        doc.getComponents().setSchemas(new LinkedHashMap<>());
        for (String schemaType : schemaTypes) {
            String schemaName = extractSimpleClassName(schemaType);
            Schema schema = new Schema();
            schema.setType("object");
            // 添加 $ref 支持的占位符
            doc.getComponents().getSchemas().put(schemaName, schema);
        }

        String openApiJson = JSON.toJSONString(doc, JSONWriter.Feature.PrettyFormat);

        // 写入文件
        FileObject apiDocFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/feat/openapi.json");
        try (PrintWriter docWriter = new PrintWriter(apiDocFile.openWriter())) {
            docWriter.print(openApiJson);
        }
        System.out.println("Generated OpenAPI documentation: " + apiDocFile.toUri());
    }
}