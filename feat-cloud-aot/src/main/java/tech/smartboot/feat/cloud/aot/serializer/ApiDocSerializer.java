/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *   without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.aot.Serializer;
import tech.smartboot.feat.cloud.aot.license.License;
import tech.smartboot.feat.core.common.FeatUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAPI 文档序列化器
 * <p>
 * 编译时收集所有 Controller 的 API 信息，生成 OpenAPI 3.0 规范的 JSON 文档。
 *
 * @author 三刀 zhengjunweimail@163.com
 */
public final class ApiDocSerializer implements Serializer {

    private final ProcessingEnvironment processingEnv;
    private final String config;
    private final List<ApiEndpoint> endpoints = new ArrayList<>();
    private final Set<String> schemas = new HashSet<>();
    private final License license;

    public ApiDocSerializer(ProcessingEnvironment processingEnv, String config, License license) {
        this.processingEnv = processingEnv;
        this.config = config;
        this.license = license;
    }

    @Override
    public String packageName() {
        return null;
    }

    @Override
    public String className() {
        return null;
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE; // 最后执行
    }

    @Override
    public void serializeLoadBean() {
        // 不需要生成 Bean
    }

    @Override
    public void serializeRouter() {
        // 不需要生成路由
    }

    public void addController(Element controllerElement) {
        Controller controller = controllerElement.getAnnotation(Controller.class);
        String basePath = controller.value();
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }

        for (Element methodElement : controllerElement.getEnclosedElements()) {
            RequestMapping requestMapping = methodElement.getAnnotation(RequestMapping.class);
            if (requestMapping == null) {
                continue;
            }

            String methodPath = requestMapping.value();
            String fullPath = basePath + methodPath;
            fullPath = fullPath.replace("/{", "/:").replace("}", "");

            if (fullPath.endsWith("/")) {
                fullPath = fullPath.substring(0, fullPath.length() - 1);
            }

            ApiEndpoint endpoint = new ApiEndpoint();
            endpoint.path = fullPath;
            endpoint.description = controllerElement.getSimpleName().toString() + "." + methodElement.getSimpleName().toString();

            // 处理 HTTP 方法
            for (tech.smartboot.feat.cloud.annotation.RequestMethod rm : requestMapping.method()) {
                endpoint.methods.add(rm.name().toLowerCase());
            }
            if (endpoint.methods.isEmpty()) {
                endpoint.methods.add("get");
            }

            // 处理参数
            for (VariableElement param : ((ExecutableElement) methodElement).getParameters()) {
                ApiParameter parameter = new ApiParameter();

                PathParam pathParam = param.getAnnotation(PathParam.class);
                Param paramAnnotation = param.getAnnotation(Param.class);

                if (pathParam != null) {
                    parameter.name = pathParam.value();
                    parameter.in = "path";
                    parameter.required = true;
                } else if (paramAnnotation != null) {
                    parameter.name = FeatUtils.isBlank(paramAnnotation.value())
                            ? param.getSimpleName().toString()
                            : paramAnnotation.value();
                    parameter.in = "query";
                    parameter.required = false;
                } else {
                    parameter.name = param.getSimpleName().toString();
                    parameter.in = "body";
                    parameter.required = false;
                }

                parameter.type = mapToOpenApiType(param.asType().toString());
                endpoint.parameters.add(parameter);
            }

            // 处理返回类型
            String returnType = ((ExecutableElement) methodElement).getReturnType().toString();
            endpoint.responseType = returnType;

            endpoints.add(endpoint);

            // 收集 schema
            collectSchema(returnType);
        }
    }

    private void collectSchema(String type) {
        if (type == null || type.startsWith("java.") || type.equals("void")) {
            return;
        }
        schemas.add(type);
    }

    private String mapToOpenApiType(String javaType) {
        if (javaType == null) {
            return "string";
        }
        if (javaType.equals("int") || javaType.equals("java.lang.Integer")) {
            return "integer";
        } else if (javaType.equals("long") || javaType.equals("java.lang.Long")) {
            return "integer";
        } else if (javaType.equals("double") || javaType.equals("java.lang.Double")) {
            return "number";
        } else if (javaType.equals("float") || javaType.equals("java.lang.Float")) {
            return "number";
        } else if (javaType.equals("boolean") || javaType.equals("java.lang.Boolean")) {
            return "boolean";
        } else if (javaType.equals("java.lang.String")) {
            return "string";
        } else if (javaType.startsWith("java.util.List")) {
            return "array";
        } else if (javaType.startsWith("java.util.Map")) {
            return "object";
        }
        return "object";
    }

    public String generateOpenApiJson(String title, String version) {
        OpenApiDoc doc = new OpenApiDoc();
        doc.openapi = "3.0.0";

        // 设置 info
        doc.info = new Info();
        doc.info.title = title;
        doc.info.version = version;
        doc.info.description = "Auto-generated API documentation";

        // 按路径分组构建 paths
        doc.paths = new LinkedHashMap<>();
        Map<String, List<ApiEndpoint>> pathMap = new LinkedHashMap<>();
        for (ApiEndpoint endpoint : endpoints) {
            pathMap.computeIfAbsent(endpoint.path, k -> new ArrayList<>()).add(endpoint);
        }

        for (Map.Entry<String, List<ApiEndpoint>> entry : pathMap.entrySet()) {
            PathItem pathItem = new PathItem();
            doc.paths.put(entry.getKey(), pathItem);

            for (ApiEndpoint endpoint : entry.getValue()) {
                for (String method : endpoint.methods) {
                    Operation operation = new Operation();
                    operation.summary = endpoint.description;

                    // 设置参数
                    operation.parameters = new ArrayList<>();
                    for (ApiParameter param : endpoint.parameters) {
                        Parameter parameter = new Parameter();
                        parameter.name = param.name;
                        parameter.in = param.in;
                        parameter.required = param.required;
                        parameter.schema = new Schema();
                        parameter.schema.type = param.type;
                        operation.parameters.add(parameter);
                    }

                    // 设置响应
                    operation.responses = new LinkedHashMap<>();
                    Response response = new Response();
                    response.description = "Successful response";

                    if (!endpoint.responseType.equals("void")) {
                        response.content = new Content();
                        response.content.applicationJson = new MediaType();
                        response.content.applicationJson.schema = new Schema();
                        response.content.applicationJson.schema.type = mapToOpenApiType(endpoint.responseType);
                    }

                    operation.responses.put("200", response);

                    // 根据 HTTP 方法设置对应的操作
                    switch (method.toLowerCase()) {
                        case "get":
                            pathItem.get = operation;
                            break;
                        case "post":
                            pathItem.post = operation;
                            break;
                        case "put":
                            pathItem.put = operation;
                            break;
                        case "delete":
                            pathItem.delete = operation;
                            break;
                        case "patch":
                            pathItem.patch = operation;
                            break;
                        case "options":
                            pathItem.options = operation;
                            break;
                        case "head":
                            pathItem.head = operation;
                            break;
                        default:
                            pathItem.get = operation;
                    }
                }
            }
        }

        // 设置 components/schemas
        doc.components = new Components();
        doc.components.schemas = new LinkedHashMap<>();
        for (String schema : schemas) {
            Schema schemaObj = new Schema();
            schemaObj.type = "object";
            doc.components.schemas.put(schema.substring(schema.lastIndexOf('.') + 1), schemaObj);
        }

        return JSON.toJSONString(doc, JSONWriter.Feature.PrettyFormat);
    }

    /**
     * 生成 OpenAPI 文档
     */
    public void generateOpenApiDoc(License license) {
        try {
            // 从配置中获取项目信息
            String title = "Feat API";
            String version = "1.0.0";
            try {
                Object titleVal = com.alibaba.fastjson2.JSONPath.eval(config, "$.server.apiDoc.title");
                Object versionVal = com.alibaba.fastjson2.JSONPath.eval(config, "$.server.apiDoc.version");
                if (titleVal != null) {
                    title = titleVal.toString();
                }
                if (versionVal != null) {
                    version = versionVal.toString();
                }
            } catch (Exception ignored) {
            }

            // 生成 OpenAPI JSON
            String openApiJson = generateOpenApiJson(title, version);

            // 写入文件
            FileObject apiDocFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/feat/openapi.json");
            PrintWriter docWriter = new PrintWriter(apiDocFile.openWriter());
            docWriter.print(openApiJson);
            docWriter.close();

            System.out.println("Generated OpenAPI documentation: " + apiDocFile.toUri());
        } catch (Throwable e) {
            System.err.println("Failed to generate OpenAPI documentation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== OpenAPI POJO 类 ====================

    static class OpenApiDoc {
        String openapi;
        Info info;
        Map<String, PathItem> paths;
        Components components;
    }

    static class Info {
        String title;
        String version;
        String description;
    }

    static class PathItem {
        Operation get;
        Operation post;
        Operation put;
        Operation delete;
        Operation patch;
        Operation options;
        Operation head;
    }

    static class Operation {
        String summary;
        List<Parameter> parameters;
        Map<String, Response> responses;
    }

    static class Parameter {
        String name;
        String in;
        boolean required;
        Schema schema;
    }

    static class Response {
        String description;
        Content content;
    }

    static class Content {
        @com.alibaba.fastjson2.annotation.JSONField(name = "application/json")
        MediaType applicationJson;
    }

    static class MediaType {
        Schema schema;
    }

    static class Components {
        Map<String, Schema> schemas;
    }

    static class Schema {
        String type;
    }

    // ==================== 原有数据结构 ====================

    static class ApiEndpoint {
        String path;
        String description;
        String responseType;
        List<String> methods = new ArrayList<>();
        List<ApiParameter> parameters = new ArrayList<>();
    }

    static class ApiParameter {
        String name;
        String in;
        String type;
        boolean required;
    }
}