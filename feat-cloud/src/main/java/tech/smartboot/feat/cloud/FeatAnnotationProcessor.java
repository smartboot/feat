/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import com.alibaba.fastjson2.JSONObject;
import org.apache.ibatis.annotations.Mapper;
import tech.smartboot.feat.ai.mcp.server.McpServer;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.InterceptorMapping;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.cloud.annotation.mcp.McpEndpoint;
import tech.smartboot.feat.cloud.mcp.McpServerSerializer;
import tech.smartboot.feat.cloud.serializer.JsonSerializer;
import tech.smartboot.feat.cloud.serializer.value.FeatYamlValueSerializer;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.utils.ByteTree;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.RouterHandler;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

// 该注解表示该处理器支持的 Java 源代码版本

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class FeatAnnotationProcessor extends AbstractProcessor {
    private static final int RETURN_TYPE_VOID = 0;
    private static final int RETURN_TYPE_STRING = 1;
    private static final int RETURN_TYPE_OBJECT = 2;
    private static final int RETURN_TYPE_BYTE_ARRAY = 3;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(Bean.class.getCanonicalName());
        types.add(Autowired.class.getCanonicalName());
        types.add(Controller.class.getCanonicalName());
        types.add(Mapper.class.getCanonicalName());
        types.add(McpEndpoint.class.getCanonicalName());
        return types;
    }

    FileObject serviceFile;
    PrintWriter serviceWrite;
    private boolean exception = false;
    private final List<String> services = new ArrayList<>();
    private FeatYamlValueSerializer yamlValueSerializer;

    private McpServerSerializer mcpServerSerializer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            serviceFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + CloudService.class.getName());
            serviceWrite = new PrintWriter(serviceFile.openWriter());
        } catch (IOException e) {
            throw new FeatException(e);
        }
        System.out.println("processor init: " + this);
        yamlValueSerializer = new FeatYamlValueSerializer(processingEnv, services);
        mcpServerSerializer = new McpServerSerializer(yamlValueSerializer);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        System.out.println("annotation process: " + this + " ,annotations: " + annotations);


        for (Element element : roundEnv.getElementsAnnotatedWith(Bean.class)) {
            Bean bean = element.getAnnotation(Bean.class);
            if (element.getKind() == ElementKind.CLASS) {
                try {
                    createAptLoader(element, bean);
                } catch (Throwable e) {
                    System.err.println("createAptLoader error: " + element.getSimpleName() + " " + e.getMessage());
                    exception = true;
                }
            }
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(Controller.class)) {
            Controller controller = element.getAnnotation(Controller.class);
            try {
                if (element.getAnnotation(McpEndpoint.class) != null) {
                    throw new FeatException("@Controller and @McpEndpoint cannot be used together!");
                }
                createAptLoader(element, controller);
            } catch (Throwable e) {
                System.err.println("createAptLoader error: " + element.getSimpleName() + " " + e.getMessage());
                exception = true;
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(McpEndpoint.class)) {
            McpEndpoint mcpEndpoint = element.getAnnotation(McpEndpoint.class);
            try {
                createAptLoader(element, mcpEndpoint);
            } catch (Throwable e) {
                System.err.println("createAptLoader error: " + element.getSimpleName() + " " + e.getMessage());
                exception = true;
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Mapper.class)) {
            Mapper mapper = element.getAnnotation(Mapper.class);
            try {
                createAptLoader(element, mapper);
            } catch (Throwable e) {
                System.err.println("createAptLoader error: " + element.getSimpleName() + " " + e.getMessage());
                exception = true;
            }
        }
        // 如果不希望后续的处理器继续处理这些注解，返回 true，否则返回 false
        for (String service : services) {
            serviceWrite.println(service);
        }
        serviceWrite.flush();

        if (exception || yamlValueSerializer.isException()) {
            throw new FeatException("编译失败！请根据提示修复错误，或者联系开发者：https://gitee.com/smartboot/feat/issues");
        }
        return false;
    }

    private <T extends Annotation> void createAptLoader(Element element, T annotation) throws IOException {
        String packageName = element.getEnclosingElement().toString();
        String loaderName = element.getSimpleName() + "BeanAptLoader";
        //生成service配置
        services.add(packageName + "." + loaderName);

        FileObject preFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, packageName, loaderName + ".java");
        File f = new File(preFileObject.toUri());
        if (f.exists()) {
            f.delete();
        }

        JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(packageName + "." + loaderName);
        Writer writer = javaFileObject.openWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println("package " + packageName + ";");
        printWriter.println();
        if (annotation instanceof McpEndpoint) {
            printWriter.println("import " + McpServer.class.getName() + ";");
        }
        printWriter.println("import " + AbstractServiceLoader.class.getName() + ";");
        printWriter.println("import " + ApplicationContext.class.getName() + ";");
        printWriter.println("import " + Router.class.getName() + ";");
        printWriter.println("import " + JSONObject.class.getName() + ";");
        printWriter.println("import com.alibaba.fastjson2.JSON;");
        printWriter.println();
        printWriter.println("public class " + loaderName + " extends " + AbstractServiceLoader.class.getSimpleName() + " {");
        printWriter.println();

        if (annotation instanceof Mapper) {
            printWriter.println("    private org.apache.ibatis.session.SqlSessionFactory factory;");
        }
        if (annotation instanceof McpEndpoint) {
            printWriter.println("    private McpServer mcpServer = new McpServer();");
            printWriter.println("    private " + element.getSimpleName() + " bean = new " + element.getSimpleName() + "();");
        } else {
            printWriter.println("    private " + element.getSimpleName() + " bean;");
        }
        if (annotation instanceof Bean) {
            printWriter.println();
            printWriter.println("\tpublic int order() {");
            printWriter.println("\t\treturn " + ((Bean) annotation).order() + ";");
            printWriter.println("\t}");
        }
        printWriter.println();

        printWriter.println("\tpublic void loadBean(ApplicationContext applicationContext) throws Throwable {");
        if (annotation instanceof Mapper) {
            createMapperBean(element, printWriter);
        } else if (!(annotation instanceof McpEndpoint)) {
            generateBeanOrController(element, printWriter);
            String beanName = element.getSimpleName().toString().substring(0, 1).toLowerCase() + element.getSimpleName().toString().substring(1);
            if (annotation instanceof Bean && !((Bean) annotation).value().isEmpty()) {
                beanName = ((Bean) annotation).value();
            }
            printWriter.println("\t\tapplicationContext.addBean(\"" + beanName + "\", bean);");
        }
        printWriter.println("\t}");

        printWriter.println();
        printWriter.println("\tpublic void autowired(ApplicationContext applicationContext) throws Throwable {");
        printWriter.append(generateAutoWriedMethod(element, annotation));
        printWriter.append(yamlValueSerializer.generateValueSetter(element));
        //注册MCP 服务
        mcpServerSerializer.serialize(processingEnv, printWriter, element);
        printWriter.println("\t}");
        printWriter.println();

        printWriter.println("\tpublic void router(ApplicationContext applicationContext, " + Router.class.getSimpleName() + " router) {");
        Map<String, String> bytesCache = new HashMap<>();
        if (annotation instanceof Controller) {
            createController(element, (Controller) annotation, printWriter, bytesCache);
        }
        //扫描拦截器
        addInterceptor(element, printWriter);

        printWriter.println("\t}");
        printWriter.println();
        if (!bytesCache.isEmpty()) {
            for (Map.Entry<String, String> entry : bytesCache.entrySet()) {
                printWriter.println("\t" + entry.getValue());
            }
        }
        printWriter.println();
        printWriter.println("\tpublic void destroy() throws Throwable {");
        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (PreDestroy.class.getName().equals(mirror.getAnnotationType().toString())) {
                    printWriter.println("\t\tbean." + se.getSimpleName() + "();");
                }
            }
        }
        printWriter.println("\t}");
        printWriter.println();
        printWriter.println("\tpublic void postConstruct(ApplicationContext applicationContext) throws Throwable {");
        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (PostConstruct.class.getName().equals(mirror.getAnnotationType().toString())) {
                    printWriter.println("\t\tbean." + se.getSimpleName() + "();");
                }
            }
        }
        printWriter.println("\t}");
        printWriter.println("}");
        writer.close();
    }

    private <T extends Annotation> void generateBeanOrController(Element element, PrintWriter printWriter) {
        printWriter.println("\t\tbean = new " + element.getSimpleName() + "(); ");
        //初始化通过方法实例化的bean
        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (!Bean.class.getName().equals(mirror.getAnnotationType().toString())) {
                    continue;
                }
                printWriter.print("\t\tapplicationContext.addBean(\"" + se.getSimpleName() + "\", bean." + se.getSimpleName() + "(");
                boolean first = true;
                for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                    if (first) {
                        first = false;
                    } else {
                        printWriter.print(", ");
                    }
                    printWriter.print("loadBean(\"" + param.getSimpleName() + "\", applicationContext)");
                }
                printWriter.println("));");
            }
        }
    }

    /**
     * 生成自动注入方法
     */
    private <T extends Annotation> String generateAutoWriedMethod(Element element, T annotation) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Element> autowiredFields = new ArrayList<>();
        for (Element field : element.getEnclosedElements()) {
            if (field.getAnnotation(Autowired.class) != null) {
                autowiredFields.add(field);
            }
        }
        for (Element field : autowiredFields) {
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
                stringBuilder.append("\t\tbean.set").append(name).append("(loadBean(\"").append(field.getSimpleName()).append("\", applicationContext));\n");
            } else {
                stringBuilder.append("\t\treflectAutowired(bean, \"").append(field.getSimpleName().toString()).append("\", applicationContext);\n");
            }
        }
        if (annotation instanceof Mapper) {
            stringBuilder.append("\t\tfactory = applicationContext.getBean(\"sessionFactory\");\n");
        }
        return stringBuilder.toString();
    }


    private <T extends Annotation> void createController(Element element, Controller annotation, PrintWriter printWriter, Map<String, String> bytesCache) throws IOException {
        Controller controller = annotation;
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

                printWriter.println("\t\tSystem.out.println(\" \\u001B[32m|->\\u001B[0m " + requestURL + " ==> " + element.getSimpleName() + "@" + se.getSimpleName() + "\");");
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
                    printWriter.println("\t\trouter.route(\"" + requestURL + "\"" + routeMethods + ", ctx -> {");
                }


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
                    } else if (param.getAnnotation(PathParam.class) != null) {
                        PathParam pathParam = param.getAnnotation(PathParam.class);
                        params.append("ctx.pathParam(\"" + pathParam.value() + "\")");
                    } else {
                        if (i == 0) {
                            newParams.append("\t\t\tJSONObject jsonObject = getParams(ctx.Request);\n");
                        }
                        Param paramAnnotation = param.getAnnotation(Param.class);
                        if (paramAnnotation == null && param.asType().toString().startsWith("java")) {
                            throw new FeatException("the param of " + element.getSimpleName() + "@" + se.getSimpleName() + " is not allowed to be empty.");
                        }
                        newParams.append("\t\t\t");
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
                        printWriter.print("\t\t\tbean." + se.getSimpleName() + "(");
                        break;
                    case RETURN_TYPE_STRING:
                        printWriter.print("\t\t\tString rst = bean." + se.getSimpleName() + "(");
                        break;
                    case RETURN_TYPE_BYTE_ARRAY:
                        printWriter.print("\t\t\tbyte[] bytes = bean." + se.getSimpleName() + "(");
                        break;
                    case RETURN_TYPE_OBJECT:
                        if (async) {
                            printWriter.print("\t");
                        }
                        printWriter.print("\t\t\t" + returnType + " rst = bean." + se.getSimpleName() + "(");
                        break;
                    default:
                        throw new RuntimeException("不支持的返回类型");
                }
                printWriter.append(params).println(");");
                boolean gzip = annotation.gzip();
                int gzipThreshold = annotation.gzipThreshold();
                if (Boolean.parseBoolean(yamlValueSerializer.getFeatYamlValue("$.server.gzip"))) {
                    gzip = true;
                    gzipThreshold = FeatUtils.toInt(yamlValueSerializer.getFeatYamlValue("$.server.gzipThreshold"), gzipThreshold);
                }
                switch (returnTypeInt) {
                    case RETURN_TYPE_VOID:
                        break;
                    case RETURN_TYPE_STRING:
                        printWriter.println("\t\t\tbyte[] bytes = rst.getBytes(\"UTF-8\"); ");
                    case RETURN_TYPE_BYTE_ARRAY:
                        if (gzip) {
                            printWriter.println("\t\t\tif(bytes.length > " + gzipThreshold + ") {");
                            printWriter.println("\t\t\t\tbytes = " + FeatUtils.class.getName() + ".gzip(bytes);");
                            printWriter.println("\t\t\t\tctx.Response.setHeader(\"Content-Encoding\", \"gzip\");");
                            printWriter.println("\t\t\t}");
                        }
                        printWriter.println("\t\t\tctx.Response.setContentLength(bytes.length);");
                        printWriter.println("\t\t\tctx.Response.write(bytes);");
                        break;
                    case RETURN_TYPE_OBJECT:
                        if (AsyncResponse.class.getName().equals(returnType.toString())) {
                            if (gzip) {
                                printWriter.println("\t\t\t\tgzipResponse(rst, ctx, completableFuture, " + gzipThreshold + ");");
                            } else {
                                printWriter.println("\t\t\t\tresponse(rst, ctx, completableFuture);");
                            }
                            printWriter.println("\t\t\t}");
                        } else if (int.class.getName().equals(returnType.toString())) {
                            printWriter.println("\t\t\twriteInt(ctx.Response.getOutputStream(), rst);");
                        } else if (boolean.class.getName().equals(returnType.toString())) {
                            printWriter.println("\t\t\tctx.Response.setContentType(\"application/json\");");
                            printWriter.println("\t\t\tif (rst) {");
                            printWriter.println("\t\t\tctx.Response.setContentLength(4);");
                            printWriter.println("\t\t\t} else {");
                            printWriter.println("\t\t\tctx.Response.setContentLength(5);");
                            printWriter.println("\t\t\t}");
                            printWriter.println("\t\t\twriteBool(ctx.Response.getOutputStream(), rst);");
                        } else {
                            printWriter.println("\t\t\tjava.io.ByteArrayOutputStream os = getOutputStream();");
                            JsonSerializer jsonSerializer = new JsonSerializer(printWriter);
                            jsonSerializer.serialize(returnType, "rst", 0, null);
                            bytesCache.putAll(jsonSerializer.getByteCache());
                            printWriter.println("\t\t\tctx.Response.setContentType(\"application/json\");");
                            if (gzip) {
                                printWriter.println("\t\t\tbyte[] bytes = os.toByteArray();");
                                printWriter.println("\t\t\tif (bytes.length > " + gzipThreshold + ") {");
                                printWriter.println("\t\t\t\tbytes = " + FeatUtils.class.getName() + ".gzip(bytes);");
                                printWriter.println("\t\t\t\tctx.Response.setHeader(\"Content-Encoding\", \"gzip\");");
                                printWriter.println("\t\t\t}");
                                printWriter.println("\t\t\tctx.Response.setContentLength(bytes.length);");
                                printWriter.println("\t\t\tctx.Response.write(bytes);");
                            } else {
                                printWriter.println("\t\t\tctx.Response.setContentLength(os.size());");
                                printWriter.println("\t\t\tos.writeTo(ctx.Response.getOutputStream());");
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
                printWriter.println("\t\t});");
            }
        }
    }

    private <T extends Annotation> void addInterceptor(Element element, PrintWriter printWriter) {
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
                printWriter.append("\t\trouter.addInterceptors(java.util.Arrays.asList(" + patterns + ")").append(", bean." + se.getSimpleName() + "()");
                printWriter.println(");");
            }
        }
    }

    private <T extends Annotation> void createMapperBean(Element element, PrintWriter printWriter) {
        printWriter.println("\t\tbean = new " + element.getSimpleName() + "() { ");
        for (Element se : element.getEnclosedElements()) {
            String returnType = ((ExecutableElement) se).getReturnType().toString();
            printWriter.print("\t\t\tpublic " + returnType + " " + se.getSimpleName() + "(");
            boolean first = true;
            for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                if (first) {
                    first = false;
                } else {
                    printWriter.print(",");
                }
                printWriter.print(param.asType().toString() + " " + param.getSimpleName());
            }
            printWriter.println(") {");
            printWriter.append(JsonSerializer.headBlank(1)).println("try (org.apache.ibatis.session.SqlSession session = factory.openSession(true)) {");
            printWriter.print(JsonSerializer.headBlank(2));
            if (!"void".equals(returnType)) {
                printWriter.print("return ");
            }
            printWriter.print("session.getMapper(" + element.getSimpleName() + ".class)." + se.getSimpleName() + "(");
            first = true;
            for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                if (first) {
                    first = false;
                } else {
                    printWriter.print(", ");
                }
                printWriter.print(param.getSimpleName().toString());
            }
            printWriter.println(");");
            printWriter.append(JsonSerializer.headBlank(1)).println("}");
            printWriter.println("\t\t\t}");
            printWriter.println();
        }
        printWriter.println("\t\t};");
    }
}
