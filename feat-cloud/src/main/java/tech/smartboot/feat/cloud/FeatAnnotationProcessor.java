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
import com.alibaba.fastjson2.annotation.JSONField;
import org.apache.ibatis.annotations.Mapper;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.InterceptorMapping;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.serializer.BooleanSerializer;
import tech.smartboot.feat.cloud.serializer.DateSerializer;
import tech.smartboot.feat.cloud.serializer.JsonFieldSerializer;
import tech.smartboot.feat.cloud.serializer.StringSerializer;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.utils.StringUtils;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

// 该注解表示该处理器支持的 Java 源代码版本

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class FeatAnnotationProcessor extends AbstractProcessor {
    private static final int RETURN_TYPE_VOID = 0;
    private static final int RETURN_TYPE_STRING = 1;
    private static final int RETURN_TYPE_OBJECT = 2;
    private static final int RETURN_TYPE_BYTE_ARRAY = 3;
    private Map<String, JsonFieldSerializer> jsonFieldSerializerMap = new HashMap<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(Bean.class.getCanonicalName());
        types.add(Autowired.class.getCanonicalName());
        types.add(Controller.class.getCanonicalName());
        types.add(Mapper.class.getCanonicalName());
        return types;
    }

    FileObject serviceFile;
    PrintWriter serviceWrite;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            serviceFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + CloudService.class.getName());
            serviceWrite = new PrintWriter(serviceFile.openWriter());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        jsonFieldSerializerMap.put(boolean.class.getName(), new BooleanSerializer());
        jsonFieldSerializerMap.put(String.class.getName(), new StringSerializer());
        jsonFieldSerializerMap.put(Date.class.getName(), new DateSerializer());
        jsonFieldSerializerMap.put(Timestamp.class.getName(), new DateSerializer());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<String> services = new ArrayList<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Bean.class)) {
            Bean bean = element.getAnnotation(Bean.class);
            if (element.getKind() == ElementKind.CLASS) {
                createAptLoader(element, bean, services);
            }
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(Controller.class)) {
            Controller controller = element.getAnnotation(Controller.class);
            createAptLoader(element, controller, services);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(Mapper.class)) {
            Mapper controller = element.getAnnotation(Mapper.class);
            createAptLoader(element, controller, services);
        }
        // 如果不希望后续的处理器继续处理这些注解，返回 true，否则返回 false
        for (String service : services) {
            serviceWrite.println(service);
        }
        serviceWrite.flush();

        return false;
    }

    private <T extends Annotation> void createAptLoader(Element element, T annotation, List<String> services) {
        try {

            //获取所有包含Autowired注解的字段
            List<Element> autowiredFields = new ArrayList<>();
            for (Element field : element.getEnclosedElements()) {
                if (field.getAnnotation(Autowired.class) != null) {
                    autowiredFields.add(field);
                }
            }
//            //element增加setter方法
//            if (!autowiredFields.isEmpty()) {
//                FileObject origFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, element.getEnclosingElement().toString(), element.getSimpleName() + ".java");
//                String origContent = origFileObject.getCharContent(false).toString();
//                int lastIndex = origContent.lastIndexOf("}");
//                StringBuilder writer = new StringBuilder();
//                writer.append(origContent.substring(0, lastIndex));
//                for (Element field : autowiredFields) {
//                    writer.append("    public void set" + field.getSimpleName() + "(" + field.asType() + " " + field.getSimpleName() + ") {");
//                    writer.append("        this." + field.getSimpleName() + " = " + field.getSimpleName() + ";");
//                    writer.append("    }");
//                }
//                writer.append("}");
//
//            }


            String loaderName = element.getSimpleName() + "BeanAptLoader";
            JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(loaderName);
            Writer writer = javaFileObject.openWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            printWriter.println("package " + element.getEnclosingElement().toString() + ";");
            printWriter.println();
            printWriter.println("import " + AbstractServiceLoader.class.getName() + ";");
            printWriter.println("import " + ApplicationContext.class.getName() + ";");
            printWriter.println("import " + Router.class.getName() + ";");
            printWriter.println("import " + JSONObject.class.getName() + ";");
            printWriter.println("import com.alibaba.fastjson2.JSON;");
            printWriter.println();
            printWriter.println("public class " + loaderName + " extends " + AbstractServiceLoader.class.getSimpleName() + " {");
            printWriter.println();
            printWriter.println("    private " + element.getSimpleName() + " bean;");
            if (annotation instanceof Mapper) {
                printWriter.println("    private org.apache.ibatis.session.SqlSessionFactory factory;");
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
            } else {
                generateBeanOrController(element, annotation, printWriter);
            }
            String beanName = element.getSimpleName().toString().substring(0, 1).toLowerCase() + element.getSimpleName().toString().substring(1);
            if (annotation instanceof Bean && !((Bean) annotation).value().isEmpty()) {
                beanName = ((Bean) annotation).value();
            }
            printWriter.println("\t\tapplicationContext.addBean(\"" + beanName + "\", bean);");
            printWriter.println("\t}");

            printWriter.println();
            printWriter.println("\tpublic void autowired(ApplicationContext applicationContext) throws Throwable {");
            printWriter.append(generateAutoWriedMethod(element, annotation));
            printWriter.println("\t}");
            printWriter.println();

            printWriter.println("\tpublic void router(" + Router.class.getSimpleName() + " router) {");
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

            //生成service配置
            services.add(element.getEnclosingElement().toString() + "." + loaderName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Annotation> void generateBeanOrController(Element element, T annotation, PrintWriter printWriter) {
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
        if (!StringUtils.startsWith(basePath, "/")) {
            basePath = "/" + basePath;
        }
        for (Element se : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : se.getAnnotationMirrors()) {
                if (RequestMapping.class.getName().equals(mirror.getAnnotationType().toString())) {
                    String requestURL = basePath;

                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                        ExecutableElement k = entry.getKey();
                        AnnotationValue v = entry.getValue();
                        if ("value".equals(k.getSimpleName().toString())) {
                            requestURL = v.getValue().toString();
                            if (basePath.endsWith("/") && requestURL.startsWith("/")) {
                                requestURL = basePath + requestURL.substring(1);
                            } else if (basePath.endsWith("/") && !requestURL.startsWith("/") || !basePath.endsWith("/") && requestURL.startsWith("/") || requestURL.isEmpty()) {
                                requestURL = basePath + requestURL;
                            } else {
                                requestURL = basePath + "/" + requestURL;
                            }
                        } else if ("method".equals(k.getSimpleName().toString())) {
                            System.out.println(v.getValue());
//                                    printWriter.println(v.getValue().toString());
                        }
                    }
                    if (StringUtils.isBlank(requestURL)) {
                        throw new FeatException("the value of RequestMapping on " + element.getSimpleName() + "@" + se.getSimpleName() + " is not allowed to be empty.");
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
                    boolean async = returnTypeInt == RETURN_TYPE_OBJECT && AsyncResponse.class.getName().equals(returnType.toString());
                    if (async) {
                        printWriter.println("\t\trouter.route(\"" + requestURL + "\", new " + RouterHandler.class.getName() + "()  {");
                        printWriter.println("\t\t\t@Override");
                        printWriter.println("\t\t\tpublic void handle(" + Context.class.getName() + " ctx) throws Throwable {");
                        printWriter.println();
                        printWriter.println("\t\t\t}");
                        printWriter.println();
                        printWriter.println("\t\t\t@Override");
                        printWriter.println("\t\t\tpublic void handle(" + Context.class.getName() + " ctx, " + CompletableFuture.class.getName() + "<Void> completableFuture) throws Throwable {");
                    } else {
                        printWriter.println("\t\trouter.route(\"" + requestURL + "\", ctx -> {");
                    }


                    boolean first = true;
                    StringBuilder newParams = new StringBuilder();
                    StringBuilder params = new StringBuilder();
                    int i = 0;
                    for (VariableElement param : ((ExecutableElement) se).getParameters()) {
                        if (first) {
                            first = false;
                        } else {
                            params.append(",");
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

                    switch (returnTypeInt) {
                        case RETURN_TYPE_VOID:
                            break;
                        case RETURN_TYPE_STRING:
                            printWriter.println("\t\t\tbyte[] bytes = rst.getBytes(\"UTF-8\"); ");
                            printWriter.println("\t\t\tctx.Response.setContentLength(bytes.length);");
                            printWriter.println("\t\t\tctx.Response.write(bytes);");
                            break;
                        case RETURN_TYPE_BYTE_ARRAY:
                            printWriter.println("\t\t\tctx.Response.setContentLength(bytes.length);");
                            printWriter.println("\t\t\tctx.Response.write(bytes);");
                            break;
                        case RETURN_TYPE_OBJECT:

                            if (AsyncResponse.class.getName().equals(returnType.toString())) {
                                printWriter.println("\t\t\t\tresponse(rst, ctx, completableFuture);");
                                printWriter.println("\t\t\t}");
                            } else {
                                printWriter.println("\t\t\tjava.io.ByteArrayOutputStream os = getOutputStream();");
                                writeJsonObject(printWriter, returnType, "rst", 0, new HashMap<>(), bytesCache);
                                printWriter.println("\t\t\tctx.Response.setContentType(\"application/json\");");
                                printWriter.println("\t\t\tctx.Response.setContentLength(os.size());");
                                printWriter.println("\t\t\tos.writeTo(ctx.Response.getOutputStream());");
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
    }

    private static void toBytesPool(PrintWriter printWriter, Map<String, String> map, String value) {
        String key = ("b_" + value.hashCode()).replace("-", "$");
        map.put(key, "private static final byte[] " + key + " = " + toBytesStr(value) + ";");
        printWriter.append("os.write(").append(key).println(");");
    }

    private static String headBlank(int i) {
        StringBuilder sb = new StringBuilder("\t\t");
        do {
            sb.append("\t");
        } while (i-- > 0);
        return sb.toString();
    }

    public void writeJsonObject(PrintWriter printWriter, TypeMirror typeMirror, String obj, int i, Map<TypeMirror, TypeMirror> typeMap0, Map<String, String> byteCache) throws IOException {
        //深层级采用JSON框架序列化，防止循环引用
        if (i > 4) {
            printWriter.println(headBlank(i) + "if (" + obj + " != null) {");
            printWriter.println(headBlank(i + 1) + "os.write(JSON.toJSONBytes(" + obj + "));");
            printWriter.println(headBlank(i) + "} else {");
            toBytesPool(printWriter, byteCache, "null");
            printWriter.println(headBlank(i) + "}");
            return;
        }
        if (typeMirror instanceof ArrayType) {
            printWriter.append("os.write('[');");
            printWriter.append("for (" + typeMirror + ")");
            printWriter.append("os.write(']');");
            return;
        } else if (typeMirror.toString().startsWith("java.util.List") || typeMirror.toString().startsWith("java.util.Collection")) {
            printWriter.append(headBlank(i)).println("if (" + obj + " != null) {");
            printWriter.append(headBlank(i + 1)).println("os.write('[');");
            TypeMirror type = ((DeclaredType) typeMirror).getTypeArguments().get(0);
            if (typeMap0.containsKey(type)) {
                type = typeMap0.get(type);
            }
            printWriter.append(headBlank(i + 1)).println("boolean first" + i + " = true;");
            printWriter.append(headBlank(i + 1)).println("for (" + type + " p" + i + " : " + obj + " ) {");
            printWriter.append(headBlank(i + 2)).println("if (first" + i + ") {");
            printWriter.append(headBlank(i + 3)).println("first" + i + " = false;");
            printWriter.append(headBlank(i + 2)).println("} else {");
            printWriter.append(headBlank(i + 3)).println("os.write(',');");
            printWriter.append(headBlank(i + 2)).println("}");
            if (String.class.getName().equals(type.toString())) {
                printWriter.append(headBlank(i + 2)).println("os.write('\"');");
                printWriter.append(headBlank(i + 2)).println("os.write(p" + i + ".getBytes());");
                printWriter.append(headBlank(i + 2)).println("os.write('\"');");
            } else {
                writeJsonObject(printWriter, type, "p" + i, i + 2, typeMap0, byteCache);
            }

            printWriter.append(headBlank(i + 1)).println("}");
            printWriter.append(headBlank(i + 1)).println("os.write(']');");
            printWriter.append(headBlank(i)).println("} else {");
            printWriter.append(headBlank(i + 1));
            toBytesPool(printWriter, byteCache, "null");
            printWriter.append(headBlank(i)).println("}");
            return;
        } else if (typeMirror.toString().startsWith("java.util.Map")) {
            printWriter.println("os.write(new JSONObject(" + obj + ").toString().getBytes());");
            return;
        } else if (typeMirror.toString().endsWith(".JSONObject")) {
            printWriter.println("if (" + obj + " != null) {");
            printWriter.println("os.write(" + obj + ".toString().getBytes());");
            printWriter.println("} else {");
            toBytesPool(printWriter, byteCache, "null");
            printWriter.println("}");
            return;
        }
        printWriter.println(headBlank(i) + "os.write('{');");

        //获取泛型参数
        List<? extends TypeMirror> typeKey = ((DeclaredType) (((DeclaredType) typeMirror).asElement().asType())).getTypeArguments();
        List<? extends TypeMirror> typeArgs = ((DeclaredType) typeMirror).getTypeArguments();
        Map<TypeMirror, TypeMirror> typeMap = new HashMap<>();
        for (int z = 0; z < typeArgs.size(); z++) {
            typeMap.put(typeKey.get(z), typeArgs.get(z));
        }
        int j = i * 10;

        List<Element> elements = new ArrayList<>();
        //当子类存在相同字段时，子类的字段会覆盖父类的字段
        Set<String> fields = new HashSet<>();

        //提取父类的字段
        TypeMirror temp = typeMirror;
        while (!temp.toString().startsWith("java.")) {
            for (Element element : ((DeclaredType) temp).asElement().getEnclosedElements()) {
                if (element.getKind() != ElementKind.FIELD) {
                    continue;
                }
                if (element.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                if (fields.contains(element.getSimpleName().toString())) {
                    continue;
                }
                fields.add(element.getSimpleName().toString());

                JSONField jsonField = element.getAnnotation(JSONField.class);
                if (jsonField != null && !jsonField.serialize()) {
                    continue;
                }

                elements.add(element);
            }
            temp = ((TypeElement) ((DeclaredType) temp).asElement()).getSuperclass();
        }

        for (Element se : elements) {
            if (j++ > i * 10) {
                printWriter.append(headBlank(i));
                printWriter.println("os.write(',');");
            }

            TypeMirror type = se.asType();
            if (se.asType().getKind() == TypeKind.TYPEVAR) {
                type = ((DeclaredType) typeMirror).getTypeArguments().get(0);
            }
            String fieldName = se.getSimpleName().toString();
            JSONField jsonField = se.getAnnotation(JSONField.class);
            if (jsonField != null && StringUtils.isNotBlank(jsonField.name())) {
                fieldName = jsonField.name();
            }
            JsonFieldSerializer serializer = jsonFieldSerializerMap.get(type.toString());
            if (serializer != null) {
                serializer.serialize(printWriter, se, obj, i, byteCache);
            } else if (Arrays.asList("int", "short", "byte", "long", "float", "double", "java.lang.Integer", "java.lang.Short", "java.lang.Byte", "java.lang.Long", "java.lang.Float", "java.lang.Double").contains(type.toString())) {
                printWriter.append(headBlank(i));
                toBytesPool(printWriter, byteCache, "\"" + fieldName + "\":");
                printWriter.append(headBlank(i));
                printWriter.append("os.write(String.valueOf(").append(obj).append(".get").append(se.getSimpleName().toString().substring(0, 1).toUpperCase()).append(se.getSimpleName().toString().substring(1)).println("()).getBytes());");
            } else {
                printWriter.append(headBlank(i));
                toBytesPool(printWriter, byteCache, "\"" + fieldName + "\":");
                String filedName = obj + ".get" + se.getSimpleName().toString().substring(0, 1).toUpperCase() + se.getSimpleName().toString().substring(1) + "()";
                printWriter.append(headBlank(i)).println("if (" + filedName + " == null) {");
                printWriter.append(headBlank(i + 1));
                toBytesPool(printWriter, byteCache, "null");
                printWriter.append(headBlank(i)).println("} else {");
                writeJsonObject(printWriter, type, filedName, i + 1, typeMap, byteCache);
                printWriter.append(headBlank(i)).println("}");
            }
        }
        printWriter.append(headBlank(i)).println("os.write('}');");
    }


    private static String toBytesStr(String str) {
        StringBuilder s = new StringBuilder("{");

        for (int i = 0; i < str.length(); i++) {
            if (i > 0) {
                s.append(", ");
            }
            s.append('\'').append(str.charAt(i)).append('\'');
        }
        return s + "}";
    }

    private static <T extends Annotation> void addInterceptor(Element element, PrintWriter printWriter) throws IOException {
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

    private static <T extends Annotation> void createMapperBean(Element element, PrintWriter printWriter) throws IOException {
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
            printWriter.append(headBlank(1)).println("try (org.apache.ibatis.session.SqlSession session = factory.openSession(true)) {");
            printWriter.print(headBlank(2));
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
            printWriter.append(headBlank(1)).println("}");
            printWriter.println("\t\t\t}");
            printWriter.println();
        }
        printWriter.println("\t\t};");
    }
}
