/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *   without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer.doc;

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
            endpoint.setPath(fullPath);
            endpoint.setDescription(controllerElement.getSimpleName().toString() + "." + methodElement.getSimpleName().toString());

            // 处理 HTTP 方法
            for (tech.smartboot.feat.cloud.annotation.RequestMethod rm : requestMapping.method()) {
                endpoint.getMethods().add(rm.name().toLowerCase());
            }
            if (endpoint.getMethods().isEmpty()) {
                endpoint.getMethods().add("get");
            }

            // 处理参数
            for (VariableElement param : ((ExecutableElement) methodElement).getParameters()) {
                ApiParameter parameter = new ApiParameter();

                PathParam pathParam = param.getAnnotation(PathParam.class);
                Param paramAnnotation = param.getAnnotation(Param.class);

                if (pathParam != null) {
                    parameter.setName(pathParam.value());
                    parameter.setIn("path");
                    parameter.setRequired(true);
                } else if (paramAnnotation != null) {
                    parameter.setName(FeatUtils.isBlank(paramAnnotation.value())
                            ? param.getSimpleName().toString()
                            : paramAnnotation.value());
                    parameter.setIn("query");
                    parameter.setRequired(false);
                } else {
                    parameter.setName(param.getSimpleName().toString());
                    parameter.setIn("body");
                    parameter.setRequired(false);
                }

                parameter.setType(mapToOpenApiType(param.asType().toString()));
                endpoint.getParameters().add(parameter);
            }

            // 处理返回类型
            String returnType = ((ExecutableElement) methodElement).getReturnType().toString();
            endpoint.setResponseType(returnType);

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

    private String generateOpenApiJson(String title, String version, License license) {
        OpenApiDoc doc = new OpenApiDoc();
        doc.setOpenapi("3.0.0");

        // 设置 info
        Info info = new Info();
        info.setTitle(title);
        info.setVersion(version);
        info.setDescription("Auto-generated API documentation");

        // 设置授权信息
        if (license != null) {
            LicenseInfo licenseInfo = new LicenseInfo();
            licenseInfo.setName(license.getName());
            licenseInfo.setNum(license.getNum());
            licenseInfo.setUrl("https://smartboot.tech/feat/license/?num=" + license.getNum());
            info.setLicense(licenseInfo);
        } else {
            LicenseInfo licenseInfo = new LicenseInfo();
            licenseInfo.setName("Apache-2.0");
            licenseInfo.setUrl("https://www.apache.org/licenses/LICENSE-2.0");
            info.setLicense(licenseInfo);
        }
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

                    // 设置参数
                    operation.setParameters(new ArrayList<>());
                    for (ApiParameter param : endpoint.getParameters()) {
                        Parameter parameter = new Parameter();
                        parameter.setName(param.getName());
                        parameter.setIn(param.getIn());
                        parameter.setRequired(param.isRequired());
                        parameter.setSchema(new Schema());
                        parameter.getSchema().setType(param.getType());
                        operation.getParameters().add(parameter);
                    }

                    // 设置响应
                    operation.setResponses(new LinkedHashMap<>());
                    Response response = new Response();
                    response.setDescription("Successful response");

                    if (!endpoint.getResponseType().equals("void")) {
                        response.setContent(new Content());
                        response.getContent().setApplicationJson(new MediaType());
                        response.getContent().getApplicationJson().setSchema(new Schema());
                        response.getContent().getApplicationJson().getSchema().setType(mapToOpenApiType(endpoint.getResponseType()));
                    }

                    operation.getResponses().put("200", response);

                    // 根据 HTTP 方法设置对应的操作
                    switch (method.toLowerCase()) {
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
                        default:
                            pathItem.setGet(operation);
                    }
                }
            }
        }

        // 设置 components/schemas
        doc.setComponents(new Components());
        doc.getComponents().setSchemas(new LinkedHashMap<>());
        for (String schema : schemas) {
            Schema schemaObj = new Schema();
            schemaObj.setType("object");
            doc.getComponents().getSchemas().put(schema.substring(schema.lastIndexOf('.') + 1), schemaObj);
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
            String openApiJson = generateOpenApiJson(title, version, license);

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
}