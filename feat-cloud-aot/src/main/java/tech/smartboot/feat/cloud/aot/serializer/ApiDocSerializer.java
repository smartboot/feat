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

import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.aot.Serializer;
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

    public ApiDocSerializer(ProcessingEnvironment processingEnv, String config) {
        this.processingEnv = processingEnv;
        this.config = config;
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
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"openapi\": \"3.0.0\",\n");
        sb.append("  \"info\": {\n");
        sb.append("    \"title\": \"").append(escapeJson(title)).append("\",\n");
        sb.append("    \"version\": \"").append(escapeJson(version)).append("\",\n");
        sb.append("    \"description\": \"Auto-generated API documentation\"\n");
        sb.append("  },\n");
        sb.append("  \"paths\": {\n");

        // 按路径分组
        Map<String, List<ApiEndpoint>> pathMap = new LinkedHashMap<>();
        for (ApiEndpoint endpoint : endpoints) {
            pathMap.computeIfAbsent(endpoint.path, k -> new ArrayList<>()).add(endpoint);
        }

        boolean firstPath = true;
        for (Map.Entry<String, List<ApiEndpoint>> entry : pathMap.entrySet()) {
            if (!firstPath) {
                sb.append(",\n");
            }
            firstPath = false;

            sb.append("    \"").append(escapeJson(entry.getKey())).append("\": {\n");

            boolean firstMethod = true;
            for (ApiEndpoint endpoint : entry.getValue()) {
                if (!firstMethod) {
                    sb.append(",\n");
                }
                firstMethod = false;

                for (String method : endpoint.methods) {
                    sb.append("      \"").append(method).append("\": {\n");
                    sb.append("        \"summary\": \"").append(escapeJson(endpoint.description)).append("\",\n");
                    sb.append("        \"parameters\": [");

                    boolean firstParam = true;
                    for (ApiParameter param : endpoint.parameters) {
                        if (!firstParam) {
                            sb.append(", ");
                        }
                        firstParam = false;
                        sb.append("{");
                        sb.append("\"name\": \"").append(escapeJson(param.name)).append("\", ");
                        sb.append("\"in\": \"").append(param.in).append("\", ");
                        sb.append("\"required\": ").append(param.required).append(", ");
                        sb.append("\"schema\": { \"type\": \"").append(param.type).append("\" }");
                        sb.append("}");
                    }
                    sb.append("],\n");

                    sb.append("        \"responses\": {\n");
                    sb.append("          \"200\": {\n");
                    sb.append("            \"description\": \"Successful response\",\n");
                    if (!endpoint.responseType.equals("void")) {
                        sb.append("            \"content\": {\n");
                        sb.append("              \"application/json\": {\n");
                        sb.append("                \"schema\": { \"type\": \"")
                                .append(mapToOpenApiType(endpoint.responseType)).append("\" }\n");
                        sb.append("              }\n");
                        sb.append("            }\n");
                        sb.append("          }\n");
                    }
                    sb.append("        }\n");
                    sb.append("      }");
                }
            }
            sb.append("\n    }");
        }

        sb.append("\n  },\n");

        // 添加 components/schemas
        sb.append("  \"components\": {\n");
        sb.append("    \"schemas\": {\n");
        boolean firstSchema = true;
        for (String schema : schemas) {
            if (!firstSchema) {
                sb.append(",\n");
            }
            firstSchema = false;
            sb.append("      \"").append(escapeJson(schema.substring(schema.lastIndexOf('.') + 1))).append("\": {\n");
            sb.append("        \"type\": \"object\"\n");
            sb.append("      }");
        }
        sb.append("\n    }\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 生成 OpenAPI 文档
     */
    public void generateOpenApiDoc() {
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

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

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
